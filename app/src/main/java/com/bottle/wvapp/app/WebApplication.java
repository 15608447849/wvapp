package com.bottle.wvapp.app;

import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.CrashHandler;
import lee.bottle.lib.toolset.threadpool.IOUtils;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public class WebApplication extends ApplicationAbs implements CrashHandler.Callback {

    @Override
    protected void onCreateByApplicationMainProgress(String processName) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                NativeServerImp.bindApplication(WebApplication.this);
            }
        });
    }

    @Override
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        setCrashCallback(this);
    }

    @Override
    public void devInfo(final Map<String, String> devInfoMap,final String mapStr) {
       IOUtils.run(new Runnable() {
           @Override
           public void run() {
               try{
                   File devInfo = new File(getApplicationContext().getCacheDir(),"dev.info");
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
                   List<HttpServerImp.JSFileItem> list = new ArrayList<>();
                   HttpServerImp.JSFileItem item = new HttpServerImp.JSFileItem();
                   item.remotePath = remotePath;
                   item.fileName = "dev.info";
                   item.uri = devInfo.getAbsolutePath();
                   list.add(item);
                   //获取本地logs文件目录, 发送所有日志到服务器
                   File dir = new File(LLog.getBuild().getLogFolderPath());
                   File[] logFiles = dir.listFiles();
                   if (logFiles==null||logFiles.length==0) return;
                   for (File logFile : logFiles){
                       item = new HttpServerImp.JSFileItem();
                       item.remotePath = remotePath;
                       item.fileName = logFile.getName();
                       item.uri = logFile.getAbsolutePath();
                       list.add(item);
                   }
                   HttpServerImp.JSFileItem[] array = new HttpServerImp.JSFileItem[list.size()];
                   list.toArray(array);
                   HttpServerImp.updateFile(array);
               }catch (Exception e){
                   e.printStackTrace();
               }
           }
       });
    }

    @Override
    public void crash(final String crashFilePath, Throwable ex) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                LLog.print("报错日志文件: " + crashFilePath);
                HttpServerImp.JSFileItem item = new HttpServerImp.JSFileItem();
                item.remotePath = "/app/crash";
                item.fileName = crashFilePath.substring(crashFilePath.lastIndexOf("/")+1);
                item.uri = crashFilePath;
                HttpServerImp.updateFile(item);
                //new File(crashFilePath).delete();
            }
        });
    }
}
