package com.bottom.wvapp.tbsx5;

import com.tencent.smtt.export.external.interfaces.ConsoleMessage;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;

import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/10.
 * email: 793065165@qq.com
 */
public class X5WebChromeClient extends WebChromeClient {


    //进度状态
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        LLog.print("onProgressChanged(WebView,int):\t"+newProgress);
    }

   @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissionsCallback callback) {
        LLog.print("onGeolocationPermissionsShowPrompt(String,GeolocationPermissions)\t");
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }



    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String fileName =  consoleMessage.sourceId();
//        consoleMessage.sourceId().substring(consoleMessage.sourceId().lastIndexOf("/"));
        LLog.print(
//                "(" +fileName + ":" + consoleMessage.lineNumber() + ")" +"\n",
                "["+consoleMessage.messageLevel()+"]\t"+consoleMessage.message()
        );
        return true;
//        return super.onConsoleMessage(consoleMessage);
    }
}
