package lee.bottle.lib.toolset.os;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.util.TimeUtils;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;


/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */
public abstract class ApplicationAbs extends Application implements Application.ActivityLifecycleCallbacks {


    // 进程PID存储文件
    private static final String PID_FILE_NAME = "pid_recode_catalog";
    // 应用首次启动时间
    private static final long startTime = System.currentTimeMillis();
    // 应用全局对象
    private final static HashMap<Class<?>,Object> applicationMap = new HashMap<>();
    // activity启动的列表
    private static final Map<String,List<Activity>> activityMaps = new HashMap<>();
    // 应用数据存放目录
    private static File applicationDir;
    /** 是否注册activity声明周期的回调管理 */
    private boolean isRegisterActivityLifecycleCallbacks = true;
    // 是否打印activity生命周期
    private boolean isPrintLifeLog = false;

    /* 获取应用运行时长 */
    public static String runtimeStr(){
        return TimeUtils.formatDuring(System.currentTimeMillis() - startTime);
    }

    /* 设置应用全局对象 */
    public static void putApplicationObject(Object install){
        applicationMap.put(install.getClass(),install);
//        LLog.print(install.getClass() + " [加入] 全局对象: "+ install);
    }

    /* 设置应用全局对象 */
    public static void putApplicationObject(Class<?> classKey,Object install){
        try {
            install.getClass().asSubclass(classKey);
            applicationMap.put(classKey,install);
//            LLog.print( classKey + " [加入] 全局对象: "+ install);
        } catch (Exception e) {
            LLog.print(install.getClass() + " [加入] 全局对象失败: "+ e);
        }
    }

    /* 获取应用全局对象 */
    public static <Target> Target getApplicationObject(Class<? extends Target> classKey){
        Object install = applicationMap.get(classKey);
//        LLog.print(classKey + " [获取] 全局对象 "+ install);
        if (install == null) return null;
        return  (Target)install;
    }

    /* 删除应用全局对象*/
    public static void delApplicationObject(Class<?> classKey){
        Object install = applicationMap.remove(classKey);
//        LLog.print( classKey + " [移除] 全局对象 "+ install);
    }


    /* 设置应用文件存储目录 */
    public static void setApplicationDataDict(Context context, String dictName) {



        File dict = context.getFilesDir(); // 应用私有目录;

       if (!FileUtils.checkDictPermission(dict)){
           dict = context.getCacheDir();// 应用缓存目录
           if (!FileUtils.checkDictPermission(dict)){
               if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                   dict = context.getDataDir();
                   if (!FileUtils.checkDictPermission(dict)){
                       throw new RuntimeException("应用文件读写权限被拒绝");
                   }
               }else {
                   throw new RuntimeException("应用文件读写权限被拒绝");
               }
           }
       }

        if (dictName!=null && dictName.length()>0){
            dict = new File(dict, dictName);
            if ( !dict.exists() ){
                if (  !dict.mkdir() ) throw new RuntimeException("应用创建目录失败 : "+ dict);
            }
        }

        ApplicationAbs.applicationDir = dict;
        LLog.print("应用存储目录 : "+ ApplicationAbs.applicationDir);
    }


    public static File getApplicationDIRRoot(){
        return getApplicationDIR(null,false,null);
    }

    public static File getApplicationDIR(String subDic){
        return getApplicationDIR(subDic,false,null);
    }

    /* 获取应用文件存储目录 */
    public static File getApplicationDIR(String subDic,boolean isPriorityExternal,Context context){
        try {
            if (applicationDir == null){
               throw new IllegalArgumentException("未设置应用目录");
            }

            File dict = applicationDir;

            // 尝试获取外置卡 如果外置卡允许,则使用外置卡
            if (isPriorityExternal && context!=null){
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || AppUtils.checkPermissionExist(context,WRITE_EXTERNAL_STORAGE)){
                    File sdDict = new File(Environment.getExternalStorageDirectory(),applicationDir.getName());
                    if(FileUtils.checkDictPermission(sdDict)){
                        dict = sdDict;
                    }
                }
            }

            File rootDir;
            if (subDic==null){
                rootDir = dict;
            }else{
                rootDir = new File(dict,subDic);
            }

            if (!rootDir.exists()){
                if (!rootDir.mkdirs()){
                    throw new IllegalArgumentException("无法创建文件夹: "+ rootDir);
                }
            }

            //LLog.print(" 获取文件目录 : "+ rootDir);

            return rootDir;
        } catch (Exception e) {
            LLog.print("获取应用目录失败: "+ e);
        }
        return null;
    }

    private synchronized static void addActivityToMap(Activity activity){
        String classPath = activity.getClass().getName();
        List<Activity> list = activityMaps.get(classPath);
        if (list == null){
            list = new ArrayList<>();
            activityMaps.put(classPath,list);
        }
        list.add(activity);
//        LLog.print("[添加Activity]  " + activity +" 当前数量: " + list.size());
    }

    private synchronized static void removeActivityToMap(Activity activity){
        String classPath = activity.getClass().getName();
        List<Activity> list = activityMaps.get(classPath);
        if (list!=null){
            Iterator<Activity> iterator = list.iterator();
            while (iterator.hasNext()){
                Activity next = iterator.next();
                if (next.equals(activity)){
                    iterator.remove();
//                    LLog.print("[移除Activity]  " + activity);
                }
            }
        }
    }

    protected void setRegisterActivityLifecycleCallbacks(boolean flag) {
        this.isRegisterActivityLifecycleCallbacks =  flag;
    }

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
        try {
            LLog.getBuild()
            .setContext(getApplicationContext())
            .setLevel(Log.ASSERT)
            .setDateFormat(TimeUtils.getSimpleDateFormat("[MM/dd HH:mm]"))
            .setLogFileName(processName+"_"+ TimeUtils.formatUTCByCurrent("MMdd"))
            .setLogFolderPath(getLogFolderDir())
            .setWriteFile(true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 存储应用进程号
                storeProcessPidToFile(this,processName,android.os.Process.myPid());
    }

    protected String getLogFolderDir(){
        Log.w("设置日志文件目录", "getLogFolderDir: "+ this.getFilesDir().getAbsolutePath());
        return this.getFilesDir().getAbsolutePath();
    }

    /* 记录所有进程的PID */
    private static void storeProcessPidToFile(Context context,String processName, int pid) {
        try {
            File dirs = new File(context.getCacheDir(),PID_FILE_NAME);

            if (!dirs.exists()) {
                if (!dirs.mkdirs()){
                    return;
                }
            }

            File file  = new File(dirs,processName);
            if (!file.exists()) {
                if (!file.createNewFile()){
                    return;
                }
            }

            try(FileWriter writer = new FileWriter(file)){
                writer.write(pid + "\n");
                writer.flush();
            }


        } catch (Exception e) {
            LLog.print("记录进程PID失败: "+ e);
        }
    }

    /* 杀死当前存在的所有进程 , true-包括自己 */
    public static void killAllProcess(Context context,boolean containSelf){
        try {

            File dirs = new File(context.getCacheDir(),PID_FILE_NAME);

            if (dirs.exists()) {
                for (File file : dirs.listFiles()){
                    int pid = 0;
                    try(BufferedReader reader = new BufferedReader(new FileReader(file))){
                        pid = Integer.parseInt(reader.readLine());
                    }catch (IOException e){
                      LLog.print("根据PID文件杀死进程,读取文件失败: "+ e);
                    }

                     if (!file.delete()){
                         LLog.print("根据PID文件杀死进程,删除文件失败,文件路径: "+ file);
                     }

                    if (pid==0 || pid == android.os.Process.myPid()) continue;
                    android.os.Process.killProcess(pid);
                }
            }

           if (containSelf) android.os.Process.killProcess(android.os.Process.myPid());

        } catch (Exception e) {
            LLog.print("根据PID文件杀死进程失败: "+ e);
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
        addActivityToMap(activity);

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
        removeActivityToMap(activity);
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onDestroyed");
    }

}
