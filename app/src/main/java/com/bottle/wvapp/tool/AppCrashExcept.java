package com.bottle.wvapp.tool;

import android.app.Application;

import com.bottle.wvapp.jsprovider.HttpServerImp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.CrashHandler;
import lee.bottle.lib.toolset.threadpool.IOUtils;

/**
 * Created by Leeping on 2020/10/29.
 * email: 793065165@qq.com
 * 应用错误捕获
 */
public class AppCrashExcept implements CrashHandler.Callback {
    private final Application application;
    public AppCrashExcept(Application application) {
        this.application = application;
    }
    @Override
    public void devInfo(final Map<String, String> devInfoMap,final String mapStr) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                try{
                    File devInfo = new File(application.getCacheDir(),"dev.info");
                    if (!devInfo.exists()){
                        //写入文件
                        try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(devInfo), StandardCharsets.UTF_8)){
                            writer.write(mapStr);
                        }catch (Exception e){
                            e.printStackTrace();
                            return;
                        }
                    }

                    String remotePath = "/app/logs/"+devInfoMap.get("型号")+"/";
                    List<HttpServerImp.UploadFileItem> list = new ArrayList<>();
                    HttpServerImp.UploadFileItem item = new HttpServerImp.UploadFileItem();
                    item.remotePath = remotePath;
                    item.fileName = "dev.info";
                    item.uri = devInfo.getPath();
                    item.uploadSuccessDelete = true;
                    list.add(item);
                    //获取本地logs文件目录, 发送所有日志到服务器
                    File dir = new File(LLog.getBuild().getLogFolderPath());
                    File[] logFiles = dir.listFiles();
                    if (logFiles==null||logFiles.length==0) return;
                    for (File logFile : logFiles){
                        item = new HttpServerImp.UploadFileItem();
                        item.remotePath = remotePath;
                        item.fileName = logFile.getName();
                        item.uri = logFile.getAbsolutePath();
                        item.uploadSuccessDelete = true;
                        list.add(item);
                    }
                    HttpServerImp.UploadFileItem[] array = new HttpServerImp.UploadFileItem[list.size()];
                    list.toArray(array);
                    HttpServerImp.addUpdateFileToQueue(array);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void crash(final String crashFilePath,final Throwable ex) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                LLog.print("报错("+ ex.getMessage() +")日志文件: " + crashFilePath);
                HttpServerImp.UploadFileItem item = new HttpServerImp.UploadFileItem();
                item.remotePath = "/app/crash";
                item.fileName = crashFilePath.substring(crashFilePath.lastIndexOf("/")+1);
                item.uri = crashFilePath;
                item.uploadSuccessDelete = true;
                HttpServerImp.addUpdateFileToQueue(item);
            }
        });
    }
}
