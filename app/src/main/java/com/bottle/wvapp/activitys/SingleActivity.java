package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.R;
import com.bottle.wvapp.tool.LaunchPage;

import lee.bottle.lib.singlepageframwork.anno.SLayoutId;
import lee.bottle.lib.singlepageframwork.base.SActivity;
import lee.bottle.lib.toolset.jsbridge.JSUtils;
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.READ_CONTACTS,//读取联系人
            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
            Manifest.permission.CALL_PHONE // 拨号
    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    /* 进度条 */
    private ProgressBar progressBar;

    /* fragment 容器*/
    @SLayoutId("content")
    private FrameLayout layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        progressBar = findViewById(R.id.progress_bar);

        layout = findViewById(R.id.container);
        LaunchPage.start(this, new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(final int current) {
                SingleActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar!=null){
                            progressBar.setProgress(current);
                            if (current==100){
                                progressBar.setVisibility(View.GONE);
                            }else{
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
            }
        });

    }



    /* 权限审核回调 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionApply != null) permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (permissionApply != null) permissionApply.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        LLog.print("onSaveInstanceState","onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState)");
        super.onSaveInstanceState(outState, outPersistentState);
    }

    //授权成功回调
    @Override
    public void onPermissionsGranted() {
    }

    //忽略电源回调
    @Override
    public void onPowerIgnoreGranted() {
    }

    @Override
    protected void onResume() {
        super.onResume();
        LaunchPage.stop();
    }

    //界面显示
    @Override
    protected void onInitResume() {
        super.onInitResume();
        if (!isSysRecovery()){
            initApp();
            permissionApply.permissionCheck(); //权限检测
        }
    }

    //初始化应用
    private void initApp() {
        //加载页面
        getSFOPage().skip("content","web");//跳转到web页面
    }

}
