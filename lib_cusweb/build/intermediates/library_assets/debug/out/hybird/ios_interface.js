/* eslint-disable */

// author:jsx
// edit time:20221008
// ps: IOS实现
(function (window) {

    if (window.IOS) return;

    /***************************************接口方法实现****************************************************************/


    // 判断是不是IOS设备
    function currentEnv() {
        // 待实现
        return navigator.userAgent.indexOf('onedrugiosApp') != -1;
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


    /***************************************1、基本通用方法****************************************************************/


    /*
    * 添加一个内存和持久化缓存
    * 字符串 键值对
    */
    function putCache(k, v) {
        try {
            window.prompt('nativePutData', JSON.stringify({ k: k, v: v }));
        } catch (exception) {
            console.error(exception);
        }
        return v;
    }

    /*
    * 获取一个内存和持久化缓存,优先内存
    * 字符串 键值对
    */
    function delCache(k) {
        try {
            window.prompt('nativeDelData', k);
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
    function getCache(k) {
        try {
            v = window.prompt('nativeGetData', k);
            return v ? v : null;
        } catch (exception) {
            console.error(exception);
        }
        return null;
    }

    /* JS 打印native控制台信息 */
    function println(log) {
        try {
            if (typeof log === "undefined") return;
            if (typeof log === "object") {
                log = JSON.stringify(log);
            }
            alert(log);
        } catch (exception) {
            console.error(exception);
        }
    }

    /* JS 获取设备信息 */
    function getDevice() {
        try {
            return window.prompt("getDeviceInfoMap");
        } catch (exception) {
            console.error(exception);
        }
        return '{}';
    }

    /* JS 跳转其他页面 */
    function openWindow(url, data) {
        try {
            if (typeof data === "undefined") {
                data = null;
            }
            if (typeof data === "object") {
                data = JSON.stringify(data);
            }
            var obj = {
                url:url,
                type:data
            };
            jsInvokeNative("openWindow", JSON.stringify(obj));
        } catch (exception) {
            console.error(exception);
        }
    }
    /* JS 关闭当前页面 */
    function closeCurrentWindow(bool){
        try{
            jsInvokeNative("closeCurrentWindow", bool);
        }catch(exception){
            console.error(exception);
        }
    }

    /* JS 页面加载完成通知 */
    function onInitializationComplete() {
        try {
            jsInvokeNative("pageLoadComplete", url, data);
        } catch (exception) {
            console.error(exception);
        }
    }

    /***************************************2、交互方法****************************************************************/

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
    【native.requestNative】
     1、JS向native发送请求
     _method：native的方法名 ,
     _content：string数据,
     _response_callback：js回调函数名
    */
    function jsInvokeNative(_method, _content, _response_callback) {
        //判断当前参数
        if (arguments.length === 0) {
            throw new Error("参数不正确");
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
        //如果存在回调函数,存储
        if (isFunction(_response_callback)) {
            _callbackId = "js_callback_native_" + new Date().getTime();
            js_invoke_native_callback_ids[_callbackId] = _response_callback;
        }
        try {
            //jxs?：这里是绑定在webview的回调上的
            window.webkit.messageHandlers.Param.postMessage({
                method: _method,
                content: _content,
                callbackId: _callbackId,
            });
        } catch (exception) {
            if (isFunction(response_callback)) {
                //移除回调接口
                delete js_invoke_native_callback_ids[_callbackId];
            }
            throw exception;
        }
    }

    /* 
[native.callbackInvoke]
ios没这个方法，用的window.ICEPayResult,方法名例如【PayResult、ICEPayResult、forceLogout】
2、返回js invoke native 的结果 resp
* js调用 native后, iOS native主动发起的结果回调
*/
    function _jsInvokeNativeResponse(callback_fun_id, result) {
        setTimeout(function () {
            let response_callback = js_invoke_native_callback_ids[callback_fun_id];
            if (!response_callback) {
                console.error(callback_fun_id + " callback function doesn't exist!");
                return;
            }
            //移除回调接口
            delete js_invoke_native_callback_ids[callback_fun_id];

            //调用函数 传递结果字符串
            response_callback(result);
        });
    }
    
    

    /* 
    【native.invoke】
     3、iOS native对js的主动调用
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
                    value = " iOS native invoke js except: " + exception;
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
                    //jxs?：这里是绑定在webview的回调上的
                    window.webkit.messageHandlers.NativeInvokeParam.postMessage({
                        content: value,
                        callbackId: native_callback_id,
                    });
                } catch (exception) {
                    // 打印错误信息
                    console.error(exception);
                }
            }
        });
    }



    /**********************************************************************************************************************************/
    


    window.IOS = {
        currentEnv: currentEnv, // 检测环境函数
        
        _nativeInvokeJS: _nativeInvokeJS, //native调用js方法
        _jsInvokeNativeResponse: _jsInvokeNativeResponse, //native主动回调,返回js请求的响应结果
    

        registerJSFunction: registerJSFunction,  //JS 注册 native可以使用的接口
        jsInvokeNative: jsInvokeNative,  //native 请求 JS的接口
        
        //native调用js方法

        putCache: putCache, //JS 添加本地缓存(native可共享)
        getCache: getCache, //JS 获取本地缓存(内存优先)
        getCache: delCache, //JS 删除本地缓存

        getDevice: getDevice,  //JS 获取设备信息
        println: println,  //JS 打印设备日志
        openWindow:openWindow,//JS 打开其他页面(url,打开类型) push 压入一个新页面 ; pushAndRemove 移除当前页 打开新页面 ; pushAndRemoveAll 移除所有历史页面 打开新页面
        closeCurrentWindow:closeCurrentWindow,//JS 关闭当前页  false - 强制关闭当前页 ; true - 强制整个关闭应用


        onInitializationComplete: onInitializationComplete,  //JS 通知页面初始化加载完成
    }


})(window);
