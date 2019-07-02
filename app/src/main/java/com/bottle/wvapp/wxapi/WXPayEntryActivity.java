package com.bottle.wvapp.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;

/**
 * Created by Leeping on 2019/7/1.
 * email: 793065165@qq.com
 */
public class WXPayEntryActivity extends Activity implements IWXAPIEventHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeServerImp.INSTANCE.wxapi.handleIntent(getIntent(), this);
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        NativeServerImp.INSTANCE.wxapi.handleIntent(intent, this);
    }
    @Override
    public void onReq(BaseReq req) {
    }
    @Override
    public void onResp(BaseResp resp) {
        NativeServerImp.INSTANCE.wxpayRes = resp.errCode;
        NativeServerImp.INSTANCE.exeNotify();
        finish();
    }
}
