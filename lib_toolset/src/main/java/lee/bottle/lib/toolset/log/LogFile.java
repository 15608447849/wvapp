package lee.bottle.lib.toolset.log;

import android.Manifest;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Leeping on 2018/8/20.
 * email: 793065165@qq.com
 * 文件输出实现
 */
class LogFile implements ILogHandler{

    void clear(Build build){
        try {
            File folder = new File(build.logFolderPath);
            if (!folder.exists()) return;
            File[] files = folder.listFiles();
            ArrayList<File> delFile = new ArrayList<>();
            if(files==null){
                return;
            }
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
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    @Override
    public void handle(String tag,Build build,String content) throws Exception{

        //判断是否存在文件写入权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            if (build.context==null) throw new NullPointerException(" build context is null");
            int hasPermission = build.context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            //没有授权写权限
            if (hasPermission == PackageManager.PERMISSION_DENIED) {
                throw  new Exception("android 6.0 文件权限写入拒绝");
            }
        }

        if (!build.isWriteFile) return;

        File folder = new File(build.logFolderPath);

        if (!folder.exists()) {
            folder.mkdirs(); //创建目录
        }

        File file = getLogFile(folder,build.logFileName,build.logFileSizeLimit,0);

        if (!file.exists()) {
            boolean cSuccess = file.createNewFile();
            if (!cSuccess) throw new FileNotFoundException(file.toString());
        }

        //写入
        try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            String head = build.dateFormat.format(new Date());
            out.write(head + "\t" + content + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
