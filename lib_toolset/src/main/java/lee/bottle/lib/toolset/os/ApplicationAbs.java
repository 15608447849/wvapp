package lee.bottle.lib.toolset.os;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.util.TimeUtils;


/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */
public abstract class ApplicationAbs extends Application implements Application.ActivityLifecycleCallbacks {

    private static HashMap<Class<?>,Object> applicationMap = new HashMap<>();

    public static void putApplicationObject(Object install){
        applicationMap.put(install.getClass(),install);
//        LLog.print(install.getClass() + " 放入全局应用: "+ install);

    }

    public static <Target> Target getApplicationObject(Class<? extends Target> classKey){
        Object install = applicationMap.get(classKey);
//        LLog.print(classKey + " 获取全局应用: "+ install);
        if (install == null) return null;
        return  (Target)install;
    }

    public static void delApplicationObject(Class<?> classKey){
        applicationMap.remove(classKey);
    }

    private static File applicationDir = null;

    public static void setApplicationDir(final File dir) {
        if (ApplicationAbs.applicationDir != null) return;
        try {
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalArgumentException("无法创建");
            File checkFile = new File(dir,"check");
            // 存在检测删除 , 不存在检测创建
            if (checkFile.exists() && !checkFile.delete() || !checkFile.exists() && !checkFile.createNewFile()){
                throw new IllegalArgumentException("没有文件写入权限");
            }
        } catch (Exception e) {
           LLog.print("设置应用目录("+dir+")失败,错误 "+ e);
           return;
        }
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                //删除时间大于7天的数据
                FileUtils.clearFiles(dir, 7 * 24 * 60 * 60 * 1000L);
            }
        });

        ApplicationAbs.applicationDir = dir;
        LLog.print("设置成功应用目录: "+  ApplicationAbs.applicationDir );
    }

    private static long startTime = System.currentTimeMillis();

    public static String runtimeStr(){
        return TimeUtils.formatDuring(System.currentTimeMillis() - startTime);
    }

    public static File getApplicationDIR(String subDic){
        try {
            if (applicationDir == null){
               throw new IllegalArgumentException("未设置应用目录");
            }
            File rootDir = null;
            if (subDic==null){
                rootDir = applicationDir;
            }else{
                rootDir = new File(applicationDir,subDic);
            }

            if (!rootDir.exists()){
                if (!rootDir.mkdirs()){
                    throw new IllegalArgumentException("无法创建文件夹: "+ rootDir);
                }
            }
            return rootDir;
        } catch (Exception e) {
            //LLog.print("获取应用目录失败,错误 "+ e);
        }
        return null;
    }

    /** 是否注册activity声明周期的回调管理 */
    private boolean isRegisterActivityLifecycleCallbacks = true;

    protected void setRegisterActivityLifecycleCallbacks(boolean flag) {
        this.isRegisterActivityLifecycleCallbacks =  flag;
    }

    private boolean isPrintLifeLog = false;

    public void setPrintLifeLog(boolean flag) {
        isPrintLifeLog = flag;
    }

    public void setCrashCallback(CrashHandler.Callback callback){
        CrashHandler.getInstance().setCallback(callback);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(getApplicationContext());
        String progressName = AppUtils.getCurrentProcessName(getApplicationContext());
        onCreateByAllProgress(progressName);
        if ( isRegisterActivityLifecycleCallbacks ) registerActivityLifecycleCallbacks(this);//注册 activity 生命周期管理
        if (AppUtils.checkCurrentIsMainProgress(getApplicationContext(),progressName)){
            onCreateByApplicationMainProgress(progressName);
        }else{
            onCreateByApplicationOtherProgress(progressName);
        }
    }

    /**
     * 所有进程需要的初始化操作
     */
    protected void onCreateByAllProgress(String processName) {
                //日志参数
                LLog.getBuild()
                .setContext(getApplicationContext())
                .setLevel(Log.ASSERT)
                .setDateFormat(TimeUtils.getSimpleDateFormat("[MM/dd HH:mm]"))
                .setLogFileName(processName+"_"+ TimeUtils.formatUTCByCurrent("MMdd"))
                .setWriteFile(true);
                //存储应用进程号
                storeProcessPidToFile(processName,android.os.Process.myPid());
    }

    private void storeProcessPidToFile(String processName, int pid) {
        try {
            File dirs = new File(getCacheDir().getPath()+"/pids");
            if (!dirs.exists()) dirs.mkdirs();
            File file  = new File(dirs,processName);
            if (!file.exists()) file.createNewFile();
            FileWriter writer = new FileWriter(file);
            writer.write(pid+"\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void killAllProcess(boolean containSelf){
        try {
            File dirs = new File(getCacheDir().getPath()+"/pids");
            if (dirs.exists()) {

                for (File file : dirs.listFiles()){
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String sPid = reader.readLine();
                    reader.close();
                    file.delete();
                    int pid = Integer.parseInt(sPid);
                    if (pid == android.os.Process.myPid()) continue;
                    android.os.Process.killProcess(pid);

                }
            }
           if (containSelf) android.os.Process.killProcess(android.os.Process.myPid());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 主包名进程 初始化创建
     */
    protected void onCreateByApplicationMainProgress(String processName){

    }

    /**
     * 其他包名进程 初始化创建
     */
    protected void onCreateByApplicationOtherProgress(String processName){

    }


    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onCreated");
        //竖屏锁定
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //横屏锁定
//      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //没有title
        //activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //硬件加速
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        //应用运行时，保持屏幕高亮,不锁屏
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //设定软键盘的输入法模式 覆盖在图层上 不会改变布局
//        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onStarted");
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onResumed");
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onPaused");
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onSaveInstanceState");
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onStopped");
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onDestroyed");
    }
}
