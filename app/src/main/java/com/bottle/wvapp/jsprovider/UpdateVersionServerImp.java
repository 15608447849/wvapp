package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.content.DialogInterface;

import androidx.fragment.app.Fragment;

import com.bottle.wvapp.R;
import com.bottle.wvapp.tool.NotifyUer;

import java.io.File;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.os.FrontNotification;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;

/**
 * Created by Leeping on 2019/7/5.
 * email: 793065165@qq.com
 * 更新APP版本
 */
public class UpdateVersionServerImp extends HttpUtil.CallbackAbs implements Runnable{

    private static volatile boolean isExecute = false;

    private final boolean isAuto;

    private static  FrontNotification notification;

    private UpdateVersionServerImp(boolean isAuto) {
        this.isAuto = isAuto;
    }

    static void execute(boolean isAuto){

        if (NativeServerImp.app == null ) throw new RuntimeException("应用未初始化");

        if (isExecute) {
            if (!isAuto) tryToast("正在检查版本信息,请稍等");
            return;
        }
        IOUtils.run(new UpdateVersionServerImp(isAuto));
    }

    private static void tryToast(final String message) {
        final Fragment fragment = NativeServerImp.fragment.get();
        if (fragment!=null){
            final Activity activity = fragment.getActivity();
            if (activity!=null){
                fragment.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppUtils.toast(activity,message);
                    }
                });
            }
        }
    }

    @Override
    public void run() {
        isExecute = true;
        openProgress();
        checkVersionAndDownload();
        closeProgress();
       isExecute = false;
    }

    //打开进度条
    private void openProgress() {
            notification = NotifyUer.createDownloadApkNotify(NativeServerImp.app.getApplicationContext(),"下载apk文件");
            notification.setProgress(100,0);
    }

    private void closeProgress() {
        notification.cancelNotification();
        notification = null;
    }

    private void checkVersionAndDownload() {
        if (!isAuto) NativeServerImp.updateServerConfigJson();

        final NativeServerImp.ServerConfig c = NativeServerImp.config;

        int sv = c.serverVersion;
        int cv = AppUtils.getVersionCode(NativeServerImp.app);

        if (sv == 0 || cv >= sv) {
            if (!isAuto) tryToast("当前已经是最新版本");
            return;
        }

        if (!isAuto) tryToast("正在下载新版本,请稍等");
        //下载apk
        final File file = HttpServerImp.downloadFile(c.apkLink,NativeServerImp.app.getCacheDir() + "/temp.apk",this);
        if (file == null) {
            if (!isAuto) tryToast("无法下载最新版本app,请重新尝试");
            return;
        }

        //打开安装对话框
        Fragment fragment = NativeServerImp.fragment.get();
        if (fragment == null) return;
        final Activity activity = fragment.getActivity();
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DialogUtil.build(activity,
                        "版本更新",
                        c.updateMessage,
                        R.drawable.ic_update_version,
                        "现在更新",
                        "下次再说",
                        null,
                        0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                if (which == DialogInterface.BUTTON_POSITIVE){
                                    //提示安装
                                    AppUtils.installApk(NativeServerImp.app, file);
                                }
                            }
                        });

            }
        });

    }

//    private static NumberFormat format = NumberFormat.getPercentInstance();

    @Override
    public void onProgress(File file, long progress, long total) {
        //打开进度指示条的通知栏
        int current = (int)( (progress * 100f) / total );
        notification.setProgress(100, current);
    }
}
