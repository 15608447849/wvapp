package lee.bottle.lib.toolset.os;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;


import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.util.StringUtils;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/*
* 应用设备信息
* */
public class ApplicationDevInfo {

    private ApplicationDevInfo(){ }

    private static final String DATA_STORAGE_FLAG = "SP_ON_SHARED_STORE_DATA";

    private static String DICT_NAME = "DEV_INFO";
    private static final String DICT_SUB_NAME = "设备";

    /* 应用对设备产生的UUID */
    private static final String APP_UUID_KEY = "APP_UUID";

    /* 设备标识 */
    private static String DEVID_KYD = "DEV_TOKEN";

    private static String DEVID = "UNKOWN";

    public static SharedPreferences sharedStorage(Context context){
        return context.getSharedPreferences(DATA_STORAGE_FLAG,Context.MODE_MULTI_PROCESS);
    }

    public static void init(Application application,String rootDictName){
        if (rootDictName!=null) DICT_NAME = rootDictName;
        ApplicationAbs.setApplicationDataDict(application,DICT_NAME);
        loadDEVID(application);// 加载设备ID
    }

    /* 加载设备唯一ID */
    private static void loadDEVID(Application application) {
        String uuid = getUUID_File(application);
        DEVID = StringUtils.strMD5(uuid);
        sharedStorage(application).edit().putString(DEVID_KYD,DEVID).apply();
        persistentStorageDEVID(application);
        LLog.print("设备ID : " + ApplicationDevInfo.getMemoryDEVID());
    }

    /*
    * 20220928 兼容以前版本的文件存放方式
    * */
    public static void transferDictCompatible(Application application){

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                        || AppUtils.checkPermissionExist(application,WRITE_EXTERNAL_STORAGE)){
            // 外置卡有权限且存在
           File old_dict = new File(Environment.getExternalStorageDirectory(),DICT_NAME+"/"+DICT_SUB_NAME);
           if (old_dict.exists()){
               LLog.print("尝试加载SD卡设备ID文件...");
               File cur_dict = ApplicationAbs.getApplicationDIRRoot();
               if (cur_dict!=null){
                   // 文件循环转移
                   FileUtils.copyFileDict(old_dict,cur_dict);
                   loadDEVID(application);// 加载设备ID
               }

           }
        }
    }
    /*
     * 20220928 兼容以前版本的文件存放方式
     * */
    private static void persistentStorageDEVID(Application application) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                    || AppUtils.checkPermissionExist(application,WRITE_EXTERNAL_STORAGE)){
                // 外置卡有权限且设备ID不存在
                File sd_dict = new File(Environment.getExternalStorageDirectory(),DICT_NAME+"/"+DICT_SUB_NAME);
                if (!sd_dict.exists()){
                    File cur_dict = ApplicationAbs.getApplicationDIRRoot();
                    if (cur_dict!=null){
                        // 文件循环转移
                        FileUtils.copyFileDict(cur_dict,sd_dict);
                    }

                }
            }
        } catch (Exception ignored) {

        }
    }

    private static String genUUID(Application application){
        // 这是在设备首次启动时生成并存储的64位数量
        String androidID = Settings.System.getString(application.getContentResolver(), Settings.Secure.ANDROID_ID);
        LLog.print("ANDROID ID = " +androidID);
        return UUID.randomUUID().toString() +"@"+System.currentTimeMillis() +"@"+ androidID;
    }

    private static String getUUID_File(Application application) {
        String uuid = null;
        try {
            File dict = ApplicationAbs.getApplicationDIR(DICT_SUB_NAME);
            if (dict == null) {
                throw new IllegalAccessException("没有配置应用文件存储目录");
            }

            // 从外部文件中读取 ID值
            File file = new File(dict,APP_UUID_KEY);
            if (file.exists()){
                LLog.print("设备ID 读取源: file="+ file);
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
                LLog.print("设备ID 读取源: SharedPreference");
                uuid = getUUID_SharedPreference(application);

                try(FileOutputStream out = new FileOutputStream(file)){

                    out.write(uuid.getBytes());
                    out.flush();
                }
            }

        } catch (Exception e) {
            LLog.error("无法获取设备或生成设备ID",e);
            uuid = "DEV-DEFAULT-RANDOM-" + System.currentTimeMillis();
        }

        return uuid;
    }

    private static String getUUID_SharedPreference(Application application) {
        SharedPreferences sp = sharedStorage(application);

        String uuid = sp.getString(APP_UUID_KEY,null);
        if (uuid == null){
            uuid = genUUID(application);
            sp.edit().putString(APP_UUID_KEY,uuid).apply();
        }
        return uuid;
    }

    public static String getMemoryDEVID() {
        return DEVID ;
    }

    public static String getShareDEVID(Context context) {
        return sharedStorage(context).getString(DEVID_KYD,DEVID);
    }



}
