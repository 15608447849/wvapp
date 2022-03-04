package com.bottle.wvapp.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

import lee.bottle.lib.toolset.jsbridge.JSInterface;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.util.StringUtils;

public class ApplicationDevInfo {

    private ApplicationDevInfo(){ }

    /* 设备类型 */
    public static final String DEVTYPE = "PHONE";

    /* 应用对设备产生的UUID */
    private static final String APP_UUID_KEY = "APP_UUID";

    /* 设备标识 */
    private static String DEVID_KYD = "DEV_TOKEN";

    private static String DEVID = "unkown";

    public static void load(Application application){
        ApplicationAbs.setApplicationDir_OS_M(application,"1k.一块医药");
        loadDEVID(application);
        LLog.print("设备唯一标识 : " + ApplicationDevInfo.getMemDevToken());
    }

    private static void loadDEVID(Application application) {
        String uuid = getUUID_File(application);
        DEVID = StringUtils.strMD5(uuid);
        JSInterface.sharedStorage(application).edit().putString(DEVID_KYD,DEVID).apply();
    }

    private static String genUUID(Application application){
        return UUID.randomUUID().toString() +"@"+System.currentTimeMillis() +"@"+ Settings.System.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private static String getUUID_File(Application application) {
        String uuid = null;
        try {
            File dict = ApplicationAbs.getApplicationDIR("设备");
            if (dict == null) {
                throw new IllegalAccessException("没有配置应用文件存储目录");
            }
            File file = new File(dict,APP_UUID_KEY);
            if (file.exists()){
                try(FileInputStream in = new FileInputStream(file)){
                    byte[] bytes = new byte[1024];
                    int len;
                    StringBuilder sb = new StringBuilder();
                    while (( len= in.read(bytes))>0 ){
                        sb.append(new String(bytes,0,len));
                    }
                    if (sb.length()>0){
                        uuid = sb.toString();
                    }
                }
            }

            if (uuid == null){
                try(FileOutputStream out = new FileOutputStream(file)){
                    uuid = getUUID_SP(application);
                    out.write(uuid.getBytes());
                    out.flush();
                }
            }

        } catch (Exception e) {
            uuid = getUUID_SP(application);
        }
        return uuid;
    }

    private static String getUUID_SP(Application application) {
        SharedPreferences sp = JSInterface.sharedStorage(application);

        String uuid = sp.getString(APP_UUID_KEY,null);
        if (uuid == null){
            uuid = genUUID(application);
            sp.edit().putString(APP_UUID_KEY,uuid).apply();
        }
        return uuid;
    }

    public static String getMemDevToken() {
        return DEVID + "@" + DEVTYPE;
    }

    public static String getShareDevID(Context context) {
        return JSInterface.sharedStorage(context).getString(DEVID_KYD,null);
    }

    public static String getShareDevToken(Application app) {
        return getShareDevID(app) + "@" + DEVTYPE;
    }
}
