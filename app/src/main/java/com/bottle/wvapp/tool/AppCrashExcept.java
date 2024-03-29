package com.bottle.wvapp.tool;

import android.app.Application;

import lee.bottle.lib.toolset.os.ApplicationDevInfo;

import com.bottle.wvapp.app.WebApplication;
import com.bottle.wvapp.beans.BusinessData;

import lee.bottle.lib.toolset.http.FileServerClient;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.CrashHandler;
import lee.bottle.lib.toolset.threadpool.IOUtils;

/**
 * Created by Leeping on 2020/10/29.
 * email: 793065165@qq.com
 * 应用错误捕获
 */
public class AppCrashExcept extends Thread implements CrashHandler.Callback {
    // 异常文件列表
    private final BlockingQueue<File> crashFileQueue = new LinkedBlockingQueue<>();

    public AppCrashExcept(Application application) {
        initErgodic(application);
        setDaemon(true);
        start();
    }

    private void initErgodic(Application application) {
        //遍历错误文件,加入队列
        File crashDict = CrashHandler.getCrashDict(application);
        File[] crashFileArray = crashDict.listFiles();
        if (crashFileArray!=null){
            for (File crashFile : crashFileArray){
                crashFileQueue.offer(crashFile);
            }
        }
    }

    @Override
    public void devInfo(final Map<String, String> devInfoMap,final String mapStr) {

    }

    @Override
    public void crash(final File crashFile,final Throwable ex) {
        LLog.error(ex);
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                StringBuilder s = new StringBuilder();
                s.append("token").append("=").append(ApplicationDevInfo.getMemoryDEVID()+ "@" + WebApplication.DEVTYPE).append("\n");
                s.append("companyID").append("=").append(BusinessData.getCurrentDevCompanyID(false,null)).append("\n");
                try (FileOutputStream fos = new FileOutputStream(crashFile,true)){
                    fos.write(s.toString().getBytes());
                    fos.flush();
                }catch (Exception ignored){ }
                // 加入异常文件列表
                crashFileQueue.offer(crashFile);
            }
        });
    }


    @Override
    public void run() {

        // 定时检测异常捕获文件,上传
        while (true){
            try{
                File crashFile = crashFileQueue.take();
                //LLog.print("上传错误文件: "+ crashFile);
                FileServerClient.UploadFileItem item = new FileServerClient.UploadFileItem();
                item.uri = crashFile.getCanonicalPath();
                item.remotePath = "/app/crash";
                item.fileName = crashFile.getName();
                item.uploadSuccessDelete = true;
                FileServerClient.addUpdateFileToQueue(item);
            }catch (Exception e){
                LLog.error(e);
            }
        }
    }

}
