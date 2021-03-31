package com.bottle.wvapp.tool;

import android.app.Application;

import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    private final Application application;

    public AppCrashExcept(Application application) {
        this.application = application;
        setDaemon(true);
        start();
    }

    @Override
    public void devInfo(final Map<String, String> devInfoMap,final String mapStr) {
        NativeServerImp.bindApplication(application, devInfoMap);
        //遍历错误文件,加入队列
        File crashDict = CrashHandler.getCrashDict(application);
        File[] crashFileArray = crashDict.listFiles();
        for (File crashFile : crashFileArray){
            crashFileQueue.offer(crashFile);
        }
    }

    @Override
    public void crash(final File crashFile,final Throwable ex) {
        ex.printStackTrace();
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                StringBuilder s = new StringBuilder();
                s.append("token").append("=").append(NativeServerImp.getDevToken()).append("\n");
                s.append("companyID").append("=").append(NativeServerImp.refreshCompanyInfoAndOutput(true)).append("\n");
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
                LLog.print("上传错误文件: "+ crashFile);

                HttpServerImp.UploadFileItem item = new HttpServerImp.UploadFileItem();
                item.uri = crashFile.getCanonicalPath();
                item.remotePath = "/app/crash";
                item.fileName = crashFile.getName();
                item.uploadSuccessDelete = true;

                HttpServerImp.addUpdateFileToQueue(item);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

}
