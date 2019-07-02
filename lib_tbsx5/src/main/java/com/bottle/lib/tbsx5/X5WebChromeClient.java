package com.bottle.lib.tbsx5;

import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.export.external.interfaces.JsResult;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.DialogUtil;

import static lee.bottle.lib.toolset.jsbridge.JSUtils.progressHandler;

/**
 * Created by Leeping on 2019/6/10.
 * email: 793065165@qq.com
 */
public class X5WebChromeClient extends WebChromeClient {


    //进度状态
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        progressHandler(view.getContext(),newProgress);
    }

   @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissionsCallback callback) {
        callback.invoke(origin, true, false);
    }



    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String fileName =  consoleMessage.sourceId();
        LLog.print(
                "浏览器控制台输出 - ["+consoleMessage.messageLevel()+"]\t"+consoleMessage.message()
        );
        return true;
    }

    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
        DialogUtil.dialogSimple2(view.getContext(), message, "确认", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.confirm();
            }
        }, "取消", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.confirm();
            }
        });
    return true;
    }
}
