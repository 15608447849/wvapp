package lee.bottle.lib.toolset.web;

import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.DialogUtil;

import static lee.bottle.lib.toolset.jsbridge.JSUtils.progressHandler;

/**
 * Created by Leeping on 2019/7/7.
 * email: 793065165@qq.com
 */
public class SysWebChromeClient extends WebChromeClient {
    //进度状态
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        progressHandler(view.getContext(),newProgress);
    }
    @Override
    public boolean onJsAlert(WebView view, String url, String message,final JsResult result) {
        DialogUtil.dialogSimple(view.getContext(), message, "确认", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.confirm();
            }
        });
        return true;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String fileName =  consoleMessage.sourceId();

        LLog.print(
                "浏览器控制台-["+consoleMessage.messageLevel()+"]\n"+consoleMessage.message()
                        + (consoleMessage.messageLevel().name().equalsIgnoreCase("error") ? "\n" + fileName +":"+consoleMessage.lineNumber():"")

        );

//        LLog.print("WEB LOG \n\t" + consoleMessage.message());
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
                result.cancel();
            }
        });
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

}
