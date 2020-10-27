package com.bottle.wvapp.activitys;

import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

/**
 * Created by Leeping on 2020/10/19.
 * email: 793065165@qq.com
 */
public class BaseActivity extends AppCompatActivity {
    private Handler mHandler = new Handler();

    /* webView实现 */
    private IWebViewInit iWebViewInit;

    private String webMainUrl;

    //初始化应用
    protected void loadWebMainPage(String webMainUrl, ViewGroup viewGroup, IBridgeImp bridge,DownloadListener listener) {
        //加载页面
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            AppUtils.toast(this,"抱歉,应用无法兼容ANDROID 6.0以下版本");
            return;
        }

        if (iWebViewInit == null){
            iWebViewInit = IWebViewInit.createIWebView("lee.bottle.lib.toolset.web.SysCore",
                    this, viewGroup, bridge);
            clearWeb(true);
        }
        if (iWebViewInit!=null) iWebViewInit.setDownloadListener(listener);
        this.webMainUrl = webMainUrl;
        openWebPage(this.webMainUrl);
    }

    public void reloadWebMainPage(){
        openWebPage(this.webMainUrl);
    }

    public void openWebPage(String url){
        if (iWebViewInit!=null){
            LLog.print("打开链接: "+ url);
            iWebViewInit.getProxy().loadUrl(url);
        }
    }

    public void clearWeb(boolean includeDiskFiles){
        if (iWebViewInit!=null) iWebViewInit.clear(includeDiskFiles);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (iWebViewInit!=null) iWebViewInit.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    /** 捕获返回键 */
    private long cur_back_time = -1;
    @Override
    public void onBackPressed() {
        if (iWebViewInit != null && iWebViewInit.onBackPressed()) return;
        final Runnable resetBack = new Runnable() {
            @Override
            public void run() {
                cur_back_time = -1; //重置
            }
        };
        if (cur_back_time == -1){
            Toast.makeText(this,"再次点击将退出应用",Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(resetBack,2000);
            cur_back_time = System.currentTimeMillis();
        }else{
            if (System.currentTimeMillis() - cur_back_time < 100) {
                cur_back_time = System.currentTimeMillis();
                return;
            }
            mHandler.removeCallbacks(resetBack);
            super.onBackPressed();
        }
    }
    @Override
    protected void onDestroy() {
        if (iWebViewInit!=null){
            iWebViewInit.clear(false);
            iWebViewInit = null;
        }
        super.onDestroy();
    }


}
