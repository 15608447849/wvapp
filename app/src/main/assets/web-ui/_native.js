(function() {

  if (window.JNB) return;

    //判断是函数
    function isFunction( obj ) {
      return typeof obj !== "undefined" && typeof obj === "function" && typeof obj.nodeType !== "number";
    }

    //判断非空
    function isNotNull(str){
        return str && str !== 'null';
    }

  /*js注册,提供给native可以调用的函数*/
    var _js_register_functions = {};

    /*注册接口方法函数 */
  function _register(function_name,function_imp){
      if(isFunction(function_imp)){
          _js_register_functions[function_name] = function_imp;
      }
    }

  //对象转json字符串
  function convertString(obj){
          if (obj && typeof obj === 'object') {
               return JSON.stringify(obj);
           }
           return obj;
    }

 //字符串转对象
 function convertObject(str){
          var obj;
          try {
               obj = JSON.parse(str);
           } catch (e) {
           obj = str;
           }
          return obj;
    }

  //回调函数临时存储集合
  var _callback_fun_ids = {};

  //回调标识序号
  var uniqueId = 1;

  //转发
  function _transfer(serverName,cls,method){
        return "ts:"+serverName+"@"+cls+"@"+method;
  }

  /*向native发送请求信息, native的方法名 , 内容string , 回调函数 */
    function _requestNative(method, content, response_callback) {
         //判断当前参数
    if (arguments.length === 0) {
         throw new Error('参数不正确');
    }          
    if (arguments.length === 1) {
          content = null;
          response_callback = null;
    }       
    if(arguments.length === 2){
            if (isFunction(content)){
                response_callback = content;
                content = null;
            }
        }

    var callbackId = null;
        //如果存在回调函数,存储
    if (isFunction(response_callback)) {
       
        if (isFunction(response_callback)) {
             callbackId = 'js_callback_' + (uniqueId++) + '_' + new Date().getTime();
             _callback_fun_ids[callbackId] = response_callback;
        }
    } 

    try {
        native.invoke(method, content, callbackId); //
    } catch (exception) {
        if(isFunction(response_callback)){
            //移除回调接口
            delete _callback_fun_ids[callbackId];
        }
        throw exception;
    }
}

  /*js调用native后, native的回调*/
  function _callbackInvoke(callback_fun_id, result) {
      setTimeout(function () {
          response_callback = _callback_fun_ids[callback_fun_id];
          if (!response_callback) {
              console.error(callback_fun_id + ' callback function doesn\'t exist!');
              return;
          }
          //移除回调接口
          delete _callback_fun_ids[callback_fun_id];
          //调用函数 传递结果字符串
          response_callback(result);

      });
  }

  /*native对js的主动调用传递消息, function_name js端的函数名, 数据文本,native端的回调函数id*/
  function _invoke(function_name,content,callback_id) {
      setTimeout(function () {
          var value = null;
          var function_imp = _js_register_functions[function_name];
          if (function_imp) {
              try {
                  //执行函数
                  value = function_imp(content); //返回 string类型
              } catch (exception) {
                  console.error(exception);
                  value = "except:" + exception;
              }
              if (typeof value === "undefined") {
                  value = null;
              } else if (typeof value === "object") {
                  value = JSON.stringify(value);
              }
          } else {
              //没有找到函数
              value = "except:" + function_name + " doesn\'t exist!";
          }
          if (isNotNull(callback_id)) {
              try {
                  //回调给native结果
                  native.callbackInvoke(callback_id, value);
              } catch (exception) {
                  //打印错误信息
                  console.error(exception);
              }
          }

      });
  }

    var JNB = window.JNB = {
        _register: _register, //注册
        _transfer: _transfer, //生成转发
        _requestNative: _requestNative, //请求native方法
        _callbackInvoke: _callbackInvoke, //native 回调
        _invoke: _invoke //native调用js方法
    };

  var doc = document;
  var readyEvent = doc.createEvent('Events');
  readyEvent.initEvent('JNB_Init');
  readyEvent.bridge = JNB;
  doc.dispatchEvent(readyEvent);
  console.log("JNB 对象注入成功");
})();

