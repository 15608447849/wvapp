package com.bottle.wvapp.app;

import android.view.ViewGroup;

import lee.bottle.lib.toolset.http.FileServerClient;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.beans.BusinessData;
import com.bottle.wvapp.tool.AppCrashExcept;
import com.onek.client.IceClient;

import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.ApplicationDevInfo;
import lee.bottle.lib.webh5.SysWebViewSetting;

import static com.bottle.wvapp.BuildConfig._FILE_SERVER_URL;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs  {

    /* 设备类型 */
    public static final String DEVTYPE = "PHONE";

    /* ICE连接客户端 */
    public final static IceClient iceClient = new IceClient(BuildConfig._ICE_TAG,BuildConfig._ADDRESS,BuildConfig._ARGS).startCommunication();

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        // 设置文件服务器地址
        FileServerClient.init(this,_FILE_SERVER_URL);
        // 设置全局异常捕获
        setCrashCallback(new AppCrashExcept(this));
        // 加载设备标识及目录
        ApplicationDevInfo.init(this,"1k.一块医药");
        // 业务数据处理
        BusinessData.settingApplication(this);
    }

    @Override
    protected void onCreateByApplicationMainProgress(String processName) {
        // 处理webview
        SysWebViewSetting.initGlobalSetting(this);
        // 创建webview
        GlobalMainWebView.init(this);

    }

}
