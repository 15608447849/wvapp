package com.bottle.wvapp.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bottle.wvapp.jsprovider.NativeMethodCallImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.util.GsonUtils;

/**
 * Created by Leeping on 2019/7/1.
 * email: 793065165@qq.com
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IWXAPI wxapi = ApplicationAbs.getApplicationObject(IWXAPI.class);
        if (wxapi!=null){
            wxapi.handleIntent(getIntent(), this);
        }

    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        IWXAPI wxapi = ApplicationAbs.getApplicationObject(IWXAPI.class);
        if (wxapi!=null){
            wxapi.handleIntent(getIntent(), this);
        }
    }

    @Override
    public void onReq(BaseReq req) {

    }
    @Override
    public void onResp(BaseResp resp) {
        String flag = "";
        // {"prepayId":"wx201539564014345b5cedbdc32fe2a70000","returnKey":"","errCode":0}
        if(resp.getType() == ConstantsAPI.COMMAND_PAY_BY_WX){
            // 0 成功, -1 错误, -2 用户取消
            flag = "移动APP方案";
        }
        // 易宝支付 微信小程序支付方案
        if (resp.getType() == ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM) {
//            WXLaunchMiniProgram.Resp launchMiniProResp = (WXLaunchMiniProgram.Resp) resp;
//            String extraData =launchMiniProResp.extMsg; //对应小程序组件 <button open-type="launchApp"> 中的 app-parameter 属性
            flag = "易宝小程序方案";
        }
        LLog.print("微信支付 "+flag+" 结果: " + GsonUtils.javaBeanToJson(resp));
        finish();
    }
}
