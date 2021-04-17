package com.bottle.wvapp.app;

import android.content.Intent;

import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.services.IMService;
import com.bottle.wvapp.tool.AppCrashExcept;

import lee.bottle.lib.toolset.os.ApplicationAbs;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs  {

    @Override
    protected void onCreateByApplicationMainProgress(String processName) {
        // 设置全局异常捕获
        setCrashCallback(new AppCrashExcept(this));
        // 初始化后端交互服务器
        NativeServerImp.init(this);

    }

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        BusinessData.settingApplication(this);
    }
}
