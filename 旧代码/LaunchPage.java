package com.bottle.wvapp.tool;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import lee.bottle.lib.toolset.http.HttpRequest;
import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;

// 强制登出提示框
//        final BaseActivity activity = activityRef.get();
//        if (activity == null) return;
//
//        activity.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //刷新首页
//                activity.reloadWebMainPage();
//
//                DialogUtil.build(activity,
//                        "安全提示",
//                        "尊敬的用户∶\n\t\t\t\t您的账号已在其他设备登录,如非本人操作请联系您的商务经理或致电客服热线。",
//                        R.drawable.ic_update_version,
//                        "确定",
//                        null,
//                        null,
//                        0,
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//
//            }
//        });

/**
 * Created by Leeping on 2019/7/12.
 * email: 793065165@qq.com
 */
public class LaunchPage{
    private static long executeStartTime;
    private static Activity showActivity;

//    private static ViewGroup rootView;

    private static ImageView iv;

    public static void start(Activity activity,final JSUtils.WebProgressI webProgressI){
        if (showActivity!=null) return;
        executeStartTime = System.currentTimeMillis();
        showActivity = activity;
//        iv = showActivity.findViewById(R.id.iv_launch);

        JSUtils.setWebProgressI(new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(int current) {
                if (current>=100){
                    //设置页面加载滚动条
                    JSUtils.setWebProgressI(webProgressI);
                    LLog.print("加载首页结束,已耗时 : "+ ApplicationAbs.runtimeStr());
                    delayTimeStop(1000);
                }
            }
        });

        /*IOUtils.run(new Runnable() {
            @Override
            public void run() {
                loadImageUrl(BuildConfig._LAUNCH_IMAGE_URL);
            }
        });*/
        delayTimeStop(30 * 1000);
    }

    private static void delayTimeStop(int delay){
        //最多十秒后销毁
        new Timer(true)
                .schedule(new TimerTask() {
            @Override
            public void run() {
                LaunchPage.stop();
            }
        },delay);
    }

    private static void loadImageUrl(String url) {
        //判断本地文件是否存在, 存在使用本地文件,同时更新文件,不存在 使用链接
        boolean useLocal = false;
        if (showActivity!=null){
            File file = new File(showActivity.getCacheDir(),"launch.png");
            if (file.exists() && file.length()>0){
                try(FileInputStream imageIn = new FileInputStream(file)){
                    imageInputStreamLoad(imageIn);
                    useLocal = true;
                }catch (Exception ignored){
                }
            }else{
                new HttpRequest().download(url,file);
            }
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)new URL(url).openConnection();
            conn.setDoInput(true);
            try(InputStream imageIn = conn.getInputStream() ){
                if (imageIn!=null) {
                    if (!useLocal){
                        imageInputStreamLoad(imageIn);
                    }else{
                        imageInputStreamWriteLocal(imageIn);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (conn != null){
                conn.disconnect();
            }
        }
    }

    private static void imageInputStreamWriteLocal(InputStream imageIn) {
        if (showActivity!=null){
            File file = new File(showActivity.getCacheDir(),"launch.png");
            LLog.print("使用本地启动页 写入");
            try(FileOutputStream fos = new FileOutputStream(file)){
                int len ;
                byte[] bytes = new byte[1024];
                while ( (len = imageIn.read(bytes)) > 0){
                    fos.write(bytes,0,len);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void imageInputStreamLoad(InputStream imageIn) {
        LLog.print("获取启动图成功 : "+ ApplicationAbs.runtimeStr());
        final Bitmap resourceBitmap = BitmapFactory.decodeStream(imageIn);
        if (showActivity!=null){
            showActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startLoadImage(resourceBitmap);
                }
            });
        }
    }

    private static void startLoadImage(Bitmap resourceBitmap) {
        if (showActivity == null || resourceBitmap == null) return;

//        rootView = showActivity.getWindow().getDecorView().findViewById(android.R.id.content);

//        setAnimation();
        //添加启动图片image view
//        iv = new ImageView(showActivity);
//        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        rootView.addView(iv);

        scaleImage(resourceBitmap);
    }

    private static void stop(){
        if (showActivity!=null){
            LLog.print("启动页 stop()执行,已耗时 : "+ (System.currentTimeMillis() - executeStartTime));

            final Activity activity = showActivity;
            showActivity = null;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (iv!=null){
//                        iv.setVisibility(View.GONE);
                    }
//                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//                    if (iv!=null && rootView!=null){
//                        releaseImageView(iv);
//                        rootView.removeView(iv);
//                        rootView.setLayoutAnimationListener(null);
//                        rootView = null;
//                        iv = null;
//                    }
                }
            });
        }
    }

    private static void setAnimation() {
        LayoutTransition mLayoutTransition = new LayoutTransition();
        mLayoutTransition.setAnimator(LayoutTransition.APPEARING, getAppearingAnimation());
        mLayoutTransition.setDuration(LayoutTransition.APPEARING, 200);
        mLayoutTransition.setStartDelay(LayoutTransition.APPEARING, 0);//源码中带有默认300毫秒的延时，需要移除，不然view添加效果不好！！

        mLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, getDisappearingAnimation());
        mLayoutTransition.setDuration(LayoutTransition.DISAPPEARING, 200);

        mLayoutTransition.setAnimator(LayoutTransition.CHANGE_APPEARING,getAppearingChangeAnimation());
        mLayoutTransition.setDuration(200);

        mLayoutTransition.setAnimator(LayoutTransition.CHANGE_DISAPPEARING,getDisappearingChangeAnimation());
        mLayoutTransition.setDuration(200);

        mLayoutTransition.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
        mLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);//源码中带有默认300毫秒的延时，需要移除，不然view添加效果不好！！
        mLayoutTransition.addTransitionListener(new LayoutTransition.TransitionListener() {
            @Override
            public void startTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {

            }
            @Override
            public void endTransition(LayoutTransition transition, ViewGroup container, View view, int transitionType) {

            }
        });
//        rootView.setLayoutTransition(mLayoutTransition);
    }

    private static Animator getAppearingAnimation() {
        AnimatorSet mSet = new AnimatorSet();
        mSet.playTogether(ObjectAnimator.ofFloat(null, "ScaleX", 2.0f, 1.0f),
                ObjectAnimator.ofFloat(null, "ScaleY", 2.0f, 1.0f),
                ObjectAnimator.ofFloat(null, "Alpha", 0.0f, 1.0f),
                ObjectAnimator.ofFloat(null,"translationX",400,0));
        return mSet;
    }

    private static Animator getDisappearingAnimation() {
        AnimatorSet mSet = new AnimatorSet();
        mSet.playTogether(ObjectAnimator.ofFloat(null, "ScaleX", 1.0f, 0f),
                ObjectAnimator.ofFloat(null, "ScaleY", 1.0f, 0f),
                ObjectAnimator.ofFloat(null, "Alpha", 1.0f, 0.0f),ObjectAnimator.ofFloat(null,"translationX",0,400));
        return mSet;
    }

    @SuppressLint("ObjectAnimatorBinding")
    private static Animator getDisappearingChangeAnimation(){
         PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", 0, 0);
        PropertyValuesHolder pvhTop = PropertyValuesHolder.ofInt("top", 0, 0);
        PropertyValuesHolder pvhRight = PropertyValuesHolder.ofInt("right", 0, 0);
        PropertyValuesHolder pvhBottom = PropertyValuesHolder.ofInt("bottom", 0, 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX",1.0f,0f,1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY",1.0f,0f,1.0f);
        PropertyValuesHolder rotate = PropertyValuesHolder.ofFloat("rotation",0,0,0);
        return ObjectAnimator.ofPropertyValuesHolder((Object)null,pvhLeft, pvhTop, pvhRight, pvhBottom,scaleX,scaleY,rotate);
    }
    @SuppressLint("ObjectAnimatorBinding")
    private static Animator getAppearingChangeAnimation(){
        PropertyValuesHolder pvhLeft = PropertyValuesHolder.ofInt("left", 0, 0);
        PropertyValuesHolder pvhTop = PropertyValuesHolder.ofInt("top", 0, 0);
        PropertyValuesHolder pvhRight = PropertyValuesHolder.ofInt("right", 0, 0);
        PropertyValuesHolder pvhBottom = PropertyValuesHolder.ofInt("bottom", 0, 0);
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX",1.0f,3f,1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY",1.0f,3f,1.0f);
        return ObjectAnimator.ofPropertyValuesHolder((Object)null,pvhLeft, pvhTop, pvhRight, pvhBottom,scaleX,scaleY);
    }

    private static void scaleImage(Bitmap resourceBitmap) {
        final Point outSize = new Point();
        showActivity.getWindow().getWindowManager().getDefaultDisplay().getSize(outSize);

        if (resourceBitmap!=null){
            int w = resourceBitmap.getWidth();
            int h = resourceBitmap.getHeight();
            float scaleW = outSize.x  * 1.0f / resourceBitmap.getWidth();
            float scaleH = outSize.y * 1.0f / resourceBitmap.getHeight();

            Matrix matrix = new Matrix();
            matrix.postScale(scaleW, scaleH); // 长和宽放大缩小的比例
            final Bitmap finallyBitmap = Bitmap.createBitmap(resourceBitmap, 0, 0, w, h, matrix, true);
            resourceBitmap.recycle();
            //设置图片显示
            iv.setBackgroundDrawable(new BitmapDrawable(showActivity.getResources(), finallyBitmap));
        }

    }

    private static void releaseImageView(ImageView iv) {
        if (iv == null) return;
        Drawable drawable = iv.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bitmap = bitmapDrawable.getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

}
