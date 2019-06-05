package com.bottom.wvapp.tool;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.webkit.ConsoleMessage;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
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

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebViewUtil {
    private static String TAG = "web-view";
    /**web 内核*/
    private static final WebChromeClient WEB_CHROME_CLIENT = new WebChromeClient(){

        //控制台消息
        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            LLog.printTag(TAG,
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

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

            String scheme = request.getUrl().getScheme();
            LLog.printTag(TAG,"请求URL:",request.getUrl()+" - " +scheme);
            try {
                if ("image".equalsIgnoreCase(scheme)
                || "audio".equalsIgnoreCase(scheme)
                || "video".equalsIgnoreCase(scheme)){
                    LLog.print("------");
                    return mediaLoad(view,request);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();// 接受所有网站的证书
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
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

    private static WebResourceResponse mediaLoad(WebView view, WebResourceRequest request) throws Exception {
            String path = request.getUrl().getPath();
            File file = new File(path);
            LLog.printTag(TAG,"加载本地文件:" + path+" , " + file.exists() );
            if (!file.exists()) throw new FileNotFoundException(path);
//            file = imageCompression(view.getContext(),file,1000);//压缩
            byte[] imageBuf = getLocalFileByte(file);
                    String mimeType = request.getUrl().getScheme()+"/*";
                    return new WebResourceResponse(mimeType, "UTF-8",
                            new ByteArrayInputStream(imageBuf));
    }
}
