package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.tool.WebResourceCache;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;

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
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

//        LLog.print(this+" 创建 Application: " + getApplication());
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;

        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_native);

        /* 初始化 */
        launchInit();

        /* web资源缓存处理 */
        JSUtils.webResourceRequestI = new WebResourceCache();

        /* web 弹窗处理 */
        JSUtils.onAlertI = new JSUtils.AlertMessageI() {
            @Override
            public void onJsAlert(final WebView view, final String url, final String message, final JsResult result) {
                LLog.print("[onJsAlert] " + message);
                result.confirm();

                DialogUtil.dialogSimple(view.getContext(), message, "确认", new DialogUtil.Action0() {
                    @Override
                    public void onAction0() {
                        result.confirm();
                    }
                });
            }
        };

        /* web页面加载层 */
        final FrameLayout layout = findViewById(R.id.container);

        /* 加载页面 */
        loadWebMainPage(_WEB_HOME_URL,layout,this);
    }



    private void launchInit(){
        JSUtils.loadErrorI = new JSUtils.LoadErrorI() {
            @Override
            public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError,int errorCount) {

                Uri uri =  webResourceRequest.getUrl();


                if (uri == null || !uri.toString().startsWith(_WEB_HOME_URL)) return;

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

                if (uri.toString().equals(_WEB_HOME_URL)){
                    intent.putExtra("reload",true);
                }

                if (extension!=null && (extension.equals("js") || extension.equals("css"))) {
                   if (errorText!=null && errorText.length()>0){
                       Snackbar.make(webView,errorText,Snackbar.LENGTH_SHORT).show();
                   }
                    return;
                }

                if (errorCode<0 && errorCode!=-1 && errorCode!=-15){
                    // 打开错误页面,并结束当前页面
                    NativeActivity.this.startActivity(intent);
                }

            }
        };
        //设置首次加载处理
        JSUtils.webProgressI = new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(String url,int current,boolean isForce) {
                if (current>=100 && isForce){
                    stopLaunch();
                }
            }
        };
    }

    /*private TimerTask stopLaunchImageShowWebTimeTask = null;

    private void delayTimeStop(int delay){
        // 延时指定秒后销毁
        Timer timer =  ApplicationAbs.getApplicationObject(Timer.class);
        if (timer == null) {
            return;
        }

        if (stopLaunchImageShowWebTimeTask!=null){
            stopLaunchImageShowWebTimeTask.cancel();
        }

        stopLaunchImageShowWebTimeTask = new TimerTask() {
            @Override
            public void run() {
                stopLaunch();
            }
        };

        timer.schedule(stopLaunchImageShowWebTimeTask,delay);
    }*/

    private void stopLaunch(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 设置activity不是全屏
                getWindow().getDecorView().setBackgroundResource(0);
                getWindow().getDecorView().setSystemUiVisibility(0);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // 展开 webview 容器层
                FrameLayout frameLayout = findViewById(R.id.container);
                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
//                LLog.print("展开 WEB VIEW 容器, 当前大小: "+ layoutParams.width+","+layoutParams.height);

                if (!(layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT
                        && layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT)){
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    frameLayout.setLayoutParams(layoutParams);
                }

                //loadWebPageProgress();
            }
        });
    }

//    private void loadWebPageProgress() {
//        //设置页面加载滚动条
//        JSUtils.webProgressI = new JSUtils.WebProgressI() {
//            @Override
//            public void updateProgress(String url,final int current,boolean isForce) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        /* 进度条 */
//                        final ProgressBar progressBar = findViewById(R.id.progress_bar);
//                        if (progressBar!=null){
//                            progressBar.setProgress(current);
//                            if (current==100){
//                                progressBar.setVisibility(View.GONE);
//                            }else{
//                                progressBar.setVisibility(View.VISIBLE);
//                            }
//                        }
//                    }
//                });
//            }
//        };
//    }


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
        ApplicationAbs.setApplicationDir_OS_M(this,"1k.一块医药");
        NativeServerImp.loadDEVID();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    //忽略电源回调
    @Override
    public void onPowerIgnoreGranted() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionApply.permissionCheck(); //权限检测
        Intent intent = getIntent();
        if (intent != null){

            boolean isReload = intent.getBooleanExtra("reload",false);
            intent.removeExtra("reload");
            if (isReload) reloadWebMainPage();

            ArrayList<String> list = intent.getStringArrayListExtra("notify_param");
            intent.removeExtra("notify_param");
            if (list!=null) NativeServerImp.notifyEntryToJs(list.get(0)); //跳转到指定页面

        }
    }

    @Override
    protected void onDestroy() {
//        LLog.print(this+" 销毁 Application: " + getApplication());
//        Timer timer = ApplicationAbs.getApplicationObject(Timer.class);
//        if (timer!=null){
//            ApplicationAbs.delApplicationObject(Timer.class);
//            timer.cancel();
//        }
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
