package com.bottom.wvapp.app;

import com.bottom.wvapp.jsprovider.NativeServerImp;
import com.bottom.wvapp.tbsx5.TbsWebViewUtil;
import com.onek.client.IceClient;

import lee.bottle.lib.singlepageframwork.use.RegisterCentre;
import lee.bottle.lib.toolset.os.ApplicationAbs;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs {

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        TbsWebViewUtil.tbsInit(getApplicationContext());
        NativeServerImp.bindApplication(this);
        //设置服务器信息
        settingServerInfo();
        //初始化页面
        initPageInfo();
    }



    private void settingServerInfo() {
        NativeServerImp.start(new IceClient("DemoIceGrid","114.116.149.145",4061));
    }
    private void initPageInfo() {
        RegisterCentre.register(NativeServerImp.dynamicPageInformation());
    }
}
