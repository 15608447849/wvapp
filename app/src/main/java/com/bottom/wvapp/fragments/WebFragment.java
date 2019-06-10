package com.bottom.wvapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottom.wvapp.R;
import com.bottom.wvapp.jsprovider.NativeServerImp;
import com.bottom.wvapp.tbsx5.TbsWebViewUtil;

import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.toolset.jsbridge.JavaScriptInterface;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebFragment extends SFragment {

    private View view;
    private com.tencent.smtt.sdk.WebView webView;
    private NativeServerImp server;
    private String loadUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (webView == null){
                TbsWebViewUtil.tbsInit(getContext());
                view = inflater.inflate(R.layout.fragment_web,null);
                webView = view.findViewById(R.id.web_view);
                server = new NativeServerImp(this);
                //webView.addJavascriptInterface(new JavaScriptInterface(webView).setIBridgeImp(server),JavaScriptInterface.NAME);
                TbsWebViewUtil.initViewWeb(webView, JavaScriptInterface.NAME,new JavaScriptInterface(webView).setIBridgeImp(server));
            }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        LLog.print(bundle);

        if (bundle!=null) {
            this.loadUrl = bundle.getString("url");
            webView.loadUrl(loadUrl); //加载链接
        }

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
