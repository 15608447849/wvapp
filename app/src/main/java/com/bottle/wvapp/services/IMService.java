package com.bottle.wvapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.activitys.NativeActivity;
import com.bottle.wvapp.app.ApplicationDevInfo;
import com.bottle.wvapp.tool.NotifyUer;
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import Ice.Connection;
import Ice.ConnectionCallback;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.bottle.wvapp.app.ApplicationDevInfo.DEVTYPE;
import static com.bottle.wvapp.app.BusinessData.getOrderServerNo;
import static com.bottle.wvapp.app.BusinessData.getCurrentDevCompanyID;


/*
* 后端通讯服务
* */
public class IMService extends Service {

    private final boolean isDebugger = false;

    /* 间隔保活定时器 */
    private final Timer timer = new Timer(true);

    /* ICE连接客户端 */
    final IceClient client = new IceClient(BuildConfig._ICE_TAG,BuildConfig._ADDRESS,BuildConfig._ARGS);

    /* 长连接接收消息 */
    private final CommunicationServerImp receive = new CommunicationServerImp(this);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
       if (isDebugger) LLog.print("IMService onCreate");
        openIceCommunication();
        openLongConnectionWatch();
        setFrontService();
        super.onCreate();

    }

    private void openIceCommunication() {
        client.startCommunication();
    }

    private void openLongConnectionWatch() {
        // 打开长连接监听
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                communication();
            }
        }, 1000L,10 * 1000L);
    }


    /** 打开/关闭连接 */
    private synchronized void communication(){
        // 获取公司码
        int compid = getCurrentDevCompanyID(false,null);
        // 检查连接
        if(checkCommunication(String.valueOf(compid))) return;
        // 关闭连接
        communicationClose();
        // 尝试连接
        communicationOpen(compid);
    }

    private void communicationOpen(int compid) {
        try {
            // 当前用户信息异常
            if (compid <= 0) return;

            String fn = "order2Service" + getOrderServerNo(compid) + "_1";
//            String fn = "order2Server" + getOrderServerNo(compid);
            LLog.print("尝试连接> 服务: "+ fn+" , "+ compid);
            // 尝试连接服务器
            receive.prx = client.settingProxy(fn).getProxy();
            receive.prx.ice_ping();

//            receive.identity = new Ice.Identity(String.valueOf(compid),DEVTYPE+"-ice_heartbeat:true");
            receive.identity = new Ice.Identity(String.valueOf(compid),DEVTYPE);
            client.getLocalAdapter().add(receive, receive.identity);
            receive.prx.ice_getConnection().setAdapter(client.getLocalAdapter());
            receive.prx.ice_getConnection().setCallback(new ConnectionCallback() {
                @Override
                public void heartbeat(Connection con) {
                    Log.i("ice","heartbeat:"+ con);
                    receive.lastHeartbeatTime = System.currentTimeMillis();
                }

                @Override
                public void closed(Connection con) {
                    Log.i("ice","closed:"+ con);
                    communicationClose();
                }
            });

            // 空闲时间,关闭策略,心跳策略
//            receive.prx.ice_getConnection().setACM(
//                    new Ice.IntOptional( 10 ),
//                    new  Ice.Optional<>(Ice.ACMClose.CloseOff),
//                    new  Ice.Optional<>(Ice.ACMHeartbeat.HeartbeatAlways)
//            );

            receive.prx.online( receive.identity );

            LLog.print("IM 服务器 连接成功: " + compid +" , "+ receive.prx.ice_getConnection());

            receive.lastHeartbeatTime = 0;
        } catch (Exception e) {
            e.printStackTrace();
            communicationClose();
        }

    }

    /* 检查长连接是否有效 */
    boolean checkCommunication(String identityName) {
        try {
            if (receive.identity!=null && receive.identity.name.equals(identityName) ){

                if(receive.lastHeartbeatTime > 0){
                    long diff = System.currentTimeMillis() - receive.lastHeartbeatTime;
                    Log.i("ice","heartbeat time diff: "+ diff);
                    return diff <= 10 * 1000L;
                }
                if (receive.prx!=null){
                    receive.prx.ice_ping();
                    Log.i("ice","ping ok");
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LLog.print("长连接检测异常: " + e.getMessage());
        }
        return false;
    }

    // 关闭连接
    private void communicationClose() {
        if (receive.prx!=null){
            try{
                Connection connection = receive.prx.ice_getConnection();
                connection.close(true);
                LLog.print("IM 服务器 断开连接: " + connection);
                receive.lastHeartbeatTime = 0;
            }catch (Exception ignored){ }
            receive.prx = null;
        }
        if (receive.identity !=null){
            try { client.getLocalAdapter().remove(receive.identity); } catch (Exception ignored) { }
            receive.identity = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isDebugger) LLog.print("IMService onStartCommand");
        return START_REDELIVER_INTENT;
    }

    private void setFrontService() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationManager manager = (NotificationManager)getSystemService (NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel ("im_server_channel_id_im","im_service",NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel (channel);
            Notification notification = new Notification.Builder (this,"im_server_channel_id_im").build();
            startForeground (9999,notification);
            if (isDebugger) LLog.print("IMService setFrontService");
        }
    }

    @Override
    public void onLowMemory() {
        if (isDebugger) LLog.print("IMService onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if (isDebugger) LLog.print("IMService onTrimMemory - "+ level);
        super.onTrimMemory(level);
    }


    @Override
    public void onDestroy() {
        if (isDebugger) LLog.print("IMService onDestroy");
        timer.cancel();
        communicationClose();
        receive.isRunning = false;
        client.stopCommunication();
        super.onDestroy();
    }


    void handlerMessage(String message) {
        try {
            if (isDebugger) LLog.print("处理长连接消息: "+ message);
            String prop = message.substring(0,message.indexOf(":"));
            String msg = message.substring(message.indexOf(":")+1);

            Intent intent = new Intent(this, NativeActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);

            protocol_ref(prop,msg,intent);
            protocol_logout(prop,msg,intent);
            protocol_payResult(prop,msg,intent);
            protocol_pushMessage(prop,msg,intent);

        } catch (Exception e) {
            LLog.print("接收长连接消息异常: "+ message+" , " + e.getMessage());
        }
    }

    private int updateLocalCacheReturnCompanyID(){
//        LLog.print("更新本地用户信息");
        int compid = getCurrentDevCompanyID(true, client);
        if (compid == 0){
            // 断开连接
            communicationClose();
            // 取消通知
            NotifyUer.cacheAllMessageByExistNotify(getApplication());
            // 通知activity刷新
        }
        return compid;
    }

    //更新本地企业信息
    private void protocol_ref(String prop, String msg, Intent intent) {
        if (prop.equals("ref")){
            LLog.print("服务器用户信息存在更新");
            updateLocalCacheReturnCompanyID();
        }
    }

    //登录/强制登录
    private void protocol_pushMessage(String prop, String msg, Intent intent) {
        if (prop.startsWith("logout")){
            LLog.print("登出信息: "+ msg);
            String message = null;
            //刷新用户/ 企业信息
            int compid = updateLocalCacheReturnCompanyID();
            String devID = msg.substring(0,msg.lastIndexOf("@"));
            String devType = msg.substring(msg.lastIndexOf("@")+1);
            String curDevID =  ApplicationDevInfo.getShareDevID(getApplication());

            if (devType.equals(ApplicationDevInfo.DEVTYPE)){

                LLog.print("公司标识: " + compid
                        + "\n目标设备: " + devID
                        + "\n当前设备: " + curDevID);
                if (devID.equals(curDevID)){
                    message = "您已从当前设备退出";
                }else{
                    if (prop.equals("logout-force")){
                        // 当前设备被强制登出
                        // 通知activity 强制退出
                        intent.putExtra("forceLogout",true);
                        getApplication().startActivity(intent);
                        message = "您的账号已在其他设备进行登录";
                    }
                }
                /*if (prop.equals("logout-force") && !devID.equals(curDevID)){
                    // 当前设备被强制登出
                    // 通知activity 强制退出
                    intent.putExtra("forceLogout",true);
                    getApplication().startActivity(intent);
                    message = "您的账号已在其他设备进行登录";
                }*/
            }else{
                message = "您的账号已从"+devType+"设备登出";
            }
            if (message!=null) NotifyUer.createMessageNotifyTips(getApplication(), message);
        }
    }

    //支付结果
    private void protocol_payResult(String prop, String msg, Intent intent) {
//    String msg = "pay:" + jsonObject.toJSONString();
//            IceRemoteUtil.sendMessageToClient(compid, msg);
        if (prop.equals("pay")){
            LLog.print("支付结果: "+ msg);
            // 通知actyivity 支付结果
            intent.putExtra("pushPaySuccessMessageToJs",msg);
            getApplication().startActivity(intent);
        }
    }
    //推送消息
    private void protocol_logout(String prop, String msg, Intent intent) {

        if (prop.equals("push") || prop.equals("custom")){
            LLog.print("推送消息: "+ msg);

            String content = msg;
            String likePath = null;

            if (prop.startsWith("custom")){
                // 自定义推送消息: 内容;链接
                String[] arr = content.split(";");
                if (arr.length >= 1){
                    content = arr[0];
                }
                if (arr.length >= 2){
                    likePath = arr[1];
                }
            }

            // 发送推送消息
            intent.putExtra("pushMessageToJs",content);
            startActivity(intent);

            //打开广播-跳转个人中心
            NotifyUer.createMessageNotify(getApplication(), content, likePath);
        }
    }

}
