package lee.bottle.lib.webh5;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;
import lee.bottle.lib.webh5.interfaces.DynamicJSInterfaceI;
import lee.bottle.lib.webh5.interfaces.JSResponseCallback;


import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import static lee.bottle.lib.toolset.os.ApplicationDevInfo.sharedStorage;

@SuppressLint("JavascriptInterface")
public abstract class JSInterfaceFunction {
    private boolean isDebug;

    private final static String JAVA_SCRIPT = "javascript:";
    private final static String define_nativeInvokeJS =  "ANDROID._nativeInvokeJS('%s','%s','%s')"; //native主动调用js的方法
    private final static String define__onResponseJSInvokeNative = "ANDROID._jsInvokeNativeResponse('%s','%s')";//响应js的请求,返回结果

    // native调用js,js处理结果返回响应,存储集合
    private final HashMap<String, JSResponseCallback> jsResponseCallbackMap = new HashMap<>();

    protected final JSInterface jsInterface;
    
    public JSInterfaceFunction(JSInterface jsInterface) {
        this.jsInterface = jsInterface;
    }

    /**
     * native主动调用js方法
     * */
    public void _nativeInvokeJS(final String jsFunctionName, final String data, JSResponseCallback callback){
        String callbackId = null;
        if (callback!=null){
            callbackId = "native_callback_js_"+System.currentTimeMillis();
            jsResponseCallbackMap.put(callbackId,callback);
        }

        loadJavaScript(String.format(define_nativeInvokeJS(),jsFunctionName ,data ,callbackId));
    }

    /**
     * 由JS主动调用, native请求js以后 ,js响应结果
     */
    @JavascriptInterface
    public void _nativeInvokeJsResponse(String callback_id,String data){
        try {
            if (isDebug) LLog.print("js响应 native_response_callback_id = "+ callback_id+" , data = "+ data);
            JSResponseCallback callback = jsResponseCallbackMap.remove(callback_id);
            if (callback==null) throw new Exception("'"+callback_id + "' native response callback function doesn't exist!");
            callback.call(data);
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    // 存储
    @JavascriptInterface
    public void putData(String key,String val){
        if (isDebug) LLog.print("js 共享数据存储 : " + key  + "=" + val);
        SharedPreferences sp = sharedStorage(jsInterface.webView.getContext());
        sp.edit().putString(key,val).apply();
    }

    //获取
    @JavascriptInterface
    public String getData(String key){
        SharedPreferences sp = sharedStorage(jsInterface.webView.getContext());
        String val = sp.getString(key,"");
        if (isDebug) LLog.print("js 共享数据取值 : " + key +"="+val);
        return val;
    }

    //移除
    @JavascriptInterface
    public void delData(String key){
        if (isDebug) LLog.print("js 共享数据删除 : " + key );
        SharedPreferences sp = sharedStorage(jsInterface.webView.getContext());
        sp.edit().remove(key).apply();
    }

    // 打印
    @JavascriptInterface
    public void println(String msg){
        LLog.print( msg );
    }

    /**
     * js -> native 动态实现的接口方法
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json/text), js回调函数的ID - function(response)
     */
    @JavascriptInterface
    public void invokeNative(final String methodName, final String data, final String callback_id) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                DynamicJSInterfaceI dynamicInterface = JSInterface.getDynamicInterface(methodName);
                if (dynamicInterface == null) return;

                Object value ;
                Throwable targetEx;
                try {
                    value = JSInterface.callDynamicJSInterfaceI(dynamicInterface,methodName,data);
                } catch (Exception e) {
                    targetEx = e;
                    if (e instanceof InvocationTargetException) {
                        targetEx =((InvocationTargetException)e).getTargetException();
                    }
                    HashMap<String,Object> map = new HashMap<>();
                    map.put("code",-1);
                    map.put("message","NATIVE ERROR");
                    map.put("error",targetEx.getMessage());
                    value = map;
                }

                if (callback_id == null) return;

                final String result  = value == null ? null :
                        value instanceof String ? StringUtils.getDecodeJSONStr(value.toString()) : GsonUtils.javaBeanToJson(value);

                loadJavaScript(String.format(define__onResponseJSInvokeNative(),callback_id ,result));

            }
        });
    }

    // 调用javascript
    public void loadJavaScript(final String javaScript){
        jsInterface.webView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    jsInterface.webView.loadUrl(JAVA_SCRIPT+javaScript);
                } catch (Exception e) {
                    LLog.error(e);
                }
            }
        });
    }

    protected String getInterfaceName(){
        return JSInterface.class.getSimpleName();
    }

    protected String define_nativeInvokeJS(){
        return define_nativeInvokeJS;
    }

    protected String define__onResponseJSInvokeNative(){
        return define__onResponseJSInvokeNative;
    }

    // 获取设备信息
    public abstract String getDevice();
    // 窗口初始化加载完成通知
    public abstract void onInitializationComplete();

    /* 打开窗口 push 压入一个新页面 ; pushAndRemove 移除当前页 打开新页面 ; pushAndRemoveAll 移除所有历史页面 打开新页面*/
    public abstract void openWindow(String url,String type);
    /* 结束当前窗口 false - 强制关闭当前页;true - 强制整个关闭应用 */
    public abstract void closeCurrentWindow(String bool);
}
