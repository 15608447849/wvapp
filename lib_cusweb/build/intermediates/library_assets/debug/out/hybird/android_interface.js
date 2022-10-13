/* eslint-disable */

// author:lsp
// edit time:20220929
// ps: android h5外壳与js交互的接口文件 ( 请勿修改 ), android对应实现类 @JSInterface.class
// _下滑线前缀表示提供给android native的默认JS函数实现,由android native主动请求

(function (window) {

  if (window.ANDROID) return;

/**********************************************************************************************************************************/

  // 判断是不是安卓设备
  function currentEnv(){
    return "undefined" !== typeof JSInterface;
  }

  // 判断是函数
  function isFunction(obj) {
    return (
      typeof obj !== "undefined" &&
      typeof obj === "function" &&
      typeof obj.nodeType !== "number"
    );
  }

   /* 对象转json字符串 */
   function convertString(obj) {
      if (obj && typeof obj === "object") {
        return JSON.stringify(obj);
      }
      return obj;
    }

    /* 字符串转对象 */
   function convertObject(str) {
      var obj;
      try {
        obj = JSON.parse(str);
      } catch (e) {
        obj = str;
      }
      return obj;
    }

     //判断非空
   function isNotNull(str) {
       return str && str !== "null";
   }


/**********************************************************************************************************************************/

  /* js请求native 获取结果的回调函数临时存储Map */
  const js_invoke_native_callback_ids = {};

  /* js注册API函数,提供给native调用 */
  const js_api_functions = {};

  /* JS注册Native可以使用的接口函数 */
  function registerJSFunction(function_name, function_imp) {
    if (isFunction(function_imp)) {
      js_api_functions[function_name] = function_imp;
    }
  }

  /*
   JS向native发送请求,
   _method= native的方法名 ,
    _data= string数据,
    _response_callback= 回调函数
  */
  function jsInvokeNative(_method, _data, _response_callback) {
    // 判断当前参数
    if (arguments.length === 0) {
      throw new Error("not specified android native method name.");
    }
    if (arguments.length === 1) {
      _content = null;
      _response_callback = null;
    }
    if (arguments.length === 2) {
      if (isFunction(_content)) {
        _response_callback = _content;
        _content = null;
      }
    }

    let _callbackId = null;
    // 如果存在回调函数则存储
    if (isFunction(_response_callback)) {
        _callbackId = "js_callback_native_" + new Date().getTime();
        js_invoke_native_callback_ids[_callbackId] = _response_callback;
    }

    try {
         //android js native api @JSInterface.class -> invoke(final String methodName, final String data, final String callback_id){...}
         JSInterface.onJSInvokeNative(_method, _content, _callbackId);
    } catch (exception) {
      if (isFunction(response_callback)) {
        //移除回调接口
        delete js_invoke_native_callback_ids[_callbackId];
      }
      throw exception;
    }
  }

   /*
   * 添加一个内存和持久化缓存
   * 字符串 键值对
   */
   function putCache(k,v){
        try {
            JSInterface.putData(k,v);
        } catch (exception) {
            console.error(exception);
        }
   }

   /*
   * 获取一个内存和持久化缓存,优先内存
   * 字符串 键值对
   */
   function delCache(k){
        try {
            JSInterface.delData(k);
            return true;
        } catch (exception) {
            console.error(exception);
        }
        return false;
   }

   /*
   * 添加一个内存和持久化缓存
   * 字符串 键值对
   */
   function getCache(k){
        try {
           return JSInterface.getData(k);
        } catch (exception) {
            console.error(exception);
        }
        return null;
   }

   /* JS 打印控制台信息 */
    function println(log){
        try {
             if (typeof log === "undefined") return;

             if (typeof log === "object") {
                log = JSON.stringify(value);
             }

            JSInterface.println(log);
        } catch (exception) {
            console.error(exception);
        }
    }

    /* JS 获取设备信息*/
    function getDevice(){
        try {
           return JSInterface.getDevice();
        } catch (exception) {
            console.error(exception);
        }
        return '{}';
    }

   /* JS 跳转其他页面 */
    function openWindow(url,data){
        try {
             if (typeof data === "undefined"){
                data = null;
             }

             if (typeof data === "object") {
                data = JSON.stringify(data);
             }

            JSInterface.openWindow(url,data);
        } catch (exception) {
            console.error(exception);
         }
      }


/**********************************************************************************************************************************/

  /* JS 页面加载完成通知 */
    function onInitializationComplete(){
        try {
            JSInterface.onInitializationComplete();
        } catch (exception) {
            console.error(exception);
         }
    }


/**********************************************************************************************************************************/

  /* 返回js invoke native 的结果 resp
   * js调用android native后, android native主动发起的结果回调
   */
  function _jsInvokeNativeResponse(callback_fun_id, result) {
    setTimeout(function () {
      let response_callback = js_invoke_native_callback_ids[callback_fun_id];
      if (!response_callback) {
        console.error("'"+callback_fun_id + "' response callback function doesn't exist!");
        return;
      }
      //移除回调接口
      delete js_invoke_native_callback_ids[callback_fun_id];
      //调用函数 传递结果字符串
       response_callback(result);
    });
  }

  /* android native对js的主动调用
   * function_name = js端的函数名
   * content=文本字符串
   * android native端的回调函数id
   */
  function _nativeInvokeJS(function_name, content, native_callback_id) {
    setTimeout(function () {
      let value = null;
      // 需要js提前注册可调用的API 函数
      let function_imp = js_api_functions[function_name];
      if (function_imp) {
        try {
          // 执行函数
          value = function_imp(content); // 返回值,直接传递到native层
        } catch (exception) {
          console.error(exception);
          value = "android native invoke js except: " + exception;
        }
        if (typeof value === "undefined") {
          value = null;
        } else if (typeof value === "object") {
          value = JSON.stringify(value);
        }
      } else {
        // 没有找到函数
        value = "'" + function_name + "' js function is unregistered.";
      }
      if (isNotNull(native_callback_id)) {
        try {
          // 回调给android native结果值 (字符串)
         JSInterface._nativeInvokeJsResponse(native_callback_id, value);
        } catch (exception) {
          // 打印错误信息
          console.error(exception);
        }
      }
    });
  }

/**********************************************************************************************************************************/

  window.ANDROID = {
    currentEnv:currentEnv, // 检测环境函数

    _nativeInvokeJS: _nativeInvokeJS, //native调用js方法
    _jsInvokeNativeResponse: _jsInvokeNativeResponse, //native主动回调,返回js请求的响应结果

    registerJSFunction: registerJSFunction,  //JS 注册 native可以使用的接口
    jsInvokeNative: jsInvokeNative,  //native 请求 JS的接口

    putCache:putCache, //JS 添加本地缓存(native可共享)
    getCache:getCache, //JS 获取本地缓存(内存优先)
    getCache:delCache, //JS 删除本地缓存

    getDevice:getDevice,  //JS 获取设备信息
    println:println,  //JS 打印设备日志
    openWindow:openWindow,//JS 打开其他页面

    onInitializationComplete:onInitializationComplete  //JS 通知页面初始化加载完成
  }


})(window);
