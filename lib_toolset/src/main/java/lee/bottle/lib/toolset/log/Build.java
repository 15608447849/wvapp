package lee.bottle.lib.toolset.log;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Leeping on 2018/8/20.
 * email: 793065165@qq.com
 * 日志
 */
public class Build {
    Context context;
    boolean isWriteConsole = true;//输出控制台
    boolean isWriteFile = false;//写入文件
    boolean isWriteThreadInfo = false; //打印线程信息
    int logFileSizeLimit = 100 * 1024 * 1024; //1M
    SimpleDateFormat dateFormat = new SimpleDateFormat("[yyy-MM-dd HH:mm:ss]", Locale.CHINA);
    String logFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separatorChar + "AppLogger";
    String logFileName = "console";
    String tag = "logger";
    int level = Log.VERBOSE;
    int methodLineCount = 0;
    long threadTime = 30 * 1000;
    int storageDays = 7; //默认一个星期
    Build(){}
    public Build setContext(Context context) {
        this.context = context;
        return this;
    }
    private void checkFileName() {
        Pattern pattern = Pattern.compile("[\\s\\\\/:\\*\\?\\\"<>\\|]");
        Matcher matcher = pattern.matcher(this.logFileName);
        this.logFileName = matcher.replaceAll("_");
    }
    public Build setWriteConsole(boolean writeConsole) {
        isWriteConsole = writeConsole;
        return this;
    }
    public Build setWriteFile(boolean writeFile) {
        isWriteFile = writeFile;
        return this;
    }
    public Build setLogFileSizeLimit(int logFileSizeLimit) {
        this.logFileSizeLimit = logFileSizeLimit;
        return this;
    }
    public Build setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }
    public Build setLogFolderPath(String logFolderPath) {
        if(logFolderPath!=null) {
            this.logFolderPath = logFolderPath + File.separatorChar + "AppLogger";
        }
        return this;
    }
    public Build setLogFileName(String logFileName) {
        this.logFileName = logFileName;
        //合法性效验
        checkFileName();
        return this;
    }
    public Build setTag(String tag) {
        this.tag = tag;
        return this;
    }
    public Build setLevel(int level) {
        this.level = level;
        return this;
    }
    public Build setMethodLineCount(int methodLineCount) {
        if (methodLineCount>4) methodLineCount = 4;
        if (methodLineCount<0) methodLineCount=0;
        this.methodLineCount = methodLineCount;
        return this;
    }
    public Build setThreadTime(long threadTime) {
        this.threadTime = threadTime;
        return this;
    }
    public Build setWriteThreadInfo(boolean writeThreadInfo) {
        isWriteThreadInfo = writeThreadInfo;
        return this;
    }
    public Build setStorageDays(int storageDays) {
        this.storageDays = storageDays;
        return this;
    }
    public String getLogFolderPath(){
        return this.logFolderPath ;
    }

}
