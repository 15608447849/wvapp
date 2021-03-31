package com.bottle.wvapp.activitys;

import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.bottle.wvapp.R;
import com.google.android.material.snackbar.Snackbar;

import lee.bottle.lib.toolset.util.AppUtils;

public class ErrorActivity extends AppCompatActivity {

    private boolean isFlag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;

        getWindow().getDecorView().setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_error);

        // 网络不可用的情况下进入,加载网络变化监听线程
        if (!AppUtils.isNetworkAvailable(ErrorActivity.this)){
            new Thread(){
                @Override
                public void run() {
                    // 加载错误页面
                    View rootView = ErrorActivity.this.findViewById(R.id.layout_root);
                    final Snackbar snackbar = Snackbar.make(
                            rootView,
                            "网络连接不可用", 30000)
                            .setAction("设置", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
                                    ErrorActivity.this.startActivity(intent);
                                }
                            });

                    View snackbarView = snackbar.getView();
                    ViewGroup.LayoutParams params = snackbarView.getLayoutParams();
                    CoordinatorLayout.LayoutParams layoutParams = new CoordinatorLayout.LayoutParams(params.width, params.height);
                    layoutParams.gravity = Gravity.TOP;
                    snackbarView.setLayoutParams(layoutParams);

                    snackbar.show();

                    int index = 0;
                    while (isFlag && index<100){
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (AppUtils.isNetworkAvailable(ErrorActivity.this)){
                           retry();
                        }
                        index++;

                    }
                }
            }.start();
        }

        findViewById(R.id.iv_gif).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retry();
            }
        });

        String errorText = getIntent().getStringExtra("errorText");

        if (errorText!=null){

            ((TextView)findViewById(R.id.tv_cause)).setText(errorText);

            final View view = findViewById(R.id.card_text);

            TranslateAnimation anim = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f,
                    Animation.RELATIVE_TO_SELF, 2.0f,
                    Animation.RELATIVE_TO_SELF, 0.0f);

            anim.setDuration(5000);

            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    view.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.startAnimation(anim);
            view.setVisibility(View.VISIBLE);

        }

    }

    private void retry() {

        Intent intent = new Intent(ErrorActivity.this,NativeActivity.class);
        intent.putExtra("reload",getIntent().getBooleanExtra("reload",false));

        ErrorActivity.this.startActivity(intent);
        isFlag = false;
        overridePendingTransition(0, 0);
        finish();
    }
}