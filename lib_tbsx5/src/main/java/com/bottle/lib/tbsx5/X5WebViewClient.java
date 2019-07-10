package com.bottle.lib.tbsx5;


import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.export.external.interfaces.WebResourceError;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/10.
 * email: 793065165@qq.com
 */
public class X5WebViewClient extends WebViewClient {
    /**
     * 防止加载网页时调起系统浏览器
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//        LLog.print("shouldOverrideUrlLoading(WebView,String)" + url);
        view.loadUrl(url);
        return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webView, WebResourceRequest request) {
//        LLog.print("shouldOverrideUrlLoading(WebView,String)"+ request.getUrl());
        return super.shouldOverrideUrlLoading(webView, request);
    }

    /**
     * url拦截
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
        if (X5Core.isX5CoreUse(webView)){
            //禁用滑动按钮
            webView.getX5WebViewExtension().setScrollBarFadingEnabled(false);
            webView.getX5WebViewExtension().setHorizontalScrollBarEnabled(false);//水平不显示滚动按钮
            webView.getX5WebViewExtension().setVerticalScrollBarEnabled(false); //垂直不显示滚动按钮
            LLog.print("腾讯X5 设置滚动条不显示完成");
        }
        WebResourceResponse webResourceResponse = JSUtils.mediaUriIntercept(webView.getContext(),url,WebResourceResponse.class);
        return webResourceResponse != null ? webResourceResponse : super.shouldInterceptRequest(webView,url);
    }

    /**
     * http加载错误
     */
    @Override
    public void onReceivedHttpError(WebView webView, WebResourceRequest request, WebResourceResponse errorResponse) {
//        LLog.print("onReceivedHttpError\t" + GsonUtils.javaBeanToJson(errorResponse));
        super.onReceivedHttpError(webView, request, errorResponse);
    }

    @Override
    public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
        super.onReceivedError(webView, webResourceRequest, webResourceError);
    }

    @Override
    public void onReceivedSslError(WebView webView, SslErrorHandler handler, SslError error) {
//        LLog.print("onReceivedSslError(WebView,SslErrorHandler,SslError)\t");
        handler.proceed();// 接受所有网站的证书
        super.onReceivedSslError(webView, handler, error);
    }



    @Override
    public void onPageFinished(WebView webView, String url) {
        LLog.print("onPageFinished()\t" + url +" ,x5 core = " + X5Core.isX5CoreUse(webView) );
        super.onPageFinished(webView, url);
    }

//    oncon
}
