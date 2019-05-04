(function() {

  if (window._JSNativeBridge) return;

    //判断是函数
    function isFunction( obj ) {
      return typeof obj !== "undefined" && typeof obj === "function" && typeof obj.nodeType !== "number";
    }


  /**js注册提供给native的调用*/
  var _js_register_functions = {}

    /** 注册函数方法函数 */
  function _register(function_name,function_imp){
      if(isFunction(function_imp)){
         _js_register_functions[function_name] = function_imp
      }
  }

  var _callback_fun_ids = {};

  var uniqueId = 1;

  /*向native发送请求信息*/
  function _requestNative(method, data, response_callback) {
    var callbackId = "null" ;
    if(isFunction(response_callback)){
        callbackId = '_callback' + (uniqueId++) + '_' + new Date().getTime();
        _callback_fun_ids[callbackId] = response_callback;
    }
    try {
    	native.invoke(method, JSON.stringify(data), callbackId)
    } catch (exception) {
        if(isFunction(response_callback)){
            //移除回调接口
            delete _callback_fun_ids[callbackId];
            response_callback(exception)
        }
    }
  }

  /** native的回调 */
  function _callbackInvoke(callback_fun_id, json) {
    setTimeout(function () {
      response_callback = _callback_fun_ids[callback_fun_id]
      if (!response_callback) {
        console.log(callback_fun_id + ' callback function doesn\'t exist!');
        return;
      }
      //移除回调接口
      delete _callback_fun_ids[callback_fun_id];
      //调用函数
      response_callback(JSON.parse(json));
    })
  }

  /**native的主动调用*/
  function _invoke(function_name,json,callback_id) {
    setTimeout(function () {
        var obj = { code:0,data:'null'}
      //暂未实现
        var function_imp = _js_register_functions[function_name]
        if(function_imp){
            //执行函数
          var result = function_imp(JSON.parse(json));
          obj.code = 200;
          console.log(result,typeof result)
          if(typeof result !== "undefined"){
           obj.data = result;
          }
        }else{
            //没有找到函数
            console.log("function_name = "+ function_name+" ,不存在")
            obj.code = -1;
            obj.data = "js 函数:"+function_name+" ,未注册";
        }
        if(callback_id != 'null'){
            try {
                native.callbackInvoke(callback_id,JSON.stringify(obj))
            } catch (exception) {
                //打印错误信息
                console.log(exception)
            }
        }

    })
  }

  var _JSNativeBridge = window._JSNativeBridge = {
    _register:_register, //注册
    _requestNative: _requestNative, //请求native方法
    _callbackInvoke: _callbackInvoke, //native 回调
    _invoke: _invoke //native调用js方法
  }

  var doc = document;
  var readyEvent = doc.createEvent('Events');
  readyEvent.initEvent('_JSNativeBridgeInit');
  readyEvent.bridge = _JSNativeBridge;
  doc.dispatchEvent(readyEvent);
  console.log("_JSNativeBridgeInit SUCCESS!!!");
})();

