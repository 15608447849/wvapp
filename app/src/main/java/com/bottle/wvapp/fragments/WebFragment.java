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

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import java.util.ArrayList;
import java.util.Objects;

import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.ObjectRefUtil;


/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class WebFragment extends SFragment {

    private View view;
    private IWebViewInit iWebViewInit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (view == null){
                view = inflater.inflate(R.layout.fragment_web,null);
                NativeServerImp.buildFragment(this);
            }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        loadView();
    }

    @Override
    public void onResume() {
        super.onResume();
        ArrayList<String> list = getActivity().getIntent().getStringArrayListExtra("notify_param");
        if (list!=null){
            getActivity().getIntent().removeExtra("notify_param");
            NativeServerImp.notifyEntryToJs(list.get(0)); //跳转到指定页面
        }
    }

    public void loadView() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            AppUtils.toast(getSActivity(),"抱歉,应用无法兼容ANDROID 6.0以下版本");
            return;
        }
        String core = "lee.bottle.lib.toolset.web.SysCore";
        try {
            iWebViewInit = (IWebViewInit) ObjectRefUtil.createObject(core,
                    new Class[]{Context.class,ViewGroup.class, IBridgeImp.class},
                    Objects.requireNonNull(getActivity()).getApplicationContext(),
                    view, NativeServerImp.iBridgeImp);
            iWebViewInit.clear();
            iWebViewInit.getProxy().loadUrl(BuildConfig._WEB_HOME_URL);
        } catch (Exception e) {
            LLog.print("加载内核失败, core = "+ core);
            AppUtils.toast(getSActivity(),"浏览器内核加载失败");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        NativeServerImp.iBridgeImp.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected boolean onBackPressed() {
        return iWebViewInit != null && iWebViewInit.onBackPressed();
    }


}
