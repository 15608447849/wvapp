package com.bottle.wvapp.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by Leeping on 2020/6/10.
 * email: 793065165@qq.com
 * 通过服务访问后台接口
 * ICE - 通讯进程
 */
public class BackServices extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
