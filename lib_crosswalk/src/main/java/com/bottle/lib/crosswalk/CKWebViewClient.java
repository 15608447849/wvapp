package com.bottle.lib.crosswalk;

import android.net.http.SslError;
import android.webkit.ValueCallback;
import android.webkit.WebResourceResponse;

import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;

import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class CKWebViewClient extends XWalkResourceClient{
    public CKWebViewClient(XWalkView view) {
        super(view);
    }

    @Override
    public void onLoadFinished(XWalkView view, String url) {
        LLog.print("onLoadFinished " + url);
        super.onLoadFinished(view, url);
    }

    @Override
    public boolean shouldOverrideUrlLoading(XWalkView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public WebResourceResponse shouldInterceptLoadRequest(XWalkView view, String url) {
        WebResourceResponse response = JSUtils.mediaUriIntercept(view.getContext(),url,WebResourceResponse.class);
        return response !=null ? response : super.shouldInterceptLoadRequest(view, url);
    }

    @Override
    public XWalkWebResourceResponse shouldInterceptLoadRequest(XWalkView view, XWalkWebResourceRequest request) {
        return super.shouldInterceptLoadRequest(view,request);
    }

    @Override
    public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {
        callback.onReceiveValue(true);
        super.onReceivedSslError(view, callback, error);
    }


    @Override
    public void onProgressChanged(XWalkView view, int progressInPercent) {
        LLog.print("onProgressChanged " + progressInPercent);
        super.onProgressChanged(view, progressInPercent);
    }
}
