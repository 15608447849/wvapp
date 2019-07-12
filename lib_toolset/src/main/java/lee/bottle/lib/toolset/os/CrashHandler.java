package lee.bottle.lib.toolset.os;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ref.SoftReference;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.ErrorUtil;
import lee.bottle.lib.toolset.util.TimeUtils;

import static android.Manifest.permission.READ_PHONE_STATE;

/**
 * Created by user on 2018/3/6.
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    //系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    //CrashHandler实例
    private static final CrashHandler INSTANCE = new CrashHandler();

    /** 获取CrashHandler实例 ,单例模式 */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    //程序的Context对象
    private SoftReference<Context> contextRef;

    //用来存储设备信息和异常信息
    private final Map<String, String> devInfoMap = new HashMap<>();

    /**
     * 初始化
     *
     * @param context
     */
    public void init(Context context) {
        if (context == null) throw new NullPointerException("app context is null.");
        contextRef = new SoftReference<>(context);
        try {
            //收集设备参数信息
            collectDeviceInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
        if (callback != null) callback.devInfo(devInfoMap, printDevInfo().toString());
    }

    @SuppressLint("HardwareIds")
    private void collectDeviceInfo() {
        Context mContext = contextRef.get();
        if (mContext == null) return;
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName + "";
                String versionCode = pi.versionCode + "";
                devInfoMap.put("应用信息", mContext.getPackageName() + "," + versionName + "," + versionCode);
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            devInfoMap.put("应用信息", "获取应用信息失败");
        }
        devInfoMap.put("系统生厂商", Build.BRAND);
        devInfoMap.put("硬件制造商", Build.MANUFACTURER);
        devInfoMap.put("型号", Build.MODEL);
        devInfoMap.put("cpu", Arrays.toString(Build.SUPPORTED_ABIS));
        devInfoMap.put("指纹", Build.FINGERPRINT);
        devInfoMap.put("序列号", Build.SERIAL);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
            int hasPermission = mContext.checkSelfPermission(READ_PHONE_STATE);
            //没有授权
             if (hasPermission == PackageManager.PERMISSION_GRANTED){
                 devInfoMap.put("序列号", Build.getSerial());
             };
        }
        devInfoMap.put("系统时间", TimeUtils.formatUTC(Build.TIME,null));
        devInfoMap.put("安卓系统版本号",Build.VERSION.RELEASE);
        devInfoMap.put("安卓SDK",Build.VERSION.SDK_INT+"");
        devInfoMap.put("分辨率",mContext.getResources().getDisplayMetrics().toString());
        if (callback != null) callback.devInfo(devInfoMap,printDevInfo().toString());
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {

        if (!handleException(e) && mDefaultHandler != null) {
            //如果应用程序没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(t, e);
        }
    }
    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) return false;
        Context mContext = contextRef.get();
        if (mContext == null) return false;
        StringBuffer sb = printDevInfo();
        sb.append(ErrorUtil.printExceptInfo(ex));

        try {
            //保存文件
            String progressName = AppUtils.getCurrentProcessName(mContext);
            String fileName = TimeUtils.formatUTC(new Date().getTime(),"yyyy_MM_dd_HH_mm_ss") + ".log";
            String filePath = mContext.getFilesDir()+File.separator+"crash"+File.separator+progressName;
            File dirFile = new File(filePath);
            if (!dirFile.exists()) dirFile.mkdirs();
            String fpStr = dirFile.getCanonicalFile()+File.separator+fileName;
            try(OutputStreamWriter out =
                        new OutputStreamWriter(new FileOutputStream(fpStr), StandardCharsets.UTF_8)){
                out.write(sb.toString());
            }
            //发送日志
            if (callback != null) callback.crash(fpStr,ex);
        } catch (IOException e) {
            Log.w("异常处理器","工作异常,原因:"+e);
            return false;
        }

        return true;
    }

    //写入设备信息
    private StringBuffer printDevInfo() {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : devInfoMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }
        return sb;
    }

    public interface Callback{
        void devInfo(Map<String,String> devInfoMap,String mapStr);
        void crash(String crashFilePath,Throwable ex);
    }

}
