package com.bottom.wvapp.activitys;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import lee.bottle.lib.toolset.os.PermissionApply;
import com.bottom.wvapp.R;

import lee.bottle.lib.singlepageframwork.anno.SLayoutId;
import lee.bottle.lib.singlepageframwork.base.SActivity;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */

public class SingleActivity extends SActivity implements PermissionApply.Callback{

    //权限数组
    private String[] permissionArray = new String[]{
            Manifest.permission.CAMERA, // 相机和闪光灯
            Manifest.permission.READ_CONTACTS,//读取联系人
            Manifest.permission.WRITE_EXTERNAL_STORAGE // 写sd卡
    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    @SLayoutId("content")
    private FrameLayout layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        layout = findViewById(R.id.container);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionApply != null) permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (permissionApply != null) permissionApply.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPermissionsGranted() {
        //授权成功
        getSFOPage().skip("content","web");//跳转到web页面
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
        }
    }


}
