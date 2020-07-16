package com.bottle.wvapp.tool;


import android.app.Notification;
import android.content.Context;
import android.content.Intent;

import com.bottle.wvapp.R;
import com.bottle.wvapp.activitys.SingleActivity;

import java.util.ArrayList;
import java.util.Arrays;

import lee.bottle.lib.toolset.os.FrontNotification;

/**
 * Created by Leeping on 2019/6/21.
 * email: 793065165@qq.com
 */
public class NotifyUer {
    private static int currentId =  1;
    public static void createMessageNotify(Context context, String message, String... params) {
        FrontNotification.Build build = new FrontNotification.Build(context).setId(currentId++);
        Intent intent = new Intent(context, SingleActivity.class);
        if (params!=null){
            ArrayList<String> paramList = new ArrayList<>(Arrays.asList(params));
            intent.putStringArrayListExtra("notify_param",paramList);
        }
//        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
//        build.setFlags(new int[]{Notification.FLAG_INSISTENT,Notification.FLAG_AUTO_CANCEL});
        build.setFlags(new int[]{Notification.FLAG_SHOW_LIGHTS,Notification.FLAG_AUTO_CANCEL});
        build.setActivityIntent(intent);
        build.setGroup("messageList");
        build.setDefaults(Notification.DEFAULT_ALL);
        build.setIcon(R.mipmap.ic_launcher);
        build.setText( context.getString(R.string.app_name),message,"点击进入");
        build.setWhen(System.currentTimeMillis());
        build.setTicker(message);
        build.generateNotification().showNotification();
    }

    public static FrontNotification createDownloadApkNotify(Context context,String title){
        Intent intent = new Intent(context, SingleActivity.class);
       return new FrontNotification.Build(context).setLevel(3)
                .setId(currentId++)
                .setGroup("download-"+title)
                .setFlags(new int[]{Notification.FLAG_FOREGROUND_SERVICE,Notification.FLAG_ONLY_ALERT_ONCE,
                        Notification.FLAG_ONGOING_EVENT,Notification.FLAG_NO_CLEAR})
                .setDefaults(Notification.DEFAULT_ALL)
               .setActivityIntent(intent)
                .setSmallIcon(R.drawable.ic_update_version)
               .setText(context.getString(R.string.app_name),title ,"下载完成自动关闭")
                .generateNotification();

    }

}
