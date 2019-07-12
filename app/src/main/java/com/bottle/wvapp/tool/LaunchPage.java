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
import android.view.WindowManager;
import android.widget.ImageView;

import com.bottle.wvapp.R;

import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

/**
 * Created by Leeping on 2019/7/12.
 * email: 793065165@qq.com
 */
public class LaunchPage {

    private static boolean isLaunch = false;

    static{
        JSUtils.openCallback = new JSUtils.WebPageOneOpen() {
            @Override
            public void pageFinish() {
                isLaunch = true;
                stop();
            }
        };
    }

    private static Activity showActivity;

    private static ViewGroup rootView;

    private static ImageView iv;


    public static void start(Activity activity){
        if (!AppUtils.checkUIThread()) return;
        if (isLaunch) return;
        showActivity = activity;
        View decorView = showActivity.getWindow().getDecorView();
        rootView = decorView.findViewById(android.R.id.content);
//        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
//        decorView.setSystemUiVisibility(uiOptions);
        showActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setAnimation();
        //添加启动图片image view
        iv = new ImageView(showActivity);
        iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        rootView.addView(iv);
        scaleImage(R.drawable.launch);
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
        rootView.setLayoutTransition(mLayoutTransition);

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



    public static void stop(){
        if (!AppUtils.checkUIThread()) return;
        if (isLaunch && rootView!=null && iv!=null){
            showActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            releaseImageView(iv);
            rootView.removeView(iv);
            rootView.setLayoutAnimationListener(null);
            rootView = null;
            iv = null;
            showActivity = null;
        }
    }

    private static void scaleImage(int drawableResId) {
        final Point outSize = new Point();
        showActivity.getWindow().getWindowManager().getDefaultDisplay().getSize(outSize);
        LLog.print("屏幕大小: w = "+ outSize.x +" h = " +outSize.y );

        final Bitmap resourceBitmap = BitmapFactory.decodeResource(showActivity.getResources(), drawableResId);
        LLog.print("图片大小: w = "+ resourceBitmap.getWidth() +" h = " + resourceBitmap.getHeight() );

        int w = resourceBitmap.getWidth();
        int h = resourceBitmap.getHeight();
        float scaleW = outSize.x  * 1.0f / resourceBitmap.getWidth();
        float scaleH = outSize.y * 1.0f / resourceBitmap.getHeight();
        LLog.print("比例: w = "+ scaleW +" h = " + scaleH);

        Matrix matrix = new Matrix();
        matrix.postScale(scaleW, scaleH); // 长和宽放大缩小的比例
        Bitmap finallyBitmap = Bitmap.createBitmap(resourceBitmap, 0, 0, w, h, matrix, true);
        resourceBitmap.recycle();
        //设置图片显示
        iv.setBackgroundDrawable(new BitmapDrawable(showActivity.getResources(), finallyBitmap));
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
