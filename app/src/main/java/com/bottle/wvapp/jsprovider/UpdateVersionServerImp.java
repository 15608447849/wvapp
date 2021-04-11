package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.DialogInterface;
import android.os.Build;

import com.bottle.wvapp.R;
import com.bottle.wvapp.activitys.BaseActivity;
import com.bottle.wvapp.tool.NotifyUer;

import java.io.File;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.FrontNotification;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;

import static com.bottle.wvapp.tool.UploadProgressWindow.progressBarCircleDialogStop;
import static com.bottle.wvapp.tool.UploadProgressWindow.progressBarCircleDialogUpdate;

/**
 * Created by Leeping on 2019/7/5.
 * email: 793065165@qq.com
 * 更新APP版本
 */
public class UpdateVersionServerImp extends HttpUtil.CallbackAbs implements Runnable{

    private static Application app;

    /* 是否正在执行中 */
    private static volatile boolean isExecute = false;

    private static  FrontNotification notification;

    /* 初始化 */
    static void init(Application application){
        app = application;
    }

    static void execute(boolean isAuto){

        if (app == null ) throw new RuntimeException("应用未初始化");

        if (isExecute) {
            if (!isAuto) tryToast(" 正在检查版本信息,请稍等 ");
            return;
        }

        IOUtils.run(new UpdateVersionServerImp(isAuto));
    }


    private static void tryToast(final String message) {
            final Activity activity = NativeServerImp.getBaseActivity();
            if (activity!=null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppUtils.toastShort(activity,message);
                    }
                });
            }
    }

    private final boolean isAuto;

    /* 私有构造*/
    private UpdateVersionServerImp(boolean isAuto) {
        this.isAuto = isAuto;
    }

    @Override
    public void run() {
        isExecute = true;
        openProgress();
        checkVersionAndDownload();
        closeProgress();
       isExecute = false;
    }

    static boolean checkAppVersionMatch(int remote) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || remote==0)  return true;
        int localVersion = AppUtils.getVersionCode(app);
//        LLog.print("当前应用版本号: "+ localVersion+" , 服务器版本号: "+ remote);
        return  localVersion>= remote;
    }

    //打开进度条
    private void openProgress() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (notification == null) {
                notification = NotifyUer.createDownloadApkNotify(app, "下载apk文件");
                notification.setProgress(100, 0);
            }
        }
    }

    private void closeProgress() {
        if(notification != null){
            notification.cancelNotification();
            notification = null;
        }
    }

    private void checkVersionAndDownload() {
        if (!isAuto) NativeServerImp.updateServerConfigJson();
        final AppUploadConfig c = NativeServerImp.config;
        boolean isMatch = checkAppVersionMatch(c.serverVersion);

        if (isMatch) {
            if (!isAuto) tryToast("当前应用已经是最新版本("+c.serverVersion+")");
            return;
        }

        if (!isAuto) tryToast("正在下载新版本("+c.serverVersion+"),请稍等");

        File apk = new File(app.getCacheDir() + "/temp.apk");
        if (System.currentTimeMillis() - apk.lastModified() >  60 * 1000){
            //下载apk
            apk = HttpServerImp.downloadFile(c.apkLink,apk.getPath(),this);
        }

        if (apk == null) {
            if (!isAuto) tryToast("无法下载最新版本应用,请重新尝试");
            return;
        }

        //打开安装对话框
        final BaseActivity activity = NativeServerImp.getBaseActivity();
        if (activity == null) return;
        boolean isChange = apk.setLastModified(System.currentTimeMillis());
        if (!isChange){
            LLog.print("无法修改APK最近更新时间");
        }
        final File _apk = apk;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (NativeServerImp.config.forceUpdate == 0){
                    normalUpdate(activity,_apk,c.updateMessage);
                }else{
                    forceUpdate(activity,_apk,c.updateMessage);
                }
            }
        });

    }

    private void forceUpdate(final BaseActivity activity, final File file, String updateMessage) {
        DialogUtil.build(activity,
                "当前版本已过期,请更新",
                updateMessage,
                R.drawable.ic_update_version,
                "现在更新",
                "退出应用",
                null,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            //提示安装
                            AppUtils.installApk(app, file);
                        }else{
//                            activity.finish();
                            System.exit(0);
                        }
                    }
                });
    }

    private void normalUpdate(BaseActivity activity,final File file,String updateMessage) {
        DialogUtil.build(activity,
                "版本更新",
                updateMessage,
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
                            AppUtils.installApk(app, file);
                        }
                    }
                });
    }

    @Override
    public void onProgress(File file, long progress, long total) {
        //打开进度指示条的通知栏
        int current = (int)( (progress * 100f) / total );
        if (notification!=null) notification.setProgress(100, current);
        Activity activity = NativeServerImp.getBaseActivity();
        if (activity!=null && current>0){
            progressBarCircleDialogUpdate(activity,"程序更新中\n当前进度:"+current+"/"+100);
            if (current == 100){
                progressBarCircleDialogStop(activity);
            }
        }
    }
}
