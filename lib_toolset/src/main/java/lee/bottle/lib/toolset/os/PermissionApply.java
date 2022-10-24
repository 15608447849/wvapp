package lee.bottle.lib.toolset.os;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

import static lee.bottle.lib.toolset.util.AppUtils.checkWindowPermission;


/**
 * Created by Leeping on 2018/6/20.
 * email: 793065165@qq.com
 */

public class PermissionApply {

     public interface Callback{
         void onPermissionsGranted();
         void onPowerIgnoreGranted();
         void onSDK30FileStorageRequestResult(boolean isGrant);
     }

    private final SoftReference<Activity> activityRef;

    //假设权限被拒绝
    private volatile boolean isPermissionsDenied = true ;

    // 申请权限
    private final int SDK_PERMISSION_REQUEST = 127;

    // 系统设置权限申请
    private final int SDK_POWER_REQUEST = 128;

    // 浮窗
    private final int OVERLAY_PERMISSION_REQ_CODE = 129;

    // android11 使用外部文件权限申请
    private final int ANDROID11_EXTERNAL_STORAGE_MANAGER_REQ_CODE = 130;

    // android 允许未知来源权限界面申请
    private final int INSTALL_PERMISSION_CODE = 131;

    // android 允许消息通知栏
    private final int REQUEST_NOTIFY_CODE = 132;

    // 需要请求权限的列表
    private final String[] permissions ;

    private final PermissionApply.Callback callback;


    public PermissionApply(Activity activity, String[] permissions, PermissionApply.Callback callback) {
        this.activityRef = new SoftReference<>(activity);
        this.permissions = permissions;
        this.callback = callback;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) isPermissionsDenied = false;
    }

    public boolean isPermissionsDenied(){
        return isPermissionsDenied;
    }

    //应用权限检测
    public boolean permissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return getPermissions();
        }
        return true;
    }
    /**
     * 请求用户给予悬浮窗的权限
     */
    @TargetApi(23)
    @SuppressLint("WrongConstant")
    public void askFloatWindowPermission() {
        Activity activity = activityRef.get();
        if (activity == null) return;

        if (!checkWindowPermission(activity)) {
            //弹窗请求授权浮窗
            final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    if (which == DialogInterface.BUTTON_POSITIVE){
                        openFloatWindowPower();
                    }
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("请求悬浮窗") ;//设置标题
            builder.setMessage("应用部分功能需要使用悬浮窗,是否授权?") ;//设置内容
            PackageManager pkm = activity.getPackageManager();
            try {
                Drawable mAppicon = pkm.getActivityInfo(activity.getComponentName(), ActivityInfo.FLAG_STATE_NOT_NEEDED).loadIcon(pkm);
                builder.setIcon(mAppicon);//设置图标，
            } catch (PackageManager.NameNotFoundException e) {
                LLog.error(e);
            }
            builder.setPositiveButton("授权",listener);
            builder.setNegativeButton("取消",listener);
            builder.setCancelable(false);
            builder.create().show();
        }
    }



    // 打开浮窗授权
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openFloatWindowPower() {
        Activity activity = activityRef.get();
        if (activity == null) return;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
    }

    /**
     * 针对M以上的Doze模式忽略电池的优化
     <!--可以直接弹出一个系统对话框让用户直接添加app到白名单-->
     <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
     */
    @SuppressLint("BatteryLife")
    public boolean isIgnoreBatteryOption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        Activity activity = activityRef.get();
        if (activity == null) return false;

        try {
            PowerManager pm = (PowerManager)activity. getSystemService(Context.POWER_SERVICE);
            if (pm == null) return false;

                if (!pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData( Uri.parse("package:" + activity.getPackageName()));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivityForResult(intent,SDK_POWER_REQUEST);
                    return false;
                }

        } catch (Exception e) {
            LLog.error(e);
        }
        return true;
    }

    @TargetApi(30)
    public void sdk30_isExternalStorageManager(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            LLog.print("ANDROID 11 申请外部文件权限");
            Activity activity = activityRef.get();
            if (activity == null) return ;
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                activity.startActivityForResult(intent, ANDROID11_EXTERNAL_STORAGE_MANAGER_REQ_CODE);
            }
        }

    }

    //获取权限
    @TargetApi(23)
    private boolean getPermissions() {
        Activity activity = activityRef.get();
        if (activity == null) return false;

        isPermissionsDenied = false;
        for (String permission : permissions) {
            boolean isDenied = activity.checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED;
            LLog.print( "检查设备权限>> "+ permission+" 权限拒绝: "+  isDenied);
            if (isDenied) {
                isPermissionsDenied = true;
                break;
            }
        }

        if(isPermissionsDenied){
            activity.requestPermissions(permissions, SDK_PERMISSION_REQUEST);
        }
        return !isPermissionsDenied();
    }

    private void startSysNotifyActivity() {
        Activity activity = activityRef.get();
        if (activity == null) return;

        Intent intent = new Intent();
        //直接跳转到应用通知设置的代码：
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0及以上
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:"+ activity.getPackageName()));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0以上到8.0以下
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.putExtra("app_package", activity.getPackageName());
            intent.putExtra("app_uid", activity.getApplicationInfo().uid);
        }
        activity.startActivityForResult(intent,REQUEST_NOTIFY_CODE);
    }

    private boolean isAlertWindowNotifyPermissionsDeniedIng = false;
    //授权失败提示框 >> 打开系统应用
    @SuppressLint("WrongConstant")
    private void alertWindowNotifyPermissionsDenied() {
        LLog.print("打开授权请求提示框 isAlertWindowNotifyPermissionsDeniedIng = "+ isAlertWindowNotifyPermissionsDeniedIng );

        Activity activity = activityRef.get();
        if (activity == null || isAlertWindowNotifyPermissionsDeniedIng) return;

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                isAlertWindowNotifyPermissionsDeniedIng = false;
                if (which == DialogInterface.BUTTON_POSITIVE){
                    startSysSettingActivity();
                }else if (which == DialogInterface.BUTTON_NEGATIVE){
                    System.exit(0);
                }else if (which == DialogInterface.BUTTON_NEUTRAL){
                    isAlertWindowNotifyPermissionsDeniedIng = true;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        PackageManager pkm = activity.getPackageManager();
        try {
            Drawable mAppicon =
                    pkm.getActivityInfo(activity.getComponentName(),
                            ActivityInfo.FLAG_STATE_NOT_NEEDED).loadIcon(pkm);
            builder.setIcon(mAppicon);//设置图标，
        } catch (PackageManager.NameNotFoundException e) {
            LLog.error(e);
        }
        builder.setTitle("应用授权失败") ;//设置标题
        builder.setMessage("您已拒绝应用相关权限,可能无法正常使用") ;//设置内容
        builder.setPositiveButton("手动授权",listener);
        builder.setNegativeButton("退出应用",listener);
        builder.setNeutralButton("继续使用",listener);
        builder.setCancelable(false);

        builder.create().show();
        isAlertWindowNotifyPermissionsDeniedIng = true;
    }

    //打开系统应用
    private void  startSysSettingActivity() {
        Activity activity = activityRef.get();
        if (activity == null) return;

        Intent intent = new Intent();
        intent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS) ;
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        activity.startActivityForResult(intent,SDK_POWER_REQUEST);
    }


    // 打开 允许安装未知来源 界面
    public void openInstallPermission() {
        Activity activity = activityRef.get();
        if (activity == null) return;

        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES ,Uri.parse("package:" + activity.getPackageName()));
        }else {
            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS ,Uri.parse("package:" + activity.getPackageName()));
        }
        activity.startActivityForResult(intent, INSTALL_PERMISSION_CODE);
    }


    private boolean isAlertMessageNotifyPermissionsDeniedIng = false;
    // 请求通知栏
    public void requestNotify(){
        final Activity activity = activityRef.get();
        if (activity == null || isAlertMessageNotifyPermissionsDeniedIng) return;

        boolean isEnable = AppUtils.isNotifyEnabled(activity);
        if (isEnable) return;

        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    startSysNotifyActivity();
                }
                isAlertMessageNotifyPermissionsDeniedIng = false;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("授权请求") ;//设置标题
        builder.setMessage("应用请求打开通知栏消息提醒") ;//设置内容
        builder.setPositiveButton("手动打开",listener);
        builder.setNegativeButton("下次再说",listener);
        builder.setCancelable(false);
        builder.create().show();
        isAlertMessageNotifyPermissionsDeniedIng = true;
    }


    /** activity 权限申请回调 */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){

        if (permissions!=null && grantResults!=null && permissions.length == grantResults.length){
            StringBuilder sb = new StringBuilder();
            sb.append("应用权限申请结果:\n");
            for (int i = 0; i<permissions.length;i++){
                sb.append(permissions[i]).append(" > ").append(grantResults[i]==-1?" 拒绝":" 允许").append("\n");
            }
            LLog.print(sb.toString());
        }

        if (requestCode == SDK_PERMISSION_REQUEST && grantResults!=null) {
            isPermissionsDenied = false; //假设授权没有被拒绝

            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    isPermissionsDenied = true;//发现有一个权限未授予,则无权限访问
                    break;
                }
            }
            if (isPermissionsDenied) {
                alertWindowNotifyPermissionsDenied();
            } else {
                callback.onPermissionsGranted();
            }
        }
    }


    /**
     * activity 回调
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        LLog.print("权限处理结果>> 回调 onActivityResult " +  requestCode +" "+ resultCode);

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE){
            permissionCheck();
        }

        if (requestCode == SDK_POWER_REQUEST){
            if (isIgnoreBatteryOption()){
                callback.onPowerIgnoreGranted();
            }
        }

        if (requestCode == ANDROID11_EXTERNAL_STORAGE_MANAGER_REQ_CODE){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                LLog.print("ANDROID 11 申请外部文件权限 结果: " + Environment.isExternalStorageManager());
                callback.onSDK30FileStorageRequestResult(Environment.isExternalStorageManager());
            }
        }


        if (requestCode == INSTALL_PERMISSION_CODE){
            LLog.print("ANDROID 未知来源权限申请结果: " +  ( resultCode == Activity.RESULT_OK));
        }

        if (requestCode == REQUEST_NOTIFY_CODE){
            LLog.print("ANDROID 允许消息通知栏结果: " +  ( resultCode == Activity.RESULT_OK));
        }
    }
}
