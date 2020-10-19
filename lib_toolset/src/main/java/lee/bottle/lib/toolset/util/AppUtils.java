package lee.bottle.lib.toolset.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lee.bottle.lib.toolset.log.LLog;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.content.Context.TELEPHONY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by Leeping on 2018/4/16.
 * email: 793065165@qq.com
 */
public class AppUtils {
    /** 读取手机通讯录
     * <uses-permission android:name="android.permission.READ_CONTACTS"/>
     * */
    private List<String> readContacts(Activity activity){
        List<String> list = new ArrayList<>();
        ContentResolver resolver  = activity.getContentResolver();
        //用于查询电话号码的URI
        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        // 查询的字段
        String[] projection = {ContactsContract.CommonDataKinds.Phone._ID,//Id
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,//通讯录姓名
                ContactsContract.CommonDataKinds.Phone.DATA1, "sort_key",//通讯录手机号
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,//通讯录Id
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID,//手机号Id
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY};
        @SuppressLint("Recycle") Cursor cursor = resolver.query(phoneUri, projection, null, null, null);
        assert cursor != null;
        while ((cursor.moveToNext())) {
            String name = cursor.getString(1);
            String phone = cursor.getString(2);
            list.add(name+":"+phone);
        }
        return list;
    }

    /**
     * 判断应用是否存在指定权限
     */
    public static boolean checkPermissionExist(Context context,String permissionName){
        //判断是否存在文件写入权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M){
            int hasPermission = context.checkSelfPermission(permissionName);
            return hasPermission != PackageManager.PERMISSION_DENIED;
        }
        return true;
    }

    /**
     * 隐藏软键盘
     * @param activity
     */
    public static void hideSoftInputFromWindow(@NonNull Activity activity){
        try {
            View v = activity.getCurrentFocus();
            if (v!=null && v.getWindowToken()!=null){
                InputMethodManager inputMethodManager =  ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE));
                if (inputMethodManager!=null)  inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     *是否打开无线模块
     * @param context
     * @return
     */
    public static boolean isOpenWifi(@NonNull Context context){
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert mWifiManager != null;
        return mWifiManager.isWifiEnabled();
    }
    /**
     * @param context 上下文
     * @return 仅仅是用来判断网络连接
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
     */
    @SuppressLint("MissingPermission")
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager != null) {
            try {
                NetworkInfo info = manager.getActiveNetworkInfo();
                LLog.print("网络状态: " + (info != null && info.isAvailable()));
                if (info != null && info.isAvailable()) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static byte[] getLocalFileByte(File image) {
        byte[] buffer = null;
        try {
            FileInputStream fis = new FileInputStream(image);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    public static boolean unZipToFolder(InputStream zipFileStream, File dir) {
        try (ZipInputStream inZip = new ZipInputStream(zipFileStream)){

            ZipEntry zipEntry;
            String temp;
            while ((zipEntry = inZip.getNextEntry()) != null) {
                temp = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    //获取部件的文件夹名
                    temp = temp.substring(0, temp.length() - 1);
                    File folder = new File(dir,temp);
                    folder.mkdirs();
                }else{
                    File file = new File(dir, temp);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    // 获取文件的输出流
                    try(FileOutputStream out = new FileOutputStream(file)){
                        int len;
                        byte[] buffer = new byte[1024];
                        while ((len = inZip.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                            out.flush();
                        }
                    }
                }

            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  false;
    }



    //检查无线网络有效
    private boolean isWirelessNetworkValid(Context context) {
        return AppUtils.isOpenWifi(context) && AppUtils.isNetworkAvailable(context);
    }
   //判断GPS是否开启
    public static boolean isOenGPS(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    //打开GPS设置界面
    public static void openGPS(@NonNull Context context){
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        // 打开GPS设置界面
        context.startActivity(intent);
    }
    //检查UI线程
    public static boolean checkUIThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
    //获取当前进程名
    public static String getCurrentProcessName(@NonNull Context context) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager!=null){
            for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
                if (process.pid == pid) {
                    processName = process.processName;
                }
            }
        }
        return processName;
    }
    //判断当前进程是否是主进程
    public static boolean checkCurrentIsMainProgress(@NonNull Context context){
        return checkCurrentIsMainProgress(context, AppUtils.getCurrentProcessName(context));
    }
    //判断当前进程是否是主进程
    public static boolean checkCurrentIsMainProgress(@NonNull Context context, @NonNull String currentProgressName){
        return context.getPackageName().equals(currentProgressName);
    }
    //获取应用版本号
    public static int getVersionCode(@NonNull Context ctx) {
        // 获取packagemanager的实例
        int version = 0;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }
    //获取应用版本名
    public static String getVersionName(@NonNull Context ctx) {
        // 获取package manager的实例
        String version = "";
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }
    //简单信息弹窗
    public static void toast(@NonNull Context context, @NonNull String message){
        if (!checkUIThread() ) return;
        Toast.makeText(context,message, Toast.LENGTH_SHORT).show();
    }
    //把bitmap 转file
    public static boolean bitmap2File(Bitmap bitmap, File file){
        try {
            if (bitmap==null || file==null) return false;
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    //创建快捷方式 ; 权限:  <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
    public static void addShortcut(Context context, int appIcon, boolean isCheck) {

        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            if (isCheck){
                boolean isExist = sharedPreferences.getBoolean("shortcut", false);
                if (isExist) return;
            }
            Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            Intent shortcutIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            final PackageManager pm = context.getPackageManager();
            String title = pm.getApplicationLabel( pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra("duplicate", false);
            Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context,appIcon);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            context.sendBroadcast(shortcut);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("shortcut", true);
            editor.apply();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    //读取assets目录指定文件内容
    public static String assetFileContentToText(Context c, String filePath) {
        InputStream in = null;
        try {
            in = c.getAssets().open(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line ;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    line = line.replaceAll("\\t", "");
                    line = line.replaceAll("\\s", "");

//                    if (!line.matches("^\\s*\\/\\/.*")) {
                        sb.append(line);
//                    }
                }
            } while (line != null);

            bufferedReader.close();
            in.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }
    // 安装apk
    public static void installApk(Context context, File apkFile) {
        try {

            LLog.print("安装: " + apkFile +" , 存在 = "+ apkFile.exists());
            if (!apkFile.exists()) return;
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileprovider", apkFile);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }else{
                try {
                    //apk放在cache文件中，需要获取读写权限
                    String command = "chmod 777 "+apkFile.getAbsolutePath();
                    Runtime runtime = Runtime.getRuntime();
                    runtime.exec(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                uri = Uri.fromFile(apkFile);
            }
            LLog.print("安装APK: " + uri );
            intent.setDataAndType(uri,"application/vnd.android.package-archive");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
     */
    @SuppressLint("HardwareIds")
    public static String devMAC(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager!=null){
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info!=null){
                String mac =  info.getMacAddress();
                if (mac.equalsIgnoreCase("02:00:00:00:00:00")) return getNewMac();
            }
        }
        return "";
    }
    /**
     * 通过网络接口取
     * @return
     */
    private static String getNewMac() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return null;
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     *  获取设备IMEI
     * <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
     */
    @SuppressLint({"MissingPermission", "HardwareIds"})
    public static String devOnlyCode(Context context){
        StringBuilder sb = new StringBuilder();

        sb.append(Build.FINGERPRINT).append(";").
        append( Arrays.toString(Build.SUPPORTED_ABIS) ).append(";");
        //物理地址
        sb.append(devMAC(context)).append(";");

        if (checkPermissionExist(context,READ_PHONE_STATE)){
            TelephonyManager telephonyMgr = (TelephonyManager)context.getSystemService(TELEPHONY_SERVICE);
            if (telephonyMgr != null){
                String deviceID  = telephonyMgr.getDeviceId();
                if (deviceID!=null){
                    sb.append(deviceID ).append(";");
                    String sel = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ? Build.getSerial() : Build.SERIAL;
                    sb.append(sel).append(";");
                }
            }
        }
        return sb.toString();
    }


    /**
     * 拨打电话
     *<uses-permission android:name="android.permission.CALL_PHONE" />
     */
    public static void callPhoneNo(Activity activity,String phoneNo){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+phoneNo));
        activity.startActivity(intent);
    }

}
