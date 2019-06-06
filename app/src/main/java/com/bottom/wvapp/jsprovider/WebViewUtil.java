package com.bottom.wvapp.jsprovider;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;

import lee.bottle.lib.toolset.log.LLog;

import static lee.bottle.lib.toolset.util.AppUtils.getLocalFileByte;
import static lee.bottle.lib.toolset.util.ImageUtils.imageCompression;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebViewUtil {

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public static void initViewWeb(WebView web_view, String name, Object jsBridge){
        WebSettings settings = web_view.getSettings();
        settings.setJavaScriptEnabled(true);  //开启JavaScript支持
        settings.setDomStorageEnabled(true);//开启DOM Storage功能
        LLog.print("getLoadsImagesAutomatically() - " + settings.getLoadsImagesAutomatically());
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);//解决图片不显示
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);//设置js可以直接打开窗口，如window.open()，默认为false
        settings.setMediaPlaybackRequiresUserGesture(false);//播放音频，多媒体需要用户手动？设置为false为可自动播放
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs (true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(web_view.getContext().getCacheDir().getPath());
        web_view.setWebChromeClient(WEB_CHROME_CLIENT);
        web_view.setWebViewClient(WEB_VIEW_CLIENT);
        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        web_view.addJavascriptInterface(jsBridge, name);
    }

    /**web 内核*/
    private static final WebChromeClient WEB_CHROME_CLIENT = new WebChromeClient(){

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress<100) {
                view.getSettings().setBlockNetworkImage(false);
            }else{
                view.getSettings().setBlockNetworkImage(true);
            }
            LLog.print("onProgressChanged 当前进度:"+newProgress);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            LLog.print("onGeolocationPermissionsShowPrompt(String,GeolocationPermissions)");
            callback.invoke(origin, true, false);
            super.onGeolocationPermissionsShowPrompt(origin, callback);
        }

        //控制台消息
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            LLog.print(
                    "(" +consoleMessage.sourceId().substring(consoleMessage.sourceId().lastIndexOf("/"))+
                            ":" + consoleMessage.lineNumber() + ","
                            +consoleMessage.messageLevel()+")",
                    consoleMessage.message()
            );
            return true;
        }
    };

    /**web 客户端*/
    private static final WebViewClient WEB_VIEW_CLIENT = new WebViewClient(){
        @Override
        public void onPageFinished(WebView view, String url) {
            if (url.equalsIgnoreCase("about:blank")) return;
            super.onPageFinished(view, url);
            LLog.print("onPageFinished 页面加载完成:" + url);
        }


        //        @Nullable
//        @Override
//        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
//            LLog.print("shouldInterceptRequest(view,url)" + url);
//            return super.shouldInterceptRequest(view, url);
//        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            LLog.print("shouldInterceptRequest(view,request)" + request.getUrl());
            String scheme = request.getUrl().getScheme();
//            if (request.getUrl().equals("localhost")) return null;
            try {
                if ("image".equalsIgnoreCase(scheme)
                || "audio".equalsIgnoreCase(scheme)
                || "video".equalsIgnoreCase(scheme)){
                    return mediaLoad(view,request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            // 这个方法在 android 6.0才出现
            int statusCode = errorResponse.getStatusCode();
            if (404 == statusCode || 500 == statusCode) {
                view.loadUrl("www.baidu.com");// 避免出现默认的错误界面
                LLog.print("加载页面错误: " + request.getUrl() );
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
            super.onReceivedError(view, webResourceRequest, webResourceError);
            if (webResourceRequest.isForMainFrame()) {//是否是为 main frame创建
                view.loadUrl("about:blank");// 避免出现默认的错误界面
            }
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            LLog.print("onReceivedSslError(WebView,SslErrorHandler)"); //如果是证书问题，会打印出此条log到console
            handler.proceed();// 接受所有网站的证书
            super.onReceivedSslError(view, handler, error);
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LLog.print("shouldOverrideUrlLoading 加载中 - "+ url);
            view.loadUrl(url);
            return true;
        }
    };

    //媒体文件加载
    private static WebResourceResponse mediaLoad(WebView view, WebResourceRequest request) throws Exception {
            String path = request.getUrl().getPath();
            File file = new File(path);
            LLog.print("加载本地文件:" + path+" , " + file.exists());
            if (!file.exists()) throw new FileNotFoundException(path);
            if ("image".equalsIgnoreCase(request.getUrl().getScheme())){
                file = imageCompression(view.getContext(),file,1000);//图片压缩
            }

            byte[] imageBuf = getLocalFileByte(file);
                    String mimeType = request.getUrl().getScheme()+"/*";
                    return new WebResourceResponse(mimeType, "UTF-8",
                            new ByteArrayInputStream(imageBuf));
    }

}
