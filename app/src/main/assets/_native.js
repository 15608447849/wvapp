(function() {

  if (window._JSNativeBridge) return;

  var _callback_fun_ids = {};

  var uniqueId = 1;

  /*向native发送请求信息*/
  function _requestNative(method, data, response_callback) {
    var callbackId = 'cb_' + (uniqueId++) + '_' + new Date().getTime();
    _callback_fun_ids[callbackId] = response_callback;
    console.log("callbackId >>> "+callbackId);
    native.invoke(method, JSON.stringify(data), callbackId)
  }
  /** native的回调 */
  function _callbackInvoke(callback_fun_id, data) {
   console.log("_callbackInvoke exe");
    setTimeout(function () {

      console.log(callback_fun_id +" -- ", JSON.stringify(data));
      response_callback = _callback_fun_ids[callback_fun_id]
      if (!response_callback) {
        console.log(callback_fun_id + ' callback function doesn\'t exist!');
        return;
      }
      response_callback(JSON.stringify(data));
      /*移除回调接口*/
      delete _callback_fun_ids[callback_fun_id];
    })
  }
  /**native的主动调用*/
  function _invoke(data, callback_id) {
    setTimeout(function () {
      //暂未实现
    })
  }

  var _JSNativeBridge = window._JSNativeBridge = {
    _requestNative: _requestNative,
    _callbackInvoke: _callbackInvoke,
    _invoke: _invoke
  }

  var doc = document;
  var readyEvent = doc.createEvent('Events');
  readyEvent.initEvent('_JSNativeBridgeInit');
  readyEvent.bridge = _JSNativeBridge;
  doc.dispatchEvent(readyEvent);
  console.log("_JSNativeBridgeInit Over!!!");
})();

