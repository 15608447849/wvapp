package com.bottom.wvapp.bridge;

import android.annotation.SuppressLint;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.bottom.log.LLog;
import com.bottom.wvapp.service.ITransferServer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * js / native 通讯接口
 */
@SuppressLint("JavascriptInterface")
public class JavaScriptInterface implements IJsBridge {

    public static final String NAME = "native";

    private final static String JAVA_SCRIPT = "javascript:";

    private final static String JS_INTERFACE_INVOKE_NAME = JAVA_SCRIPT + "_JSNativeBridge._invoke('%s','%s','%s')";// invoke js function

    private final static String JS_INTERFACE_NAME = JAVA_SCRIPT + "_JSNativeBridge._callbackInvoke('%s','%s')";// js callback function

    private final WebView webView;

    private final Object holder;

    private final ITransferServer iTransferServer; //转发服务

    public JavaScriptInterface(WebView webView,ITransferServer iTransferServer ,Object holder) {
        this.webView = webView;
        this.iTransferServer = iTransferServer;
        this.holder = holder;
    }



    private final HashMap<String, IJsBridge.JSCallback> jsCallbackMap = new HashMap<>();

    /**
     * js -> native
     *
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json/text),js回调函数的ID - function(response)
     */
    @JavascriptInterface
    public void invoke(final String methodName, final String data, final String callback_id) {
        //异步执行
        Object value;
        try {

            if (methodName.startsWith("ts:")){
                //转发协议  ts:服务名@类名@方法名
                String temp = methodName.replace("ts:","");
                String[] args = temp.split("@");
                value = iTransferServer.transfer(args[0],args[1],args[2],data);

            }else{
                //反射调用方法
                Method m = holder.getClass().getMethod(methodName, String.class);
                value = m.invoke(holder,data);
            }

        } catch (Exception e) {
            e.printStackTrace();
            value = e;
            if (e instanceof InvocationTargetException) {
                value  =((InvocationTargetException)e).getTargetException();
            }
        }
        if (callback_id == null) return;

        final String result  = value == null ? null : value.toString();

        webView.post(new Runnable() {
            @Override
            public void run() {
                LLog.print(methodName,data,callback_id," ====>> ",result);
                webView.loadUrl(String.format(JS_INTERFACE_NAME,callback_id ,result));
            }
        });
    }


    /**
     * native -> js , 然后 js回调
     */
    @JavascriptInterface
    public void callbackInvoke(String callback_id,String data){
        try {
            LLog.print("callback_id = "+ callback_id+" , "+ data);
            IJsBridge.JSCallback callback = jsCallbackMap.remove(callback_id);
            if (callback==null) throw new Exception(callback_id + " callback function doesn\'t exist!");
            callback.callback(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 主动调用js方法
    @Override
    public void requestJs(final String method, final String data, IJsBridge.JSCallback callback){

        String callbackId = null;
        if (callback!=null){
            callbackId = "java_callback_"+System.currentTimeMillis();
            jsCallbackMap.put(callbackId,callback);
        }
        final String _callbackId = callbackId;
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(String.format(JS_INTERFACE_INVOKE_NAME,method ,data,_callbackId));
            }
        });
    }

}
