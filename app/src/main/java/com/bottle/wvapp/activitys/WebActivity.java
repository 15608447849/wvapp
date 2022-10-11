package com.bottle.wvapp.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bottle.wvapp.beans.UrlDownloadDialog;
import com.bottle.wvapp.jsprovider.NativeActivityInterfaceDefault;
import com.bottle.wvapp.jsprovider.NativeJSInterface;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.syd.oden.circleprogressdialog.core.CircleProgressDialog;

import java.io.File;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.BaseActivity;
import lee.bottle.lib.webh5.SysWebView;
import lee.bottle.lib.webh5.interfaces.WebProgressI;


public class WebActivity extends BaseActivity {

    // 连接业务服务器及本地方法实现
    private final NativeServerImp nativeServerImp = new NativeServerImp();

    //底层图层
    protected FrameLayout frameLayout;
    // 浏览器
    protected SysWebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LLog.print(this +" ** onCreate " );
        super.onCreate(savedInstanceState);

        frameLayout = new FrameLayout(this);
        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT)
        );

        setContentView(frameLayout);


        final CircleProgressDialog circleProgressDialog = new CircleProgressDialog(this);

        circleProgressDialog.showDialog();


        webView = new SysWebView(this);

        // web下载事件
        webView.setDownloadListener(new UrlDownloadDialog(this){
            @Override
            protected void openDirectly(String url) {
                webView.open("https://view.xdocin.com/view?src="+url);
            }

            @Override
            protected void downloadNow(String url, File file) {
                super.downloadNow(url, file);
                finish();
            }

            @Override
            protected void cancelAction(String url) {
                finish();
            }
        });

        // 关联
        nativeServerImp.setNativeActivityInterface(  new NativeActivityInterfaceDefault(this,webView,
                new NativeJSInterface(webView.jsInterface,nativeServerImp)){
            @Override
            public void onJSPageInitialization() {
                // 加载完成通知
                circleProgressDialog.dismiss();
            }
        });

        webView.webProgressI = new WebProgressI() {
            @Override
            public void updateProgress(String url, int current, boolean isManual) {
                if (current>=100) {
                    circleProgressDialog.dismiss();
                }
            }
        };

        webView.bind(this,frameLayout);

        String indexURL = null;
        Intent intent = getIntent();
        if (intent != null){
            String url =  intent.getStringExtra("url");
            if (url != null){
                indexURL = url;
            }
        }

        if (indexURL == null){
            finish();
            return;
        }

        webView.open(indexURL);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        webView.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onResume() {
        LLog.print(this +" ** onResume " );
        super.onResume();

    }

    @Override
    protected void onStart() {
        LLog.print(this +" ** onStart " );
        super.onStart();
    }

    @Override
    protected void onRestart() {
        LLog.print(this +" ** onRestart " );
        super.onRestart();
    }

    // 捕获返回键 处理
    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        boolean isRollback = intent.getBooleanExtra("isRollback", true);
        if (isRollback && webView.onBackPressed()) return;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        LLog.print(this +" ** onDestroy " );
        if (webView != null){
            webView.close(true,true);
            webView = null;
        }

        super.onDestroy();
    }


}