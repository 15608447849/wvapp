package com.bottom.wvapp.activitys;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

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
    /**
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json),js回调函数的ID - function(response)
     *
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
        webView.post(new Runnable() {
            @Override
            public void run() {
                String js = String.format(JS_INTERFACE_NAME,callback_id ,new Gson().toJson(result));
                Log.d("JavaScriptInterface",js);
                webView.loadUrl(js);
            }
        });
//
    }

    public static void webViewLoadLocalJs(WebView view) {
        String jsContent = assetFile2Str(view.getContext(), "_native.js");
        Log.d("加载JS",jsContent);
        view.loadUrl(JAVA_SCRIPT + "document.write('<script>"+jsContent+"</script>')" );
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

}
