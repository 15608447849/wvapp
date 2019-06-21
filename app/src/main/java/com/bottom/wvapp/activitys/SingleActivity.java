package com.bottom.wvapp.activitys;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottom.wvapp.R;

import lee.bottle.lib.singlepageframwork.anno.SLayoutId;
import lee.bottle.lib.singlepageframwork.base.SActivity;
import lee.bottle.lib.toolset.log.Build;
import lee.bottle.lib.toolset.log.ILogHandler;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.PermissionApply;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 主入口
 */
public class SingleActivity extends SActivity implements PermissionApply.Callback{

    //权限数组
    private String[] permissionArray = new String[]{
            Manifest.permission.CAMERA, // 相机和闪光灯
            Manifest.permission.READ_CONTACTS,//读取联系人
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
            Manifest.permission.CALL_PHONE // 拨号

    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    @SLayoutId("content")
    private FrameLayout layout;

    private TextView tv;

    public void setLogger(final String message){
        tv.post(new Runnable() {
            @Override
            public void run() {
                tv.setText(tv.getText() + "\n" +message);
            }
        });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        layout = findViewById(R.id.container);
        tv = findViewById(R.id.logs);
        LLog.addLogHandler(new ILogHandler() {
            @Override
            public void handle(String tag, Build build, String content) throws Exception {
                setLogger(content);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionApply != null) permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LLog.print("activity","onActivityResult");
        if (permissionApply != null) permissionApply.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LLog.print("onSaveInstanceState","onSaveInstanceState(Bundle outState)");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        LLog.print("onSaveInstanceState","onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState)");
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void onPermissionsGranted() {
        LLog.print("授权成功");
        //授权成功
//        getSFOPage().skip("content","web");//跳转到web页面
    }

    @Override
    public void onPowerIgnoreGranted() {
        //忽略电源回调
    }

    @Override
    protected void onInitResume() {
        super.onInitResume();
        if (!isSysRecovery()){
            permissionApply.permissionCheck();
            getSFOPage().skip("content","web");//跳转到web页面
        }
    }

}
