package com.bottle.wvapp.activitys;

import android.Manifest;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.DownloadListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.tool.LaunchPage;
import com.bottle.wvapp.tool.NotifyUer;

import java.io.File;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.os.FrontNotification;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.FileUtils;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 主入口
 */
public class NativeActivity extends BaseActivity implements PermissionApply.Callback, DownloadListener {
    //权限数组
    private String[] permissionArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.READ_CONTACTS,//读取联系人
            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
            Manifest.permission.CALL_PHONE // 拨号
    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        NativeServerImp.bindActivity(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);

        /* 进度条 */
        final ProgressBar progressBar = findViewById(R.id.progress_bar);
        /* web页面加载层 */
        final FrameLayout layout = findViewById(R.id.container);

        LaunchPage.start(this, new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(final int current) {
                NativeActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar!=null){
                            progressBar.setProgress(current);
                            if (current==100){
                                progressBar.setVisibility(View.GONE);
                            }else{
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        });

        // 使用
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        // 加载页面
        loadWebMainPage(BuildConfig._WEB_HOME_URL,layout,NativeServerImp.iBridgeImp,this);
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
    }

    //忽略电源回调
    @Override
    public void onPowerIgnoreGranted() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionApply.permissionCheck(); //权限检测
    }


    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {

        File rootDir =
                new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separatorChar + "一块医药");
        final String fileName = url.substring(url.lastIndexOf("/")+1);
        if (!rootDir.exists()){
            if (!rootDir.mkdirs()){
                rootDir = getCacheDir();
            }
        }

        final File file = new File(rootDir,fileName);
        String msg = " 文件名:\t"+fileName+"\n" +
                "文件大小:\t"+ (FileUtils.byteLength2StringShow(contentLength))+"\n" +
                "存储目录:\t"+rootDir.getAbsolutePath()+"\n\t";
        if (file.exists()){
            msg += "存在同名文件,下载将进行覆盖";
        }
        //下载弹窗
        DialogUtil.build(this,
                "无法在线预览,是否立即下载",
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
                            //立即下载
                            IOUtils.run(new Runnable() {
                                @Override
                                public void run() {
                                    downloadFile(fileName,mimetype, url,file);
                                }
                            });
                        }
                    }
                });

    }

    private void downloadFile(final String fileName,final String mimetype,final String url,final File storeFile) {
        final FrontNotification notification = NotifyUer.createDownloadApkNotify(NativeServerImp.app.getApplicationContext(),
                fileName+" 正在下载");
        notification.setProgress(100, 0);
        try {
            final File file = HttpServerImp.downloadFile(url,storeFile.getAbsolutePath(),new HttpUtil.CallbackAbs(){
                @Override
                public void onProgress(File file, long progress, long total) {
                        //进度条更新
                    int current = (int)( (progress * 100f) / total );
                    notification.setProgress(100, current);
                }
            });

            final String resMsg = ( file!=null? "下载完成 " : "下载失败 " ) + storeFile.getAbsolutePath();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AppUtils.toast(NativeActivity.this,resMsg);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            notification.cancelNotification();
        }
    }


}
