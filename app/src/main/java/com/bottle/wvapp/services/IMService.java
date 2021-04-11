package com.bottle.wvapp.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.activitys.NativeActivity;
import com.bottle.wvapp.app.ApplicationDevInfo;
import com.bottle.wvapp.tool.NotifyUer;
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.util.Timer;
import java.util.TimerTask;

import lee.bottle.lib.toolset.log.LLog;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.bottle.wvapp.app.ApplicationDevInfo.DEVTYPE;
import static com.bottle.wvapp.app.BusinessData.getOrderServerNo;
import static com.bottle.wvapp.app.BusinessData.refreshCompanyInfoAndOutput;


/*
* 后端通讯服务
* */
public class IMService extends Service {

    private final boolean isDebugger = true;

    /* 间隔保活定时器 */
    private final Timer timer = new Timer(true);

    /* ICE连接客户端 */
    final IceClient client = new IceClient(BuildConfig._ICE_TAG,BuildConfig._ADDRESS,BuildConfig._ARGS);

    /* 本地通讯端点 */
    private Ice.ObjectAdapter localAdapter;

    /* 长连接接收消息 */
    private final CommunicationServerImp receive = new CommunicationServerImp(this);

    /* 连接检测次数 */
    private long communicationCount = 0;

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
        super.onCreate();

    }

    private void openIceCommunication() {
        client.startCommunication();
        localAdapter = client.iceCommunication().createObjectAdapter("");
        localAdapter.activate();
    }

    synchronized void communicationCountSet(int number){
        communicationCount += number;
        if (isDebugger) LLog.print("连接检测: " + communicationCount);
    }

    private void openLongConnectionWatch() {
        // 打开长连接监听
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                communication();
            }
        }, 0,5 * 1000L);
    }


    /** 打开/关闭连接 */
    private synchronized void communication(){
        try {
            // 获取用户公司码
            int compid = refreshCompanyInfoAndOutput(false,null);

            String fn = "order2Service" + getOrderServerNo(compid) + "_1";

            // 当前用户信息异常
            if (compid == 0){
                if (receive.identity!=null) throw new IllegalArgumentException("本地用户信息不存在");
                return;
            }

            // 检查连接
            if(receive.checkCommunication(fn,String.valueOf(compid))) return;

            communicationOpen(fn,compid);
        } catch (Exception e) {
            LLog.print(receive + " IM 服务器 连接错误: " + e.getMessage());
            //断开本地长连接
            communicationClose();
        }
    }

    private void communicationOpen(String fn,int compid) {
        // 尝试连接服务器
        InterfacesPrx prx = client.settingProxy(fn).getProxy();
        receive.identity = new Ice.Identity(String.valueOf(compid),DEVTYPE);
        localAdapter.add(receive, receive.identity);
        prx.ice_getConnection().setAdapter(localAdapter);
        prx.online( receive.identity );
        LLog.print("********** IM 服务器 连接成功 **********");
    }

    // 关闭连接
    private void communicationClose() {
        if (receive.identity !=null){
            localAdapter.remove(receive.identity);
            receive.identity = null;
            LLog.print("********** IM 服务器 断开连接 **********");
            communicationCount=0;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isDebugger) LLog.print("IMService onStartCommand");
        setFrontService();
        return START_REDELIVER_INTENT;
    }

    private void setFrontService() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            NotificationManager manager = (NotificationManager)getSystemService (NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel ("channel_id_im","IM",NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel (channel);
            Notification notification = new Notification.Builder (this,"channel_id_im").build();
            startForeground (999,notification);
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
            String prop = message.substring(0,message.indexOf(":"));
            String msg = message.substring(message.indexOf(":")+1);

            Intent intent = new Intent(this, NativeActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);

            if (prop.startsWith("logout")){
                //刷新用户/ 企业信息
                int compid = refreshCompanyInfoAndOutput(true, client);
                String devID = msg.substring(0,msg.lastIndexOf("@"));
                String devType = msg.substring(msg.lastIndexOf("@")+1);
                String curDevID =  ApplicationDevInfo.getShareDevID(getApplication());

                if (devType.equals(ApplicationDevInfo.DEVTYPE)){
                    LLog.print("公司标识:" + compid+
                            "\n目标设备:" + devID +
                            "\n当前设备:" +curDevID);
                    if (compid == 0){
                        // 断开连接
                        communicationClose();
                        // 取消通知
                        NotifyUer.cacheAllMessageByExistNotify(getApplication());
                    }
                    if (prop.equals("logout-force") && !devID.equals(curDevID)){
                        // 当前设备被强制登出
                        // 通知activity 强制退出
                        intent.putExtra("forceLogout",true);
                        getApplication().startActivity(intent);
                    }
                }
            }

            if (message.equals("pay")){
                // 通知actyivity 支付结果
                intent.putExtra("pushPaySuccessMessageToJs",msg);
                getApplication().startActivity(intent);
            }

            //推送消息
            if (prop.equals("push") || prop.equals("custom")){
                String content = msg;
                String likePath = null;
                if (message.startsWith("custom")){
                    // 内容;链接
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
        } catch (Exception e) {
            LLog.print("接收长连接消息异常: "+ message+" , " + e.getMessage());
        }
    }

}
