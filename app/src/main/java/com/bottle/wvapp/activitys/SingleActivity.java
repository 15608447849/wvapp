package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.singlepageframwork.anno.SLayoutId;
import lee.bottle.lib.singlepageframwork.base.SActivity;
import lee.bottle.lib.singlepageframwork.use.RegisterCentre;
import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.os.VideoView;

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

    /* fragment 容器*/
    @SLayoutId("content")
    private FrameLayout layout;

    private VideoView video;
    private View rLayout;
    private View passBtn;


    private boolean isLaunch = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single);
        layout = findViewById(R.id.container);
        launchPage();
    }

    private void launchPage() {
        video = findViewById(R.id.video);
        rLayout = findViewById(R.id.rl);
        passBtn = findViewById(R.id.rl_btn_pass);
        setAnimation();
        passBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.clearAnimation();
                closeAdVideo();
            }
        });

        JSUtils.openCallback = new JSUtils.WebPageOneOpen() {
            @Override
            public void pageFinish() {
                isLaunch = true;
                closeAdVideo();
                //显示跳过按钮
//                rLayout.setVisibility(View.VISIBLE);

            }
        };
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        String uri = "android.resource://" + getPackageName() + "/" + R.raw.launch;
        Uri path = Uri.parse(uri);
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                closeAdVideo();
            }
        });
        video.setVisibility(View.VISIBLE);
        video.setVideoURI(path,false);
    }

    private void setAnimation() {
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        passBtn.startAnimation(animation);
    }

    private void closeAdVideo() {
        if (isLaunch){
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            video.pause();
            video.stopPlayback();
            video.setVisibility(View.GONE);
            rLayout.setVisibility(View.GONE);
        }
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
        closeAdVideo();
    }

    //界面显示
    @Override
    protected void onInitResume() {
        LLog.print("onInitResume");
        super.onInitResume();
        if (!isSysRecovery()){
            initApp();
            permissionApply.permissionCheck(); //权限检测
        }
    }

    //初始化应用
    private void initApp() {
        mHandler.io(new Runnable() {
            @Override
            public void run() {
                //初始化页面
                RegisterCentre.register(NativeServerImp.dynamicPageInformation());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NativeServerImp.ServerConfig c = NativeServerImp.config;
                        //加载页面
                        getSFOPage().skip(c.page.container,c.page.name);//跳转到web页面
                    }
                });
            }
        });
    }



}
