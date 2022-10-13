package lee.bottle.lib.webh5;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.webh5.interfaces.DeviceInfoI;
import lee.bottle.lib.webh5.interfaces.PromptMessageI;

import lee.bottle.lib.webh5.interfaces.AlertMessageI;
import lee.bottle.lib.webh5.interfaces.ConfirmMessageI;
import lee.bottle.lib.webh5.interfaces.LoadErrorI;
import lee.bottle.lib.webh5.interfaces.WebProgressI;
import lee.bottle.lib.webh5.interfaces.WebResourceRequestI;

public class SysWebView extends WebView {

    //文件选择结果标识
    private static final int REQUEST_SELECT_FILE = 254;
    //文件选择
    private static ValueCallback<Uri[]> _filePathCallback;

    private Activity activity;

    public WebProgressI webProgressI;
    public WebResourceRequestI webResourceRequestI = WebResourceCache.getInstance();
    public LoadErrorI loadErrorI;
    public AlertMessageI onAlertI;
    public ConfirmMessageI onConfirm;
    public DeviceInfoI deviceI;
    public PromptMessageI onPrompt;

    private final SysWebChromeClient chrome = new SysWebChromeClient(this);
    private final SysWebViewClient client = new SysWebViewClient(this);
    public final JSInterface jsInterface = new JSInterface(this);

    public SysWebView(@NonNull Context context) {
        super(context);
        init();
    }

    // 初始化
    private void init() {
        SysWebViewSetting.initSetting(this);
        this.setWebChromeClient(chrome);
        this.setWebViewClient(client);
    }

    /************************************************************绑定解绑activity*****************************************/


    // 绑定页面
    public void bindActivity(Activity activity){
        unbindActivity();
        this.activity = activity;
        LLog.print(this+" 绑定Activity "+ activity);
    }

    // 解绑页面
    public void unbindActivity(){
        if (this.activity!=null){
            LLog.print(this+" 解绑Activity "+  this.activity);
            this.activity = null;
        }
    }

    //绑定图层
    public void bindViewGroup(ViewGroup group){
        unbindViewGroup();
        if (group!=null){
            group.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            LLog.print(this+" 加入 "+group);
        }
    }

    // 解绑图层
    public void unbindViewGroup() {
        ViewParent parent = this.getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(this);
            LLog.print(this+" 移出 "+parent);
        }
    }

    public void bind(Activity activity,ViewGroup viewGroup){
        bindActivity(activity);
        bindViewGroup(viewGroup);
    }

    public void unbind(){
        unbindViewGroup();
        unbindActivity();
    }

    /************************************************************基本操作*****************************************/

    // 清理缓存
    public void clearCaches(boolean includeDiskFiles, boolean isClearHistory) {
//        LLog.print(this +" ("+ getUrl() +") 清理缓存");
        LLog.print(this +" 清理缓存");
        this.clearCache(includeDiskFiles);
        this.clearFormData();
        this.clearMatches();
        this.clearSslPreferences();
        if(isClearHistory){
            this.clearHistory();
            LLog.print(this +" 清理历史页面完成, 是否允许回退: "+ canGoBack());
        }
    }
    // 清理图层
    public void clearViews() {
//        LLog.print(this +" ("+ getUrl() +") 清理图层");
        LLog.print(this +" 清理图层");
        this.loadData(null,"text/html","utf-8");
        this.removeAllViews();
    }

    // 回退事件
    public boolean onBackPressed() {
//        LLog.print(this +" ("+ getUrl() +") 是否允许回退: "+ canGoBack());
//        LLog.print(this +" 回退事件,是否允许回退: "+ canGoBack());
        if (this.canGoBack()) {
            this.goBack();
            return true;
        }
        return false;
    }

    //打开连接
    public void open(String url){
        this.clearCaches(false,true);
        this.loadUrl(url);
        LLog.print(this + " 打开URL : "+ url);
    }
    //关闭
    public void close(boolean includeDiskFiles, boolean isClearHistory) {
        this.clearViews();
        this.clearCaches(includeDiskFiles,isClearHistory);
        this.unbind();
        this.destroy();
    }

    /************************************************************文件选择*****************************************/

    // 文件拣选
    private static void onFilePathCallback(Uri[] uris){
        if (_filePathCallback != null) {
            _filePathCallback.onReceiveValue(uris);
            _filePathCallback = null;
        }
    }

    // 文件选择
    boolean onShowFileChooser(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {

        try {
            if (activity != null) {
                onFilePathCallback(null);
                _filePathCallback = filePathCallback;
                activity.startActivityForResult(fileChooserParams.createIntent(), REQUEST_SELECT_FILE);
            }
        } catch (Exception e) {
            LLog.error(e);
            onFilePathCallback(null);
        }

        return true;
    }


    /************************************************************activity页面跳转回传*****************************************/

    public void onActivityResultHandle(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_FILE){
            // 文件选择
            Uri[] uris = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            onFilePathCallback(uris);
        }
    }


}
