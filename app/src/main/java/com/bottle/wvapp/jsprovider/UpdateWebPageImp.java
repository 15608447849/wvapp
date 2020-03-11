package com.bottle.wvapp.jsprovider;

import android.os.Build;

import com.bottle.wvapp.tool.NotifyUer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.FrontNotification;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;

/**
 * Created by Leeping on 2019/7/16.
 * email: 793065165@qq.com
 */
class UpdateWebPageImp {
    //解压缩html文件到缓存目录
    static void transferWebPageToDir(boolean isAuto) {
        if (NativeServerImp.config == null) return;

        //判断是否已存在
        int recode = NativeServerImp.sp.getInt("webPageVersion",0);

        if (!checkAppVersionMatch(NativeServerImp.config.serverVersion,recode)) return;

        int webPageVersion = NativeServerImp.config.webPageVersion;
        //判断是否使用网络url
        if (webPageVersion <0 ) return;

        LLog.print("当前服务器web page 版本: " + webPageVersion+" , 当前记录的web page 版本: "+ recode);
        if (webPageVersion == 0 || recode == 0){
            unzipWebPageByAssets(); //从assets解压缩
        }
        if (recode<webPageVersion){
            //执行更新页面操作
            updateWebPage(isAuto);
        }
    }

    private static boolean checkAppVersionMatch(int remote,int recode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)  return true;

        //如果当前app版本与服务器版本不一致 ,检查是否存在已记录得版本
        if (AppUtils.getVersionCode(NativeServerImp.app)<remote){

            if (recode == 0){
                unzipWebPageByAssets();
            }else {
                //如果存在,检查本地文件是否存在, 存在不做任何操作,不存在解压缩
                File file = new File(NativeServerImp.app.getFilesDir()+"/dist/index,html");
                if (!file.exists()){
                    unzipWebPageByAssets();
                }
            }
            return false;
        }
        return true;
    }

    //从assets中解压缩 初始化安装或版本更新时
    private static void unzipWebPageByAssets() {
        //直接解压缩access中的文件到缓存目录
        try(InputStream in = NativeServerImp.app.getAssets().open("dist.zip");){
            //解压缩
            boolean flag = AppUtils.unZipToFolder(in,NativeServerImp.app.getFilesDir());
            if (!flag) throw new IOException("无法解压缩页面资源");
        }catch (Exception e){
            LLog.print("从asset文件中解压zip时错误,"+e);
        }
       NativeServerImp.sp.edit().remove("webPageVersion").apply();
    }

    //正在更新标识
    private static boolean isUpdateWebPageIng = false;

    //打开进度条
    private static void openProgress() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if (notification == null){
                notification = NotifyUer.createDownloadApkNotify(NativeServerImp.app.getApplicationContext(),"页面更新");
                notification.setProgress(100,0);
            }
        }
    }

    private static void closeProgress() {
        if (notification!=null){
            notification.cancelNotification();
            notification = null;
        }
    }
    private static FrontNotification notification;
    //从服务器更新
    private static void updateWebPage(final boolean isAuto) {
        if (isUpdateWebPageIng) return;
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                isUpdateWebPageIng = true;
                if (!isAuto) NativeServerImp.updateServerConfigJson();
                if (NativeServerImp.config == null) return;
                NativeServerImp.ServerConfig config = NativeServerImp.config;
                openProgress();
                //下载最新zip
                File file = HttpServerImp.downloadFile(config.zipLink, NativeServerImp.app.getCacheDir()+"/dist.zip",
                        new HttpUtil.CallbackAbs(){
                    @Override
                    public void onProgress(File file, long progress, long total) {
                        //打开进度指示条的通知栏
                        int current = (int)( (progress * 100f) / total );
                        if (notification!=null) notification.setProgress(100, current);
                    }
                });
                closeProgress();
                if (file != null) {
                    try(InputStream in = new FileInputStream(file)){
                        //解压缩
                        boolean flag = AppUtils.unZipToFolder(in,NativeServerImp.app.getFilesDir());
                        if (flag){
                            NativeServerImp.sp.edit().putInt("webPageVersion",config.webPageVersion).apply();
                            //通知页面更新
                            NativeServerImp.reopenWeb();
                        }else{
                            throw new IOException("web页面升级失败,无法解压缩页面资源");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        unzipWebPageByAssets();
                    }
                }
                isUpdateWebPageIng = false;
            }
        });
    }

}
