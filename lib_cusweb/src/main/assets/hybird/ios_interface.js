/* eslint-disable */

// author:jsx
// edit time:20221008
// ps: IOS实现

(function (window) {

  if (window.IOS) return;

/*************************************************************接口方法实现*********************************************************************/

 // 判断是不是IOS设备
 function currentEnv(){
     // 待实现
    return false;
  }
 
  /* JS注册Native可以使用的接口函数 */
  function registerJSFunction(function_name, function_imp) {
       // 待实现
  }

  /*
   JS向native发送请求,
   _method= native的方法名 ,
   _data= string数据,
   _response_callback= 回调函数
  */
  function jsInvokeNative(_method, _data, _response_callback) {
       
  }

   /*
   * 添加一个内存和持久化缓存
   * 字符串 键值对
   */
   function putCache(k,v){
        try {
            // 待实现
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
            // 待实现
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
            // 待实现
        } catch (exception) {
            console.error(exception);
        }
        return null;
   }

   /* JS 打印native控制台信息 */
    function println(log){
        try {
             // 待实现
        } catch (exception) {
            console.error(exception);
        }
    }

    /* JS 获取设备信息 */
    function getDevice(){
        return '{}';
    }

   /* JS 跳转其他页面 */
    function openWindow(url,data){
        try {
            // 待实现
        } catch (exception) {
            console.error(exception);
         }
      }


  /* JS 页面加载完成通知 */
    function onInitializationComplete(){
        try {
             // 待实现
        } catch (exception) {
            console.error(exception);
        }
    }



  window.IOS = {
    currentEnv:currentEnv, // 检测环境函数
    
    registerJSFunction: registerJSFunction,  //JS 注册 native可以使用的接口
    jsInvokeNative: jsInvokeNative,  //native 请求 JS的接口

    putCache:putCache, //JS 添加本地缓存(native可共享)
    getCache:getCache, //JS 获取本地缓存(内存优先)
    getCache:delCache, //JS 删除本地缓存

    getDevice:getDevice,  //JS 获取设备信息
    println:println,  //JS 打印设备日志
    openWindow:openWindow,//JS 打开其他页面

    onInitializationComplete:onInitializationComplete,  //JS 通知页面初始化加载完成
  }


})(window);
