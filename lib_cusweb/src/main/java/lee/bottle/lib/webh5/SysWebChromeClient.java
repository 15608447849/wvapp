package lee.bottle.lib.webh5;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.EditText;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtils;


public class SysWebChromeClient extends WebChromeClient {

    private boolean isDebug;

    private final SysWebView h;

    protected SysWebChromeClient(SysWebView webView) {
        this.h = webView;
    }

    // 页面加载进度状态
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        if (isDebug) LLog.print(this + " onProgressChanged URL : "+ view.getUrl() +" 进度: "+ newProgress);
        if (h.webProgressI!=null) h.webProgressI.updateProgress(view.getUrl(),newProgress,false);
    }

    // 弹窗
    @Override
    public boolean onJsAlert(final WebView view, String url, final String message, final JsResult result) {

        if (h.onAlertI!=null){
            h.onAlertI.onJsAlert(view,url,message,result);
        }else{
            AppUtils.toastLong(view.getContext(),message);
            result.confirm();
        }
        return true;
    }

    // 选择确认框
    @Override
    public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {

        if (h.onConfirm!=null){
            h.onConfirm.onJsConfirm(view,url,message,result);
        }else{

            DialogUtils.dialogSimple2(view.getContext(), message, "确认", new DialogUtils.Action0() {
                @Override
                public void onAction0() {
                    result.confirm();
                }
            }, "取消", new DialogUtils.Action0() {
                @Override
                public void onAction0() {
                    result.cancel();
                }
            });

        }
        return true;
    }

    // 输入确认框
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
        if (h.onConfirm!=null){
            h.onPrompt.onJsPrompt(view,url,message,defaultValue,result);
        }else{

            final EditText editText = new EditText(view.getContext());
            editText.setText(defaultValue);
            new AlertDialog.Builder(view.getContext())
                    .setTitle(message)
                    .setView(editText)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            result.confirm(editText.getText().toString());
                        }
                    }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    result.cancel();
                }
            }).setCancelable(false).show();

        }
        return true;
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        if (isDebug) LLog.print("[网页标题]\t"+title);
    }

    // 控制台消息
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {


        Log.w("[CONSOLE]", "file: " + consoleMessage.sourceId() + " (" + consoleMessage.lineNumber() + ") " + consoleMessage.messageLevel() + " >>\t" + consoleMessage.message());
        return true;
    }


    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        callback.invoke(origin, true, false);
    }

    @Override
    public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        //文件选择
        return h.onShowFileChooser(filePathCallback,fileChooserParams);
    }


}
