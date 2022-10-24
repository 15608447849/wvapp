package lee.bottle.lib.toolset.os;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import androidx.core.app.NotificationCompat;

import java.util.Random;

/**
 * Created by Leeping on 2018/5/2.
 * email: 793065165@qq.com
 */

public class FrontNotification {

    public static class Build{
        Context context;
        int id = 1000;
        NotificationCompat.Builder builder;
        NotificationChannel notificationChannel;
        Notification notification;
        NotificationManager notificationManager;

        /*
        Notification.FLAG_SHOW_LIGHTS //三色灯提醒，在使用三色灯提醒时候必须加该标志符
        Notification.FLAG_ONGOING_EVENT //发起正在运行事件（活动中）
        Notification.FLAG_INSISTENT //让声音、振动无限循环，直到用户响应 （取消或者打开）
        Notification.FLAG_ONLY_ALERT_ONCE //发起Notification后，铃声和震动均只执行一次
        Notification.FLAG_AUTO_CANCEL //用户单击通知后自动消失
        Notification.FLAG_NO_CLEAR //只有全部清除时，Notification才会清除*/
        int[] flags = new int[]{Notification.FLAG_FOREGROUND_SERVICE,Notification.FLAG_NO_CLEAR};

        int defaults = Notification.DEFAULT_LIGHTS;

        Intent serviceIntent;//点击打开指定服务
        Intent activityIntent;//点击打开指定Activity

        private String channelId;

        private int level;

        public Build(Context c){
            context = c;
            int rannum = (int)( new Random().nextDouble()*(99999-10000 + 1))+ 10000;
            channelId =  context.getPackageName() + rannum;
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            builder = new NotificationCompat.Builder(context,channelId);
        }

        public FrontNotification.Build setId(int id){
            this.id = id;
            return this;
        }


        public FrontNotification.Build setActivityIntent(Intent intent){
            this.activityIntent = intent;
            return this;
        }

        public FrontNotification.Build setServiceIntent(Class<?> destCls){
            if (destCls!=null){
                serviceIntent = new Intent(context, destCls);
            }
            return this;
        }

        public FrontNotification.Build setServiceIntent(Intent intent){
            this.serviceIntent = intent;
            return this;
        }


        public FrontNotification.Build setFlags(int[] flags){
            this.flags = flags;
            return this;
        }

        public FrontNotification.Build setDefaults(int defaults){
            this.defaults = defaults;
            return this;
        }

        public FrontNotification.Build setGroup(String group){
            builder.setGroup(group);
            return this;
        }

        public FrontNotification.Build setSmallIcon(int rid) {
           builder.setSmallIcon(rid);
            return this;
        }

        public FrontNotification.Build setBigIcon(int rid) {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(),rid));
            return this;
        }

        public FrontNotification.Build setIcon(int rid) {
            return setSmallIcon(rid).setBigIcon(rid);
        }

        public FrontNotification.Build setTicker(String message){
            builder .setTicker(message);//通知首次出现在通知栏，带上升动画效果的
            return this;
        }

        public FrontNotification.Build setWhen(long time){
            builder.setWhen(time);//通知产生的时间，会在通知信息里显示，一般是系统获取到的时间
            return this;
        }

        public FrontNotification.Build setText(String title, String content, String info){
              builder.setContentTitle(title).setContentText(content).setContentInfo(info);
              return this;
        }

        public FrontNotification.Build setExpandText(String text){
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            return this;
        }

        public FrontNotification.Build setLevel(int level){
            this.level = level;
            return this;
        }


        @SuppressLint("WrongConstant")
        public FrontNotification generateNotification(){
            PendingIntent pi = null;
            if (activityIntent !=null){
//                pi = PendingIntent.getActivity(context,0, activityIntent,PendingIntent.FLAG_UPDATE_CURRENT);

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pi = PendingIntent.getActivity(context,0, activityIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                }else {
                    pi = PendingIntent.getActivity(context,0, activityIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_ONE_SHOT);
                }

            }else if (serviceIntent!=null){
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pi = PendingIntent.getService(context,0, serviceIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                }else {
                    pi = PendingIntent.getService(context,0, serviceIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_ONE_SHOT);
                }
            }
            if (pi!=null) {
                builder.setContentIntent(pi);
            }

            builder.setDefaults(defaults!=0?defaults:Notification.DEFAULT_ALL);

            builder.setVisibility(Notification.VISIBILITY_PRIVATE);

            androidO();

            buildAndroidNotify();


            return new FrontNotification(this);
        }


        private void androidO() {
            //android 8.0
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (notificationChannel!=null) return;

                notificationChannel = new NotificationChannel(
                        channelId ,
                        context.getPackageName()+" 通知栏",
                        level==0? NotificationManager.IMPORTANCE_HIGH : level);

                notificationChannel.setDescription(context.getPackageName()+ " 消息通知渠道");
                notificationChannel.setShowBadge(true); //是否在久按桌面图标时显示此渠道的通知
                notificationChannel.enableLights(true);//是否在桌面icon右上角展示小圆点
                notificationChannel.setLightColor(Color.WHITE);//小圆点颜色

//                notificationChannel.enableVibration(false);//允许震动
//                notificationChannel.setVibrationPattern(new long[]{0});
//                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//                notificationChannel.setSound(null,null);
//                notificationChannel.enableLights(true);//设置闪光灯
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        private void buildAndroidNotify() {
            notification = builder.build();

            if (flags!=null && flags.length>0){
                for (int flag : flags){
                    notification.flags |= flag;
                }
            }
        }

    }


    private FrontNotification.Build build;

    private FrontNotification(FrontNotification.Build build){
        this.build = build;
    }

    public void setProgress(int max,int current) {
        build.builder.setProgress(max,current,false);
        build.buildAndroidNotify();
        showNotification();
    }




    public void showNotification() {
        build.notificationManager.notify(build.id, build.notification);
    }

    public void cancelNotification() {
        try {
            build.notificationManager.cancel(build.id);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                //关闭通知通道
                build.notificationManager.deleteNotificationChannel(build.channelId);
            }
        } catch (Exception ignored) { }
    }

    public void startForeground(Service service){
        service.startForeground(build.id, build.notification);
    }
    public void stopForeground(Service service){
        service.stopForeground(false);
    }

}
