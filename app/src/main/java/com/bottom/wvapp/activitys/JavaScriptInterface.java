package com.bottom.wvapp.activitys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

/**
 * 提供给JS调用的后台接口
 *
 *
 *
 *
 */
@SuppressLint("JavascriptInterface")
public class JavaScriptInterface {

    public static final String NAME = "native";

    private final static String JAVA_SCRIPT = "javascript:";

    private final static String JS_INTERFACE_INVOKE_NAME = JAVA_SCRIPT + "_JSNativeBridge._invoke('%s','%s','%s')";// invoke js function

    private final static String JS_INTERFACE_NAME = JAVA_SCRIPT + "_JSNativeBridge._callbackInvoke('%s','%s')";// js callback function

    private final WebView webView;

    private final Object holder;


    public JavaScriptInterface(WebView webView, Object holder) {
        this.webView = webView;
        this.holder = holder;
    }

    private static class JsCallbackResult{
        int code = 0;
        Object data;
    }

    interface JSCallback{
        void callback(String json,JsCallbackResult result);
    }

    private final HashMap<String,JSCallback> jsCallbackMap = new HashMap<>();

    /**
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json),js回调函数的ID - function(response)
     */
    @JavascriptInterface
    public void invoke(String methodName,String json,final String callback_id) {
        //异步执行
       final JsCallbackResult result = new JsCallbackResult();
        try {
            Log.d("JavaScriptInterface",
                     Thread.currentThread() +" @ "+
                            methodName +" , " +
                            json+" , "+
                            callback_id);
            //空判断
            //反射调用方法
            Method m = holder.getClass().getMethod(methodName, String.class);
            result.data = m.invoke(holder,json);
        } catch (Exception e) {
            e.printStackTrace();
            result.data = e;
            if (e instanceof InvocationTargetException) {
                result.data  =((InvocationTargetException)e).getTargetException();
            }
        }
        if (callback_id.equals("null")) return;
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(String.format(JS_INTERFACE_NAME,callback_id ,new Gson().toJson(result)));
            }
        });
    }
    // 主动调用js方法
    public void requestJs(final String method, final Object data, JSCallback callback){
        String callbackId = "null";
        if (callback!=null){
            callbackId = "java_callback_"+System.currentTimeMillis();
            jsCallbackMap.put(callbackId,callback);
        }
        final String _callbackId = callbackId;
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(String.format(JS_INTERFACE_INVOKE_NAME,method ,new Gson().toJson(data),_callbackId));
            }
        });
    }
    /**
     * js回调
     */
    @JavascriptInterface
    public void callbackInvoke(String callback_id,String json){
        try {
            JSCallback callback = jsCallbackMap.remove(callback_id);
            if (callback==null) throw new Exception(callback_id + " callback function doesn\'t exist!");
            callback.callback(json,new Gson().fromJson(json,JsCallbackResult.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String webViewLoadLocalJs(WebView view) {
        String jsContent = assetFile2Str(view.getContext(), "_native.js");
        Log.d("加载JS","\n"+jsContent);
        return jsContent;

    }

    private static String assetFile2Str(Context c, String urlStr) {
        InputStream in = null;
        try {
            in = c.getAssets().open(urlStr);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line ;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    line = line.replaceAll("\\t", "   ");
                    if (!line.matches("^\\s*\\/\\/.*")) {
                        sb.append(line);
                    }
                }
            } while (line != null);

            bufferedReader.close();
            in.close();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    public static void test(final JavaScriptInterface javaScriptInterface){
        new Thread(){
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String[] arr = new String[]{"1111111","555555"};
                javaScriptInterface.requestJs("test", arr, new JSCallback() {
                    @Override
                    public void callback(String json, JsCallbackResult result) {
                        Log.d("调用JS回调",json+ "\n" +result);
                    }
                });
            }
        }.start();
    }
}
