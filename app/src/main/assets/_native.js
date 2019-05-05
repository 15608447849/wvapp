(function() {

  if (window._JSNativeBridge) return;

    //判断是函数
    function isFunction( obj ) {
      return typeof obj !== "undefined" && typeof obj === "function" && typeof obj.nodeType !== "number";
    }

    function isNotNull(str){
        return str&&str!='null'
    }

  /**js注册提供给native的调用*/
  var _js_register_functions = {}

    /** 注册函数方法函数 */
  function _register(function_name,function_imp){
      if(isFunction(function_imp)){
         _js_register_functions[function_name] = function_imp
      }
  }

  function convertString(obj){
          if (obj && typeof obj === 'object') {
               return JSON.stringify(obj);
           }
           return obj;
       }

 function convertObject(str){
          var obj;
          try {
               obj = JSON.parse(str);
           } catch (e) {
           obj = str;
           }
          return obj;
      }

  var _callback_fun_ids = {};

  var uniqueId = 1;

  function _transfer(serverName,cls,method){
        return "ts:"+serverName+"@"+cls+"@"+method;
  }

  /*向native发送请求信息*/
  function _requestNative(method, object, response_callback) {
    if(arguments.length == 2){
        if(isFunction(object)){
            response_callback = object;
            object = null
        }
    }
    var callbackId = null;
    if(isFunction(response_callback)){
        callbackId = 'js_callback_' + (uniqueId++) + '_' + new Date().getTime();
        _callback_fun_ids[callbackId] = response_callback;
    }
    try {
    	native.invoke(method, convertString(object), callbackId)
    } catch (exception) {
        if(isFunction(response_callback)){
            //移除回调接口
            delete _callback_fun_ids[callbackId];
            response_callback(exception)
        }
    }
  }

  /** native的回调 */
  function _callbackInvoke(callback_fun_id, result) {
    setTimeout(function () {
      response_callback = _callback_fun_ids[callback_fun_id]
      if (!response_callback) {
        console.log(callback_fun_id + ' callback function doesn\'t exist!');
        return;
      }
      //移除回调接口
      delete _callback_fun_ids[callback_fun_id];
      //调用函数
      response_callback(convertObject(result));

    })
  }

  /**native的主动调用*/
  function _invoke(function_name,data,callback_id) {
    setTimeout(function () {
       var value = null
      //暂未实现
        var function_imp = _js_register_functions[function_name]
        if(function_imp){
            //执行函数
          value = function_imp(data); //返回 string类型

          if (typeof value === "undefined" ){
           value = null
          }else if(typeof value === "object"){
           value = JSON.stringify(value)
          }
        }else{
            //没有找到函数
            value = function_name +" doesn\'t exist!"
        }
        if(isNotNull(callback_id)){
            try {
                //回调给native结果
                native.callbackInvoke(callback_id,value)
            } catch (exception) {
                //打印错误信息
                console.log(exception)
            }
        }

    })
  }

  var _JSNativeBridge = window._JSNativeBridge = {
    _register:_register, //注册
    _transfer:_transfer, //生成转发
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

