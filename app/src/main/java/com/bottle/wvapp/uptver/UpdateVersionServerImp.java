package com.bottle.wvapp.uptver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bottle.wvapp.R;
import com.bottle.wvapp.tool.NotifyUer;

import java.io.File;

import lee.bottle.lib.toolset.http.FileServerClient;
import lee.bottle.lib.toolset.http.HttpUtils;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.FrontNotification;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtils;

/**
 * Created by Leeping on 2019/7/5.
 * email: 793065165@qq.com
 * 更新APP版本
 */
public class UpdateVersionServerImp{


    /* 检测版本更新次数 */
    private static int checkVersionUpdateIndex =0;

    /* 通知栏 */
    private static  FrontNotification notification;

    /* 进度条 */
    private static AlertDialog mProgress;

    @SuppressLint("StaticFieldLeak")
    private static TextView mProgress_tv;

    private static boolean isHind = false;

    /* 是否正在执行中 */
    private static volatile boolean isExecute = false;

    // 尝试提示
    private static void activityToast(final Activity activity , final boolean showMsg, final String message) {
        if (showMsg && activity!=null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AppUtils.toastShort(activity,message);
                }
            });
        }
    }

    // 检查版本号
    private static boolean checkAppVersionMatch(Activity activity,int remote) {
        if (remote==0)  return true;
        int localVersion = AppUtils.getVersionCode(activity);
        LLog.print("当前应用版本号: "+ localVersion+" , 服务器版本号: "+ remote);
        return  localVersion>= remote;
    }

    private static void progressBarCircleDialogUpdate(final Activity activity, final String text) {
        if (activity == null || isHind) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgress == null){
                    AlertDialog.Builder progressBuild = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT);
                    LayoutInflater inflater = LayoutInflater.from(activity);
                    @SuppressLint("InflateParams")
                    View view = inflater.inflate(R.layout.up_progress, null);
                    mProgress_tv = view.findViewById(R.id.progress_tv);
                    progressBuild.setView(view);
                    progressBuild.setPositiveButton("隐藏", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isHind = true;
                            mProgress = null;
                            mProgress_tv = null;
                        }
                    });
                    mProgress = progressBuild.create();
                    mProgress.setCanceledOnTouchOutside(false);
                    mProgress.setCancelable(false);
                    mProgress.show();
                }

                if (mProgress_tv!=null){
                    mProgress_tv.setText(text);
                }
            }
        });
    }

    private static void progressBarCircleDialogStop(final Activity activity) {
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isHind = false;
                if (mProgress_tv!=null) mProgress_tv = null;
                if (mProgress!=null) {
                    mProgress.cancel();
                    mProgress.dismiss();
                    mProgress = null;
                }
            }
        });

    }

    private static void updateProgressBar(Activity activity,int current){
        if (activity!=null && current>0){
            progressBarCircleDialogUpdate(activity,"程序更新中\n当前进度:"+current+"/"+100);
            if (current == 100){
                progressBarCircleDialogStop(activity);
            }
        }
    }

    //打开进度条
    private static void openNoticeBarProgress(Activity activity,String title) {
        if (notification == null && activity!=null) {
            notification = NotifyUer.createDownloadApkNotify(activity, title);
            notification.setProgress(100, 0);
        }
    }

    private static void updateNoticeBarProgress(int current){
        //打开进度指示条的通知栏
        if (notification!=null) notification.setProgress(100, current);
    }

    //关闭进度条
    private static void closeNoticeBarProgress() {
        if(notification != null){
            notification.cancelNotification();
            notification = null;
        }
    }


    //检查版本进度并下载
    private static void checkVersionAndDownload(final Activity activity,final boolean showMsg) {
        try{
            checkVersionUpdateIndex++;
            if ( activity == null) return;

            if (isExecute) {
                activityToast(activity,showMsg,"正在更新版本");
                return;
            }

            isExecute = true;

            final AppUploadConfig config = AppUploadConfig.load();

            boolean isMatch = checkAppVersionMatch(activity, config.serverVersion);

            if (isMatch) {
                activityToast(activity,showMsg,"当前应用已经是最新版本("+config.serverVersion+")");
                return;
            }

            File apk = new File(activity.getCacheDir() + "/temp.apk");

            activityToast(activity,showMsg,"新版本("+config.serverVersion+") 准备下载");

            openNoticeBarProgress(activity,"更新应用");
            LLog.print("下载APK URL: "+ config.apkLink);
            //下载apk
            apk = FileServerClient.downloadFile(config.apkLink, apk.getPath(), new HttpUtils.CallbackAbs() {
                @Override
                public void onProgress(File file, long progress, long total) {
                    int current = (int)( (progress * 100f) / total );
                    updateNoticeBarProgress(current);
                    if (showMsg) updateProgressBar(activity,current);
                }
            });

            closeNoticeBarProgress();

            if (apk == null || !apk.exists()) {
                activityToast(activity,showMsg,"无法下载最新版本应用,请重新尝试");
                return;
            }

            openInstallPromptBox(activity,config,apk,config.apkLink);
        }catch (Exception e){
            LLog.error(e);
        }finally {
            isExecute = false;
        }
    }

    private static void openInstallPromptBox(final Activity activity, final AppUploadConfig config, final File _apk,final String apkUrl) {

        //打开安装对话框
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (config.forceUpdate == 0){
                    normalUpdate(activity,_apk,config.updateMessage,apkUrl);
                }else{
                    forceUpdate(activity,_apk,config.updateMessage,apkUrl);
                }
            }
        });
    }

    // 强制更新
    private static void forceUpdate(final Activity activity, final File file, String updateMessage, final String apkUrl) {
        DialogUtils.build(activity,
                "版本已过期",
                updateMessage,
                R.drawable.ic_update_version,
                "立即更新",
                "退出应用",
                null,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            //提示安装
                            boolean flag = AppUtils.installApk(activity, file, apkUrl);
                            if (flag){
                                System.exit(0);
                            }
                        } else{
                            System.exit(0);
                        }
                    }
                });
    }

    // 正常更新
    private static void normalUpdate(final Activity activity, final File file, String updateMessage, final String apkUrl) {
        DialogUtils.build(activity,
                "发现新版本",
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
                            AppUtils.installApk(activity, file,apkUrl);
                        }
                    }
                });
    }

    public static void checkVersion(final Activity activity, final boolean showMsg){
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                checkVersionAndDownload(activity,showMsg);
            }
        });
    }

}
