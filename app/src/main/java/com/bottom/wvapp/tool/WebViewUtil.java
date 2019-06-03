package com.bottom.wvapp.tool;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebViewUtil {
    private static final String TAG = "WEB_VIEW_UTIL";
    /**web 内核*/
    private static final WebChromeClient WEB_CHROME_CLIENT = new WebChromeClient(){

    };

    /**web 客户端*/
    private static final WebViewClient WEB_VIEW_CLIENT = new WebViewClient(){
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();// 接受所有网站的证书
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG,"shouldOverrideUrlLoading "+ view+" "+url);
            view.loadUrl(url);
            return false;
        }
    };


    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public static void initViewWeb(WebView web_view, String name, Object jsBridge){
        WebSettings settings = web_view.getSettings();
        settings.setJavaScriptEnabled(true);  //开启JavaScript支持
        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        web_view.addJavascriptInterface(jsBridge, name);
        settings.setBlockNetworkImage(false);//解决图片不显示
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        web_view.setWebChromeClient(WEB_CHROME_CLIENT);
        web_view.setWebViewClient(WEB_VIEW_CLIENT);
    }




}
