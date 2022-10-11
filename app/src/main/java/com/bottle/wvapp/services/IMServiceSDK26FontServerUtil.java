package com.bottle.wvapp.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.bottle.wvapp.R;
import lee.bottle.lib.toolset.log.LLog;

import static android.content.Context.NOTIFICATION_SERVICE;

/*
* 后端通讯服务
* */
public class IMServiceSDK26FontServerUtil {

    private static boolean isAccept = false;

    private static final String channel_name = "im_server_channel_id_im";
    private static final int channel_id = 999;

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void openFontServer(Service service){
        if (!isAccept) return;

        // Android 8.0 不再允许后台进程直接通过startService方式去启动服务
        NotificationManager manager = (NotificationManager)service.getSystemService (NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel (channel_name,service.getString(R.string.app_name),NotificationManager.IMPORTANCE_LOW);
        manager.createNotificationChannel (channel);
        Notification notification = new Notification.Builder (service, channel_name).setContentText("安全服务").build();
        service.startForeground (channel_id,notification);

        LLog.print("android 8.+ Service ("+ service +") set FrontService");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void removeFontServer(Service service){
        if (!isAccept) return;

        NotificationManager manager = (NotificationManager)service.getSystemService (NOTIFICATION_SERVICE);
        if (manager == null) return;
        try {
            manager.cancel(channel_id);
            manager.deleteNotificationChannel(channel_name);
        }catch (Exception e){
            LLog.error(e);
        } finally {
            service.stopForeground(true);
        }
        LLog.print("android 8.+ Service ("+ service +") stop FrontService");
    }



}
