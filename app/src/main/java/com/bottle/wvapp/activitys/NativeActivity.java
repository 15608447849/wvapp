package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.R;
import com.bottle.wvapp.app.ApplicationDevInfo;
import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.services.FloatWindowView;
import com.bottle.wvapp.tool.WebResourceCache;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.ArrayList;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;
import static com.bottle.wvapp.app.BusinessData.getCurrentDevCompanyID;
import static lee.bottle.lib.toolset.util.AppUtils.getClipboardContent;
import static lee.bottle.lib.toolset.util.AppUtils.setClipboardContent;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 主入口
 */
public class NativeActivity extends BaseActivity implements PermissionApply.Callback, DownloadListener {
    //权限数组
    private String[] permissionArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
//            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.READ_CONTACTS,//读取联系人
//            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
//            Manifest.permission.CALL_PHONE // 拨号
    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this, permissionArray,this);

    public void permissionQuery(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                permissionApply.permissionCheck(); //权限检测
//                permissionApply.askFloatWindowPermission();// 请求弹窗权限
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // 加载设备标识
        ApplicationDevInfo.load(getApplication());

        // 绑定activity
        NativeServerImp.bindActivity(this);

//        permissionQuery();

//        LLog.print(this+" 创建 Application: " + getApplication());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_native);

        /* web页面错误监听 */
        webPageErrorMonitor();

        /* 页面首次加载处理 */
        launchCompleteShowWebView();

        /* web资源缓存处理 */
        JSUtils.webResourceRequestI = new WebResourceCache();

        /* web 弹窗处理 */
        JSUtils.onAlertI = new JSUtils.AlertMessageI() {
            @Override
            public void onJsAlert(final WebView view, final String url, final String message, final JsResult result) {
                result.confirm();
                LLog.print("[onJsAlert] " + message);
                AppUtils.toastLong(view.getContext(), message);
            }
        };

        /* web页面容器层 */
        final FrameLayout layout = findViewById(R.id.container);
        /* 加载页面 */
        loadWebMainPage(layout,NativeActivity.this);

    }




    /* web页面加载完成 */
    private void launchCompleteShowWebView(){
        //设置首次加载处理
        JSUtils.webProgressI = new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(String url,int current,boolean isForce) {
                //LLog.print("链接加载进度: "+ url+" , "+ current+" , " + isForce);
                if (current>=100 && isForce){
                    JSUtils.webProgressI = null;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            getWindow().getDecorView().setBackgroundResource(0);// 移除背景资源
                            getWindow().getDecorView().setSystemUiVisibility(0);// 清空UI选项
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// 清除全屏

                            // 展开 webView 容器层
                            FrameLayout frameLayout = findViewById(R.id.container);
                            ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                            if (!(layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT
                                    && layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT)){
                                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                frameLayout.setLayoutParams(layoutParams);
                            }

                            accessSharedContent();

                        }
                    });


                }
            }
        };


    }

    /*webview 页面错误监听*/
    private void webPageErrorMonitor(){
        JSUtils.loadErrorI = new JSUtils.LoadErrorI() {
            @Override
            public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError,int errorCount) {

                Uri uri =  webResourceRequest.getUrl();


                if (uri == null || !uri.toString().startsWith(webMainUrl)) return;

                String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString());

                LLog.print("错误请求( "+ errorCount+" ): " + uri +" mimeType: "+ extension);

                Intent intent = new Intent(NativeActivity.this,ErrorActivity.class);

                int errorCode = 0;

                String errorText = "";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    errorCode = webResourceError.getErrorCode();
                    errorText = JSUtils.webViewErrorCodeConvertString(errorCode);
                    intent.putExtra("errorText",errorText);
                }

                if (JSUtils.webProgressI==null && extension!=null && (extension.equals("js") || extension.equals("css"))) {
                    if (errorText.length() > 0){
                        Snackbar.make(webView,errorText,Snackbar.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (JSUtils.webProgressI!=null || uri.toString().equals(_WEB_HOME_URL)){
                    intent.putExtra("reload",true);
                }

                if (errorCode<0 && errorCode!=-1 && errorCode!=-15){
                    // 打开错误页面,并结束当前页面
                    NativeActivity.this.startActivity(intent);
                }

            }
        };

    }



    /* 权限审核回调 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionApply != null) permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (permissionApply != null) permissionApply.onActivityResult(requestCode, resultCode, data);
        NativeServerImp.iBridgeImp.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    //授权成功回调
    @Override
    public void onPermissionsGranted() {
        // 刷新设备标识
        ApplicationDevInfo.load(getApplication());
        // 刷新本地企业信息
        int compId = getCurrentDevCompanyID(true,NativeServerImp.client);
        // 重新加载页面
        if (compId > 0) reloadWebMainPage();
    }

    //忽略电源回调
    @Override
    public void onPowerIgnoreGranted() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }


    @Override
    protected void onResume() {
        super.onResume();
//        permissionApply.permissionCheck(); //权限检测
        Intent intent = getIntent();
        if (intent != null){

            //LLog.print("onResume intent = "+ intent);

            boolean isReload = intent.getBooleanExtra("reload",false);
            intent.removeExtra("reload");
            if (isReload) reloadWebMainPage();

            ArrayList<String> list = intent.getStringArrayListExtra("notify_param");
            intent.removeExtra("notify_param");
            if (list!=null) NativeServerImp.notifyEntryToJs(list.get(0)); //跳转到指定页面

            // 强制退出
            boolean isForceLogout = intent.getBooleanExtra("forceLogout",false);
            intent.removeExtra("forceLogout");
            if (isForceLogout) NativeServerImp.forceLogout();

            // 支付结果
            String pushPaySuccessMessageToJsStr = intent.getStringExtra("pushPaySuccessMessageToJs");
            intent.removeExtra("pushPaySuccessMessageToJs");
            if (pushPaySuccessMessageToJsStr!=null)  NativeServerImp.pushPaySuccessMessageToJs(pushPaySuccessMessageToJsStr);

            // 推送消息
            String pushMessageToJsStr = intent.getStringExtra("pushMessageToJs");
            intent.removeExtra("pushMessageToJs");
            if (pushMessageToJsStr!=null)  NativeServerImp.pushMessageToJs(pushMessageToJsStr);

            // 弹窗提醒
            String alertTipWindowStr = intent.getStringExtra("alertTipWindow");
            intent.removeExtra("alertTipWindow");
            if (alertTipWindowStr!=null) {
                LLog.print("接收alert: "+ alertTipWindowStr);
                DialogUtil.dialogSimple(this, alertTipWindowStr, "我知道了", null).show();
            }

        }

        accessSharedContent();

    }

    /* 获取分享内容 */
    private void accessSharedContent() {

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String content = getClipboardContent(NativeActivity.this);

                if (content.endsWith("-onekdrug")){
                    NativeServerImp.enterApp(content);
                    setClipboardContent(NativeActivity.this,"");
                }

            }
        },500);
    }

    @Override
    protected void onDestroy() {
//        LLog.print(this+" 销毁 Application: " + getApplication());

        NativeServerImp.unbindActivity();
        super.onDestroy();
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {
        File rootDir = ApplicationAbs.getApplicationDIR("下载");
        if (rootDir == null) return;
        final String fileName = url.substring(url.lastIndexOf("/")+1);
        final File file = new File(rootDir,fileName);
        String msg =
                "文 件 名:\t"+fileName+"\n" +
                "文件大小:\t"+ (FileUtils.byteLength2StringShow(contentLength))+"\n" +
                "存储目录:\t"+rootDir.getAbsolutePath()+"\n\t";
        if (file.exists()){
            msg += "存在同名文件,下载将进行覆盖";
        }
        //下载弹窗
        DialogUtil.build(this,
                "是否立即下载",
                msg,
                R.drawable.ic_update_version,
                "其他应用打开",
                "进入后台下载",
                null,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            //其他应用打开
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        }

                        if (which == DialogInterface.BUTTON_NEGATIVE){
                            //加入队列下载
                            HttpServerImp.addDownloadFileToQueue(new HttpServerImp.DownloadTask(url,file.getPath(),new HttpUtil.CallbackAbs(){

                                @Override
                                public void onResult(HttpUtil.Response response) {
                                    final File storeFile = response.getData();
                                    final String resMsg = storeFile + " 下载完成";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AppUtils.toastLong(NativeActivity.this,resMsg);
                                        }
                                    });

                                }
                            }));
                        }

//                        if (which == DialogInterface.BUTTON_NEUTRAL){
//                            openWebPage("http://erp.onekdrug.com:8877/pdfjs/web/viewer.html?file="+url);
//                        }
                    }
                });

    }

}
