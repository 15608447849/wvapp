package com.bottle.wvapp.tool;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.bottle.wvapp.R;

/**
 * Created by Leeping on 2020/4/22.
 * email: 793065165@qq.com
 */
public class UploadProgressWindow {
    private static AlertDialog mProgress;
    private static TextView mProgress_tv;
    private static boolean isHind = false;

    public static synchronized void progressBarCircleDialogUpdate(final Activity activity, final String text) {
//        LLog.print("更新进度对话框 " + mProgress_tv+" - "+ text);
        if (activity == null || isHind) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgress == null){
                    AlertDialog.Builder progressBuild = new AlertDialog.Builder(activity, AlertDialog.THEME_HOLO_LIGHT);
                    LayoutInflater inflater = LayoutInflater.from(activity);
                    @SuppressLint("InflateParams")
                    View view = inflater.inflate(R.layout.up_progress, null);
                    mProgress_tv = view.findViewById(R.id.progress_tv);
                    progressBuild.setView(view);
                    progressBuild.setPositiveButton("隐藏", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isHind = true;
                            mProgress = null;
                            mProgress_tv = null;

                        }
                    });
                    mProgress = progressBuild.create();
                    mProgress.setCanceledOnTouchOutside(false);
                    mProgress.setCancelable(false);
                    mProgress.show();
                }

                if (mProgress_tv!=null){
                    mProgress_tv.setText(text);
                }
            }
        });
    }

    public synchronized static void progressBarCircleDialogStop(final Activity activity) {
//        LLog.print("关闭进度对话框" + mProgress);
        if (activity == null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                isHind = false;
                if (mProgress_tv!=null) mProgress_tv = null;
                if (mProgress!=null) {
                    mProgress.cancel();
                    mProgress.dismiss();
                    mProgress = null;
                }
            }
        });

    }
}
