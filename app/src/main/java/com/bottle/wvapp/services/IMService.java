package com.bottle.wvapp.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.bottle.wvapp.activitys.NativeActivity;
import com.bottle.wvapp.app.WebApplication;
import com.bottle.wvapp.tool.NotifyUer;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Ice.Connection;
import Ice.ConnectionCallback;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.ErrorUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import static com.bottle.wvapp.beans.BusinessData.getOrderServerNo;
import static com.bottle.wvapp.beans.BusinessData.getCurrentDevCompanyID;


/*
* 后端通讯服务
* */
public class IMService extends Service {

    private final boolean isDebugger = true;

    /* 定时器 */
    private final Timer timer = new Timer(true);

    /* 长连接接收消息 */
    private final CommunicationServer receive = new CommunicationServer(this);


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
       if (isDebugger) LLog.print(this+ " IMService onCreate");

        openLongConnectionWatch();
        startFrontServiceSDK26();

        super.onCreate();
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
        try {
            // 获取公司码
            int compid = getCurrentDevCompanyID(false,null);
            // 检查连接
            if(checkCommunication(String.valueOf(compid))) return;
            // 关闭连接
            communicationClose();
            // 尝试连接
            communicationOpen(compid);
        } catch (Exception e) {
            LLog.error(e);
        }
    }

    private void communicationOpen(int compid) {
        try {
            // 当前用户信息异常
            if (compid <= 0) return;

            String fn = "order2Service" + getOrderServerNo(compid) + "_1";
            Log.i("ice","连接服务: "+ fn+" 公司编码: "+ compid);
            // 尝试连接服务器
            receive.prx = WebApplication.iceClient.settingProxy(fn).getProxy();
            receive.prx.ice_ping();

            receive.identity = new Ice.Identity(String.valueOf(compid), WebApplication.DEVTYPE);
            WebApplication.iceClient.getLocalAdapter().add(receive, receive.identity);
            receive.prx.ice_getConnection().setAdapter(WebApplication.iceClient.getLocalAdapter());

            receive.prx.ice_getConnection().setCallback(new ConnectionCallback() {
                @Override
                public void heartbeat(Connection con) {
                    Log.w("ice","heartbeat:"+ con._toString().replace("\n"," "));
                    receive.lastHeartbeatTime = System.currentTimeMillis();

                }

                @Override
                public void closed(Connection con) {
                    Log.w("ice","closed:"+ con._toString().replace("\n"," "));
                    communicationClose();
                }
            });

            // 空闲时间,关闭策略,心跳策略
            receive.prx.ice_getConnection().setACM(
                    new Ice.IntOptional( 10 ),
                    new  Ice.Optional<>(Ice.ACMClose.CloseOff),
                    new  Ice.Optional<>(Ice.ACMHeartbeat.HeartbeatAlways)
            );

            receive.prx.online( receive.identity );

            LLog.print(this+ " IM 服务器 连接成功: " + compid +" , "+ receive.prx.ice_getConnection()._toString().replace("\n"," "));

            receive.lastHeartbeatTime = 0;
        } catch (Exception e) {
            LLog.error(e);
            communicationClose();
        }

    }

    /* 检查长连接是否有效 */
    boolean checkCommunication(String identityName) {
        try {
            if (receive.identity!=null && receive.identity.name.equals(identityName) ){

                if(receive.lastHeartbeatTime > 0){
                    long diff = System.currentTimeMillis() - receive.lastHeartbeatTime;
                    Log.w("ice","heartbeat time diff: "+ diff);
                    return diff <= 10 * 1000L;
                }
                if (receive.prx!=null){
                    receive.prx.ice_ping();
                    Log.w("ice","ping .... success");

                    return true;
                }
            }
        } catch (Exception e) {
            LLog.print("长连接检测异常\n" + ErrorUtils.printExceptInfo(e));
        }
        return false;
    }

    // 关闭连接
    protected void communicationClose() {
        if (receive.prx!=null){
            try{
                Connection connection = receive.prx.ice_getConnection();
                connection.close(true);
                LLog.print(this+ " IM 服务器 断开连接: " + connection._toString().replace("\n"," "));
                receive.lastHeartbeatTime = 0;

            }catch (Exception ignored){ }
            receive.prx = null;
        }
        if (receive.identity !=null){
            try { WebApplication.iceClient.getLocalAdapter().remove(receive.identity); } catch (Exception ignored) { }
            receive.identity = null;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startFrontServiceSDK26();
        if (isDebugger) LLog.print(this+ " IMService onStartCommand , start count: "+ startId);
        return START_REDELIVER_INTENT;
    }

    private void startFrontServiceSDK26() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            IMServiceSDK26FontServerUtil.openFontServer(this);
        }
    }

    private void stopFrontServiceSDK26() {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            IMServiceSDK26FontServerUtil.removeFontServer(this);
        }
    }

    @Override
    public void onLowMemory() {
        if (isDebugger) LLog.print(this+ " IMService onLowMemory");
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        if (isDebugger) LLog.print(this+ " IMService onTrimMemory - "+ level);
        super.onTrimMemory(level);
    }


    @Override
    public void onDestroy() {
        if (isDebugger) LLog.print(this+ " IMService onDestroy");
        timer.cancel();
        communicationClose();
        stopFrontServiceSDK26();
        super.onDestroy();
    }

    void sendDataToNativeActivity(Map<String,String> map){
        if (map == null) return;
        Intent intent = new Intent(this, NativeActivity.class);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        for (String k:map.keySet()){
            String v = map.get(k);
            intent.putExtra(k,v);
        }
        getApplication().startActivity(intent);
        if (isDebugger) LLog.print(this+ " send data to native Activity : "+ GsonUtils.javaBeanToJson(map));
    }



}
