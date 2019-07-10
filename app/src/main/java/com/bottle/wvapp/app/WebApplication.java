package com.bottle.wvapp.app;

import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
    protected void onCreateByAllProgress(String processName) {
        super.onCreateByAllProgress(processName);
        NativeServerImp.bindApplication(this);
        setCrashCallback(this);
    }

    @Override
    public void devInfo(final Map<String, String> devInfoMap,final String mapStr) {
       IOUtils.run(new Runnable() {
           @Override
           public void run() {
               LLog.print(devInfoMap);
               File temp = new File(getApplicationContext().getCacheDir(),"dev.info");
//               if (temp.exists()) return;
               //写入文件
               try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(temp), StandardCharsets.UTF_8)){
                   writer.write(mapStr);
               }catch (Exception e){
                   e.printStackTrace();
                   return;
               }
               HttpServerImp.JSFileItem item = new HttpServerImp.JSFileItem();
               item.remotePath = "/app/设备信息收集";
               item.fileName = devInfoMap.get("型号") +"-"+System.currentTimeMillis()+".dev";
               item.uri = temp.getAbsolutePath();
               HttpServerImp.updateFile(getApplicationContext(),item);
           }
       });
    }

    @Override
    public void crash(final String crashFilePath, Throwable ex) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                HttpServerImp.JSFileItem item = new HttpServerImp.JSFileItem();
                item.remotePath = "/app/crash";
                item.fileName = crashFilePath.substring(crashFilePath.lastIndexOf("/")+1);
                item.uri = crashFilePath;
                HttpServerImp.updateFile(getApplicationContext(),item);
                new File(crashFilePath).delete();
            }
        });
    }
}
