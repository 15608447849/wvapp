package com.bottle.wvapp.activitys;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
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
    protected void loadWebMainPage(String webMainUrl, ViewGroup viewGroup, DownloadListener listener) {
        //加载页面
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            AppUtils.toastLong(this,"应用暂不支持 Android 6.0 以下版本");
            return;
        }

        //尝试从application中获取webview
        iWebViewInit = ApplicationAbs.getApplicationObject(IWebViewInit.class);

        /*if (iWebViewInit == null){
            iWebViewInit = IWebViewInit.createIWebView("lee.bottle.lib.toolset.web.SysCore",this,NativeServerImp.iBridgeImp);
            if (iWebViewInit!=null){
                ApplicationAbs.putApplicationObject(IWebViewInit.class,iWebViewInit);
            }
        }*/

        if (iWebViewInit == null) {
            throw new RuntimeException("没有可用的WebView实例");
        }

        iWebViewInit.bind(this,viewGroup,listener);
        this.webMainUrl = webMainUrl;
        openWebPage(this.webMainUrl);
    }

    public void reloadWebMainPage(){
        openWebPage(this.webMainUrl);
    }

    public void openWebPage(String url){
        if (iWebViewInit!=null){
            clearWeb(true);
            iWebViewInit.getProxy().loadUrl(url);
            LLog.print("打开链接: "+ url);
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

    private boolean isExit;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (iWebViewInit!=null) iWebViewInit.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    /** 捕获返回键 */
    private long cur_back_time = -1;
    final Runnable resetBack = new Runnable() {
        @Override
        public void run() {
            cur_back_time = -1; //重置
        }
    };
    @Override
    public void onBackPressed() {

        if (iWebViewInit != null && iWebViewInit.onBackPressed()) return;

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
            isExit = true;
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        NativeServerImp.bindActivity(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        NativeServerImp.unbindActivity();
        if (iWebViewInit!=null){
            iWebViewInit.unbind();

            iWebViewInit = null;
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        if (isExit){
            super.finish();
        }else{
            moveTaskToBack(true);
        }
    }

    // 解决系统改变字体大小的时候导致的界面布局混乱的问题
    // https://blog.csdn.net/lsmfeixiang/article/details/42213483
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config=new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config,res.getDisplayMetrics() );
        return res;
    }
}
