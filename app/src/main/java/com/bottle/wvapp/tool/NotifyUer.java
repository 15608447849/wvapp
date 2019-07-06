package com.bottle.wvapp.tool;


import android.app.Notification;
import android.content.Context;
import android.content.Intent;

import com.bottle.wvapp.R;
import com.bottle.wvapp.activitys.SingleActivity;

import lee.bottle.lib.toolset.util.FrontNotification;

/**
 * Created by Leeping on 2019/6/21.
 * email: 793065165@qq.com
 */
public class NotifyUer {
    private static int currentId =  1;
    public static void createMessageNotify(Context context,String message) {
        FrontNotification.Build build = new FrontNotification.Build(context, 4).setId(currentId++);
        Intent intent = new Intent(context, SingleActivity.class);
//        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        build.setFlags(new int[]{Notification.FLAG_INSISTENT,Notification.FLAG_AUTO_CANCEL});
        build.setActivityIntent(intent);
        build.setGroup("messageList");
        build.setDefaults(Notification.DEFAULT_ALL);
        build.setIcon(R.drawable.ic_message);
        build.autoGenerateNotification(
                "一块医药",
                message,
                "点击进入")
                .showNotification();
    }
    public static FrontNotification createDownloadApkNotify(Context context){
        Intent intent = new Intent(context, SingleActivity.class);
       return new FrontNotification.Build(context,3)
                .setId(currentId++)
                .setGroup("download")
                .setFlags(new int[]{Notification.FLAG_INSISTENT,Notification.FLAG_AUTO_CANCEL})
                .setDefaults(Notification.DEFAULT_ALL)
               .setActivityIntent(intent)
                .setSmallIcon(R.drawable.ic_update_version)
                .autoGenerateNotification(
                        context.getString(R.string.app_name),
                        "正在下载","下载完成自动关闭");

    }

}
