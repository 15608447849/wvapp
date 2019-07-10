package com.bottle.wvapp.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;

/**
 * Created by Leeping on 2019/7/1.
 * email: 793065165@qq.com
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeServerImp.INSTANCE.caller.wxapi.handleIntent(getIntent(), this);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NativeServerImp.INSTANCE.caller.wxapi.handleIntent(intent, this);
    }
    @Override
    public void onReq(BaseReq req) {
    }
    @Override
    public void onResp(BaseResp resp) {
        LLog.print("微信支付结果: " + GsonUtils.javaBeanToJson(resp));
        NativeServerImp.INSTANCE.caller.wxpayRes = resp.errCode;
        NativeServerImp.INSTANCE.threadNotify();
        finish();
    }
}
