package com.bottle.wvapp.beans;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.webkit.DownloadListener;

import com.bottle.wvapp.R;
import com.bottle.wvapp.activitys.NativeActivity;

import java.io.File;

import lee.bottle.lib.toolset.http.FileServerClient;
import lee.bottle.lib.toolset.http.HttpUtils;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtils;
import lee.bottle.lib.toolset.util.FileUtils;

public class UrlDownloadDialog implements DownloadListener {
    private Activity activity;

    public UrlDownloadDialog(Activity activity) {
        this.activity = activity;
    }



    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {
        File rootDir = ApplicationAbs.getApplicationDIR("我的下载",true,activity);
        if (rootDir == null) return;
        final String fileName = url.substring(url.lastIndexOf("/")+1);
        final File file = new File(rootDir,fileName);

        String msg = "准备下载 "+ fileName+" ("+ FileUtils.byteLength2StringShow(contentLength)+ ")  保存位置:\t" +
                rootDir.getAbsolutePath().replace("/storage/emulated/0","文件管理/手机存储");

        //下载弹窗
        DialogUtils.build(activity,
                "是否立即下载",
                msg,
                R.drawable.ic_update_version,
                "直接打开(推荐)",
                "立即下载",
                "取消",
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            openDirectly(url);
                        }
                        if (which == DialogInterface.BUTTON_NEGATIVE){
                           downloadNow(url,file);
                        }
                        if (which == DialogInterface.BUTTON_NEUTRAL){
                            cancelAction(url);
                        }
                    }
                });

    }


    // 直接打开按钮
    protected void openDirectly(String url) {
        //其他应用打开
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("https://view.xdocin.com/view?src="+url));
//                            intent.setData(Uri.parse(url));
        activity.startActivity(intent);
    }

    protected void downloadNow(String url,File file) {
        //加入队列下载
        FileServerClient.addDownloadFileToQueue(new FileServerClient.DownloadTask(url,file.getPath(),new HttpUtils.CallbackAbs(){

            @Override
            public void onResult(HttpUtils.Response response) {
                final File storeFile = response.getData();
                final String resMsg = storeFile + " 下载完成";
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppUtils.toastLong(activity,resMsg);
                    }
                });

            }
        }));
    }

    protected void cancelAction(String url) {

    }

}
