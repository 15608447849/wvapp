package lee.bottle.lib.toolset.web;

import android.net.Uri;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;

import static android.webkit.ConsoleMessage.MessageLevel.ERROR;
import static lee.bottle.lib.toolset.web.JSUtils.progressHandler;

/**
 * Created by Leeping on 2019/7/7.
 * email: 793065165@qq.com
 */
public class SysWebChromeClient extends WebChromeClient {

    private SysCore core;

    SysWebChromeClient(lee.bottle.lib.toolset.web.SysCore sysCore) {
        core = sysCore;
    }

    //进度状态
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
//        LLog.print("onProgressChanged "+ view.getUrl() +" 进度: "+ newProgress);
        progressHandler(view.getUrl(),newProgress,false);
    }

    @Override
    public boolean onJsAlert(final WebView view, String url, final String message, final JsResult result) {

        if (JSUtils.onAlertI!=null){
            JSUtils.onAlertI.onJsAlert(view,url,message,result);
        }else{
            view.post(new Runnable() {
                @Override
                public void run() {
                    AppUtils.toastLong(view.getContext(),message);
                    result.confirm();
                }
            });
        }
        return true;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        String fileName =  consoleMessage.sourceId();

        if (consoleMessage.messageLevel()==ERROR){
            LLog.print(
                    "浏览器-["+consoleMessage.messageLevel()+"]\n"+consoleMessage.message()
                            + (consoleMessage.messageLevel().name().equalsIgnoreCase("error") ?
                            "\n" + fileName +":"+consoleMessage.lineNumber():"")
            );
        }


//        LLog.print("浏览器\t" +fileName+"("+ consoleMessage.lineNumber() +")\n" + consoleMessage.message());
//        if ( consoleMessage.message().contains("JNB ok")){
//            JSUtils.progressHandler(null,100,true);
//        }
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

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        //文件选择
        return JSUtils.onShowFileChooser(core.getCurrentBinder(),webView,filePathCallback,fileChooserParams);
    }

}
