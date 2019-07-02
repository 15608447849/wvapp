package com.bottle.wvapp.app;

import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.toolset.os.ApplicationAbs;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs {
    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        NativeServerImp.bindApplication(this);
    }
}
