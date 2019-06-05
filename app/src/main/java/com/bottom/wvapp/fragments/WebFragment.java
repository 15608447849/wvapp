package com.bottom.wvapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottom.wvapp.R;
import com.bottom.wvapp.jsprovider.NativeServerImp;
import com.bottom.wvapp.tool.WebViewUtil;

import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.toolset.jsbridge.JavaScriptInterface;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebFragment extends SFragment {

    private WebView webView;
    private NativeServerImp server;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (webView == null){
                webView = (WebView) inflater.inflate(R.layout.activity_main,null);
                server = new NativeServerImp(this);
                WebViewUtil.initViewWeb(webView,JavaScriptInterface.NAME,new JavaScriptInterface(webView).setIBridgeImp(server));
            }
        return webView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        LLog.print(bundle);
        if (bundle!=null) webView.loadUrl(bundle.getString("url")); //加载链接
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LLog.print("fragment","onActivityResult");
        server.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected boolean onBackPressed() {
        if (webView!=null){
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onBackPressed();
    }
}
