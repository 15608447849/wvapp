package lee.bottle.lib.toolset.os;

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

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.TimeUtils;


/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */
public abstract class ApplicationAbs extends Application implements Application.ActivityLifecycleCallbacks {

    /** 是否注册activity声明周期的回调管理 */
    private boolean isRegisterActivityLifecycleCallbacks = true;

    protected void setRegisterActivityLifecycleCallbacks(boolean flag) {
        this.isRegisterActivityLifecycleCallbacks =  flag;
    }

    private boolean isPrintLifeLog = false;

    public void setPrintLifeLog(boolean flag) {
        isPrintLifeLog = flag;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (isPrintLifeLog) LLog.format("---%s :: %s",activity,"onCreated");
        //竖屏锁定
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //应用运行时，保持屏幕高亮,不锁屏
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //设定软键盘的输入法模式 覆盖在图层上 不会改变布局
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
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
