package com.bottle.wvapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.jsprovider.UpdateVersionServerImp;

import java.util.ArrayList;

import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.ObjectRefUtil;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebFragment extends SFragment {

    private View view;
    private NativeServerImp server;
    private String loadUrl;
    private String core;

    private IWebViewInit iWebViewInit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (view == null){
                view = inflater.inflate(R.layout.fragment_web,null);
                server = NativeServerImp.buildServer(this);
            }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        LLog.print(bundle);
        if (bundle!=null) {
            if (NativeServerImp.config==null) return;

            this.loadUrl = bundle.getString("url");
            this.core =
//            "lee.bottle.lib.toolset.web.SysCore";
                    android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                            "lee.bottle.lib.toolset.web.SysCore" :
//                            "com.bottle.lib.tbsx5.X5Core" ;
                            "com.bottle.lib.crosswalk.CKCore" ;

            int type =NativeServerImp.config.webPageVersion;
            if (type >= 0 ){
                this.loadUrl = "file://" + getContext().getFilesDir().getPath() + this.loadUrl;
            }
            loadView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ArrayList<String> list = getActivity().getIntent().getStringArrayListExtra("notify_param");
        if (list!=null){
            getActivity().getIntent().removeExtra("notify_param");
           if (list.get(0).equals("NOTIFY")){
               server.notifyEntryToJs();//跳转到JS个人中心-消息列表
            }
        }
    }

    public void loadView() {
        mHandler.ui(new Runnable() {
            @Override
            public void run() {
                if (core!= null){
                    try {
                        iWebViewInit = (IWebViewInit) ObjectRefUtil.createObject(core,
                                new Class[]{Context.class,ViewGroup.class, IBridgeImp.class},
                                getActivity().getApplicationContext(),view,server);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (iWebViewInit == null) {
                    openErrorPage();
                    UpdateVersionServerImp.executeCompatibleApk();
                    return;
                }
                LLog.print(core + " 准备加载url "+ loadUrl);
                if (iWebViewInit!=null) {
                    iWebViewInit.clear();
                    iWebViewInit.getProxy().loadUrl(loadUrl);
//                    iWebViewInit.getProxy().loadUrl("http://192.168.1.81:8888");
//                    iWebViewInit.getProxy().loadUrl("http://soft.imtt.qq.com/browser/tes/feedback.html");
                }

            }
        });
    }

    private void openErrorPage() {
        view.setBackgroundResource(android.R.drawable.stat_notify_error);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        server.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected boolean onBackPressed() {
        return iWebViewInit != null && iWebViewInit.onBackPressed();
    }
}
