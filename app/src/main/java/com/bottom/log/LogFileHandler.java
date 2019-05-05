package com.bottom.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 * Created by Leeping on 2018/8/20.
 * email: 793065165@qq.com
 */

public class LogFileHandler {


    void clear(Build build){
        try {
            File folder = new File(build.logFolderPath);
            if (!folder.exists()) return;
            File files[] = folder.listFiles();
            ArrayList<File> delFile = new ArrayList<>();
            long time;
            for (File file : files){
                time = System.currentTimeMillis() - file.lastModified(); //当前时间 - 最后修改时间
                if (time > build.storageDays * 24 * 60 * 60 * 1000L){
                    delFile.add(file);
                }
            }
            //删除文件
            for (File file : delFile){
                boolean flag =  file.delete();
                LLog.print("删除日志文件: " + file.getName() +"  - "+ flag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void handle(Build build,String msg) throws Exception{
        if (!build.isWriteFile) return;

        File folder = new File(build.logFolderPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = getLogFile(folder,build.logFileName,build.logFileSizeLimit,0);
        if (!file.exists()) {
            boolean cSuccess = file.createNewFile();
            if (!cSuccess) throw new FileNotFoundException(file.toString());
        }
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter( new FileOutputStream(file,true), "UTF-8");
            out.write(msg+"\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(out!=null){
                try {out.close();} catch (IOException ignored) {}
            }
        }

    }

    private File getLogFile(File folder, String fileName, int limit,int index) {
        File newFile = new File(folder, String.format("%s_%s.log", fileName, index));
        if (newFile.exists()) {
           //如果文件存在 - 判断文件大小
           if (newFile.length() >= limit){
               index++;
               return  getLogFile(folder,fileName,limit,index);
           }
        }
        return newFile;
    }
}
