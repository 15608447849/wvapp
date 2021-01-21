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
        setCrashCallback(new AppCrashExcept(this));
        ApplicationAbs.setApplicationDir(new File(Environment.getExternalStorageDirectory(), "1k.一块医药"));
        NativeServerImp.bindApplication(this);
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                NativeServerImp.launch();
            }
        });
    }

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
    }
}
