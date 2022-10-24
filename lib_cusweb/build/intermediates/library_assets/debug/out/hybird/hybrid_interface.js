/* eslint-disable */

// author:lsp
// edit time:20221008
// ps: 移动端混合开发接口层

(function (window) {

  if (window.hybrid) return;

/*************************************************************接口方法 WEB版本实现*********************************************************************/

 
  /* JS注册Native可以使用的接口函数 */
  function registerJSFunction(function_name, function_imp) {
    console.log('运行环境错误',function_name, function_imp);
}

/*
JS向native发送请求,
_method= native的方法名 ,
_data= string数据,
_response_callback= 回调函数
*/
function jsInvokeNative(_method, _data, _response_callback) {
    console.log('运行环境错误' , _method, _data, _response_callback);
}

/*
* 添加一个内存和持久化缓存
* 字符串 键值对
*/
function putCache(k,v){
    try {
        localStorage.setItem(k, v);
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
        localStorage.removeItem(k);
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
       return localStorage.getItem(k);
    } catch (exception) {
        console.error(exception);
    }
    return null;
}

/* JS 打印native控制台信息 */
function println(log){
    try {
         console.log(log)
    } catch (exception) {
        console.error(exception);
    }
}

/* JS 获取设备信息 */
function getDevice(){
    return '{}';
}

/* JS 跳转其他页面 */
function openWindow(url,type){
    try {
         switch (type) {
           case "pushAndRemove":
             window.history.replaceState(null, document.title, url);
             history.go(0);
             break;
           case "pushAndRemoveAll":
             break;
           default:
             window.location.href = url;
             break;
         }
    } catch (exception) {
        console.error(exception);
     }
  }

/* js关闭当前页 bool=ture结束应用 */
function closeCurrentWindow(bool){
    try {
        if(!bool) window.close(url);
    } catch (exception) {
        console.error(exception);
     }
  }

/* JS 页面加载完成通知 */
function onInitializationComplete(){
    try {
        console.log("********** 应用初始化完成 **********")
    } catch (exception) {
        console.error(exception);
    }
}


  // 默认web版本实现  
  window.hybrid = {
    registerJSFunction: registerJSFunction,  //JS 注册 native可以使用的接口
    jsInvokeNative: jsInvokeNative,  //native 请求 JS的接口

    putCache:putCache, //JS 添加本地缓存(native可共享)
    getCache:getCache, //JS 获取本地缓存(内存优先)
    getCache:delCache, //JS 删除本地缓存

    getDevice:getDevice,  //JS 获取设备信息
    println:println,  //JS 打印设备日志
    openWindow:openWindow,//JS 打开其他页面(url,打开类型) push 压入一个新页面 ; pushAndRemove 移除当前页 打开新页面 ; pushAndRemoveAll 移除所有历史页面 打开新页面
    closeCurrentWindow:closeCurrentWindow,//JS 关闭当前页  false - 强制关闭当前页 ; true - 强制整个关闭应用

    onInitializationComplete:onInitializationComplete,  //JS 通知页面初始化加载完成
  }


  if(window.ANDROID && window.ANDROID.currentEnv()){
    window.hybrid = window.ANDROID;
    console.log("hybrid 环境设置: android ")
  }

  if(window.IOS && window.IOS.currentEnv()){
    window.hybrid = window.IOS;
    console.log("hybrid 环境设置: ios ")
  }


})(window);
