package com.bottle.wvapp.activitys;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import com.bottle.wvapp.R;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.BaseActivity;

public class MainActivity extends BaseActivity {



    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LLog.print("动态广播 启动");
            startNativeActivity();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LLog.print("定时任务 启动");
                startNativeActivity();
            }
        },5000);

        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction("GLOBAL_WEB_LOAD_COMPLETE");
        registerReceiver(broadcastReceiver,intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private boolean isOpen = false;

    public void startNativeActivity(){
        if (isOpen) return;
        Intent intent = new Intent(this,NativeActivity.class);
        startActivity(intent);
        finish();
        isOpen = true;
    }

}