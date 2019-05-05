package com.bottom.wvapp;

import com.bottom.abs.ApplicationAbs;
import com.bottom.wvapp.service.BackServerImp;
import com.onek.client.IceClient;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WvApplication extends ApplicationAbs {

    @Override
    protected void onCreateByAllProgress(String processName) {
        if (processName.contains(":location")) return;
        super.onCreateByAllProgress(processName);
        //设置服务器信息
        settingServerInfo();
        //创建 web view
        createWebView();
    }

    private void createWebView() {
        //pass
    }

    private void settingServerInfo() {
        BackServerImp.start(new IceClient("DemoIceGrid","114.116.149.145",4061));
    }

}
