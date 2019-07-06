package com.bottle.wvapp.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
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
                view = inflater.inflate(R.layout.fragment_cosweb,null);
                server = NativeServerImp.createServer(this);
            }
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Bundle bundle = getArguments();
        LLog.print(bundle);
        if (bundle!=null) {
            this.loadUrl = bundle.getString("url");
            this.core = bundle.getString("core");
            int type =NativeServerImp.config.webPageVersion;
            if (type >= 0 ){
                this.loadUrl = "file://" + getContext().getCacheDir().getPath() + this.loadUrl;
                LLog.print("加载路径: "+ this.loadUrl);
            }
            loadView();
//            selectCoreDialog();
        }
    }

    private void selectCoreDialog() {
        DialogUtil.createSimpleListDialog(getContext(), "选择内核",
                new CharSequence[]{"腾讯X5", "CROSSWALK"}, true, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppUtils.toast(getContext(),"选择: "+ which);
                        core =   "com.bottle.lib." + ( which == 0 ? "tbsx5.X5Core":"crosswalk.CKCore" );
                        loadView();
                    }
                });
    }

    private void loadView() {
        if (core!= null){
            try {
                iWebViewInit = (IWebViewInit) ObjectRefUtil.createObject(core,new Class[]{ViewGroup.class, IBridgeImp.class},view,server);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LLog.print("准备加载页面: "+ loadUrl);
        if (iWebViewInit!=null) iWebViewInit.getProxy().loadUrl(loadUrl);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LLog.print("fragment","onActivityResult");
        server.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected boolean onBackPressed() {
        return iWebViewInit != null && iWebViewInit.onBackPressed();
    }


}
