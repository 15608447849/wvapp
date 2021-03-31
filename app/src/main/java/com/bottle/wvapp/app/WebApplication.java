package com.bottle.wvapp.app;

import android.os.Environment;

import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.tool.AppCrashExcept;
import com.bottle.wvapp.tool.WebResourceCache;

import java.io.File;
import java.util.Timer;

import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.web.JSUtils;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs  {

    @Override
    protected void onCreateByApplicationMainProgress(String processName) {
        // 设置全局定时器
        ApplicationAbs.putApplicationObject(new Timer());
        // 设置全局应用目录
        ApplicationAbs.setApplicationDir_OS_M(this,"1k.一块医药");
        // 设置全局异常捕获
        setCrashCallback(new AppCrashExcept(this));
        // 设置全局webView
        ApplicationAbs.putApplicationObject(
                IWebViewInit.class,IWebViewInit.createIWebView("lee.bottle.lib.toolset.web.SysCore",
                this, NativeServerImp.iBridgeImp));


}

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
    }
}
