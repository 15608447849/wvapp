package lee.bottle.lib.toolset.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lee.bottle.lib.toolset.log.LLog;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by Leeping on 2018/4/16.
 * email: 793065165@qq.com
 */
public class AppUtils {

    /** 读取手机通讯录
     * <uses-permission android:name="android.permission.READ_CONTACTS"/>
     * */
    private List<String> readContacts(Activity activity) {
        List<String> list = new ArrayList<>();
        ContentResolver resolver = activity.getContentResolver();
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
            list.add(name + ":" + phone);
        }
        return list;
    }

    /**
     * 判断应用是否存在指定权限
     */
    public static boolean checkPermissionExist(Context context, String permissionName) {
        //判断是否存在文件写入权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int hasPermission = context.checkSelfPermission(permissionName);
            return hasPermission != PackageManager.PERMISSION_DENIED;
        }
        return true;
    }

    /*
   检查浮窗是否授权
   AppOpsManager.MODE_ALLOWED —— 表示授予了权限并且重新打开了应用程序
   AppOpsManager.MODE_IGNORED —— 表示授予权限并返回应用程序
   AppOpsManager.MODE_ERRORED —— 表示当前应用没有此权限
   AppOpsManager.MODE_DEFAULT —— 表示默认值，有些手机在没有开启权限时，mode的值就是这个
   */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean checkWindowPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOpsMgr == null)
                return false;
            int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window", android.os.Process.myUid(), context
                    .getPackageName());
            return Settings.canDrawOverlays(context) || mode == AppOpsManager.MODE_ALLOWED || mode == AppOpsManager.MODE_IGNORED;
        }
        return Settings.canDrawOverlays(context);
    }

    /**
     * 隐藏软键盘
     */
    public static void hideSoftInputFromWindow(@NonNull Activity activity) {
        try {
            View v = activity.getCurrentFocus();
            if (v != null && v.getWindowToken() != null) {
                InputMethodManager inputMethodManager = ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE));
                if (inputMethodManager != null)
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /**
     *是否打开无线模块
     */
    public static boolean isOpenWifi(@NonNull Context context) {
        WifiManager mWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        assert mWifiManager != null;
        return mWifiManager.isWifiEnabled();
    }

    /**
     * 判断网络连接
     * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
     */
    @SuppressLint("MissingPermission")
    public static boolean isNetworkAvailable(@NonNull Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager != null) {
            try {
                NetworkInfo info = manager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    return true;
                }
            } catch (Exception ignored) {

            }
        }
        return false;
    }

    /* 获取本地文件输出流 */
    public static void getLocalFileToOutputStream(File file, OutputStream out) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] b = new byte[4096];
            int n;
            while ((n = fis.read(b)) != -1) {
                out.write(b, 0, n);
            }
        }
    }

    /* 将文件转换成Byte数组 */
    public static byte[] getBytesByFile(File file) {
        int len = 1024;
        try (FileInputStream fis = new FileInputStream(file)) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream(len)) {
                byte[] b = new byte[len];
                int n;
                while ((n = fis.read(b)) != -1) {
                    bos.write(b, 0, n);
                }
                return bos.toByteArray();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
        return null;
    }

    /* 解压zip */
    public static boolean unZipToFolder(InputStream zipFileStream, File dir) {
        try (ZipInputStream inZip = new ZipInputStream(zipFileStream)) {

            ZipEntry zipEntry;
            String temp;
            while ((zipEntry = inZip.getNextEntry()) != null) {
                temp = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    //获取部件的文件夹名
                    temp = temp.substring(0, temp.length() - 1);
                    File folder = new File(dir, temp);
                    folder.mkdirs();
                } else {
                    File file = new File(dir, temp);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs();
                        file.createNewFile();
                    }
                    // 获取文件的输出流
                    try (FileOutputStream out = new FileOutputStream(file)) {
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
            LLog.error(e);
        }
        return false;
    }

    /* 检查无线网络有效 */
    private boolean isWirelessNetworkValid(Context context) {
        return AppUtils.isOpenWifi(context) && AppUtils.isNetworkAvailable(context);
    }

    /* 判断GPS是否开启 */
    public static boolean isOenGPS(@NonNull Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /* 打开GPS设置界面 */
    public static void openGPS(@NonNull Context context) {
        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        // 打开GPS设置界面
        context.startActivity(intent);
    }

    /* 检查UI线程 */
    public static boolean checkUIThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    /* 获取当前进程名 */
    public static String getCurrentProcessName(@NonNull Context context) {
        int pid = android.os.Process.myPid();
        String processName = "";
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningAppProcessInfo process : manager.getRunningAppProcesses()) {
                if (process.pid == pid) {
                    processName = process.processName;
                }
            }
        }
        return processName;
    }

    /* 判断当前进程是否是主进程 */
    public static boolean checkCurrentIsMainProgress(@NonNull Context context) {
        return checkCurrentIsMainProgress(context, AppUtils.getCurrentProcessName(context));
    }

    /* 判断当前进程是否是主进程 */
    public static boolean checkCurrentIsMainProgress(@NonNull Context context, @NonNull String currentProgressName) {
        return context.getPackageName().equals(currentProgressName);
    }

    /* 获取应用版本号 */
    public static int getVersionCode(@NonNull Context ctx) {
        // 获取packagemanager的实例
        int version = 0;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionCode;
        } catch (Exception e) {
            LLog.error(e);
        }
        return version;
    }

    /* 获取应用版本名 */
    public static String getVersionName(@NonNull Context ctx) {
        // 获取package manager的实例
        String version = "";
        try {
            PackageManager packageManager = ctx.getPackageManager();
            PackageInfo packInfo = packageManager.getPackageInfo(ctx.getPackageName(), 0);
            version = packInfo.versionName;
        } catch (Exception e) {
            LLog.error(e);
        }
        return version;
    }

    /* 简单信息弹窗 */
    public static void toastLong(@NonNull Context context, @NonNull String message) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            if (toast != null) {
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* 简单信息弹窗 */
    public static void toastShort(@NonNull Context context, @NonNull String message) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            if (toast != null) {
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* 简单信息弹窗 */
    public static void toastCustom(@NonNull Context context, @NonNull String message, int duration, int gravity, View view) {
        if (!checkUIThread()) return;

        try {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            if (toast != null) {
                if (gravity > 0) {
                    toast.setGravity(gravity, 0, 0);
                }
                if (view != null) {
                    toast.setView(view);
                }
                toast.show();
            }
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* 把bitmap 转file */
    public static boolean bitmap2File(Bitmap bitmap, File file) {
        try {
            if (bitmap == null || file == null) return false;
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            return true;
        } catch (IOException e) {
            LLog.error(e);
        }
        return false;
    }

    /*
     * 创建快捷方式 ; 权限:  <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"/>
     * */
    public static void addShortcut(Context context, int appIcon, boolean isCheck) {

        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
            if (isCheck) {
                boolean isExist = sharedPreferences.getBoolean("shortcut", false);
                if (isExist) return;
            }
            Intent shortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
            Intent shortcutIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            final PackageManager pm = context.getPackageManager();
            String title = pm.getApplicationLabel(pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA)).toString();
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
            shortcut.putExtra("duplicate", false);
            Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, appIcon);
            shortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
            context.sendBroadcast(shortcut);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("shortcut", true);
            editor.apply();
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    //读取assets目录指定文件内容
    public static String assetFileContentToText(Context c, String filePath) {
        InputStream in = null;
        try {
            in = c.getAssets().open(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;
            StringBuilder sb = new StringBuilder();
            do {
                line = bufferedReader.readLine();
                if (line != null) {
                    line = line.replaceAll("\\t", "");
                    line = line.replaceAll("\\s", "");
                    sb.append(line);
                }
            } while (line != null);

            bufferedReader.close();
            in.close();
            return sb.toString();
        } catch (Exception e) {
            LLog.error(e);
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
    /*
    * <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
     * */
    public static boolean installApk(Context context, File apkFile,String apkUrl) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                boolean isInstallPermission = context.getPackageManager().canRequestPackageInstalls();
                LLog.print("安装APK sdk= " + Build.VERSION.SDK_INT +" 是否存在安装权限 : "+ isInstallPermission);
                if(!isInstallPermission){
                    // 权限没有打开则提示用户去手动打开
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES ,Uri.parse("package:" + context.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return false;
                }
            }

            LLog.print("安装APK file : " + apkFile);
            if (!apkFile.exists()) return false;

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri uri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".fileProvider", apkFile);
            } else {
                //apk放在cache文件中，需要获取读写权限
                String command = "chmod 777 " + apkFile.getAbsolutePath();
                Runtime runtime = Runtime.getRuntime();
                runtime.exec(command);
                uri = Uri.fromFile(apkFile);
            }
            LLog.print("安装APK uri : " + uri);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            LLog.error("安装APK 错误", e);
            // 尝试打开浏览器下载安装
            try{
                LLog.print("尝试使用浏览器打开 uri : " + apkUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }catch (Exception ex){
                LLog.error("安装APK 尝试使用浏览器打开失败", ex);
            }
            return false;
        }
    }



    /**
     * 获取设备mac
     * <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
     * <uses-permission android:name="android.permission.LOCAL_MAC_ADDRESS"/>
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
     *
     */
    @SuppressLint("HardwareIds")
    public static String devMAC(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo info = wifiManager.getConnectionInfo();
            if (info != null) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // 无权限
                    return getNewMac();
                }

                String mac = info.getMacAddress();
                if (mac.equalsIgnoreCase("02:00:00:00:00:00")) return getNewMac();
                return mac;

            }
        }
        return "00:00:00:00:00:00";
    }
    /**
     * 通过网络接口取
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
            LLog.error(ex);
        }
        return null;
    }



    /**
     * 拨打电话
     *<uses-permission android:name="android.permission.CALL_PHONE" />
     */
    public static void callPhoneNo(Activity activity, String phoneNo){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+phoneNo));
        activity.startActivity(intent);
    }

    public static void releaseImageView(ImageView iv) {
        if (iv == null) return;
        Drawable drawable = iv.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }


    /**
     * 获取剪切板内容
     */
    public static String getClipboardContent(Context context){
        String pasteString = "";

        try {
            ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (manager != null) {
                ClipData clipData = manager.getPrimaryClip();
                if (clipData!=null && clipData.getItemCount() > 0) {
                    CharSequence text = clipData.getItemAt(0).getText();
                    pasteString = text.toString();
                }
            }
        } catch (Exception e) {
            LLog.error(e);
        }

        return pasteString;
    }

    /* 设置剪切板内容 */
    public static void setClipboardContent(Context context,String content){
        try {
            // 得到剪贴板管理器
            ClipboardManager cmb = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);

            // 创建一个剪贴数据集，包含一个普通文本数据条目（需要复制的数据）
            ClipData clipData = ClipData.newPlainText(null, content);
            // 把数据集设置（复制）到剪贴板
            cmb.setPrimaryClip(clipData);

        } catch (Exception e) {
            LLog.error(e);
        }
    }

    /* 判断协议是否有效 */
    public static boolean schemeValid(Context context,String scheme) {
        PackageManager manager = context.getPackageManager();
        Intent action = new Intent(Intent.ACTION_VIEW);
        action.setData(Uri.parse(scheme));
        @SuppressLint("QueryPermissionsNeeded")
        List<ResolveInfo> list = manager.queryIntentActivities(action, PackageManager.GET_RESOLVED_FILTER);
        return list != null && list.size() > 0;
    }

    /* 跳转协议指定activity */
    public static void schemeJump(Context context,String scheme){
        Intent action = new Intent(Intent.ACTION_VIEW);
        action.setFlags(FLAG_ACTIVITY_NEW_TASK);
        action.setData(Uri.parse( scheme ));
        context.startActivity(action);
    }

    /* 获取状态栏高度 */
    public static int statusBarHeight(Context context){
        int statusBarHeight = -1;
        //获取status_bar_height资源的ID
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            //根据资源ID获取响应的尺寸值
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        if (statusBarHeight == -1){
            try {
                @SuppressLint("PrivateApi")
                Class<?> clazz = Class.forName("com.android.internal.R$dimen");
                Object object = clazz.newInstance();
                int height = Integer.parseInt(
                        String.valueOf(clazz.getField("status_bar_height").get(object))
                );
                statusBarHeight = context.getResources().getDimensionPixelSize(height);
            } catch (Exception e) {
                LLog.error(e);
            }
        }

        return statusBarHeight;
    }



    //调用该方法获取是否开启通知栏权限
    public static boolean isNotifyEnabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return isNotifyEnableV26(context);
        } else {
            return isNotifyEnabledV19(context);
        }
    }
    /**
     * 8.0以下判断
     * @param context api19  4.4及以上判断
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean isNotifyEnableV26(Context context) {

        AppOpsManager mAppOps =
                (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);

        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        Class<?> appOpsClass = null;

        try {
            appOpsClass = Class.forName(AppOpsManager.class.getName());

            Method checkOpNoThrowMethod =
                    appOpsClass.getMethod("checkOpNoThrow",
                            Integer.TYPE, Integer.TYPE, String.class);

            Field opPostNotificationValue = appOpsClass.getDeclaredField("OP_POST_NOTIFICATION");
            int value = (Integer) opPostNotificationValue.get(Integer.class);

            return ((Integer) checkOpNoThrowMethod.invoke(mAppOps, value, uid, pkg) ==
                    AppOpsManager.MODE_ALLOWED);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * 8.0及以上通知权限判断
     * @param context
     * @return
     */
    private static boolean isNotifyEnabledV19(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        try {
            NotificationManager notificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            @SuppressLint("DiscouragedPrivateApi")
            Method sServiceField = notificationManager.getClass().getDeclaredMethod("getService");
            sServiceField.setAccessible(true);
            Object sService = sServiceField.invoke(notificationManager);

            Method method = sService.getClass().getDeclaredMethod("areNotificationsEnabledForPackage",String.class, Integer.TYPE);
            method.setAccessible(true);
            return (boolean) method.invoke(sService, pkg, uid);
        } catch (Exception e) {
            return true;
        }
    }


}
