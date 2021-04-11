package com.bottle.wvapp.activitys;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;

/**
 * Created by Leeping on 2020/10/19.
 * email: 793065165@qq.com
 */
public class BaseActivity extends AppCompatActivity {

    /* webView实现 */
    private lee.bottle.lib.toolset.web.SysCore iWebViewInit;
    protected Handler mHandler = new Handler();
    protected String webMainUrl = _WEB_HOME_URL;

    //初始化应用
    protected void loadWebMainPage(ViewGroup viewGroup, DownloadListener listener) {
        //加载页面
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            AppUtils.toastLong(this,"应用暂不支持 Android 6.0 以下版本");
            return;
        }

        iWebViewInit = new lee.bottle.lib.toolset.web.SysCore(this, NativeServerImp.iBridgeImp);

        iWebViewInit.bind(this,viewGroup,listener);

        reloadWebMainPage();
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
    protected void onDestroy() {
        if (iWebViewInit!=null){
            iWebViewInit.unbind();
            iWebViewInit=null;
        }
        super.onDestroy();
    }

    @Override
    public void finish() {
        if (isExit){
            super.finish();
            android.os.Process.killProcess(android.os.Process.myPid());
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
