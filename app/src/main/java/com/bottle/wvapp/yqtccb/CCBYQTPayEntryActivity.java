package com.bottle.wvapp.yqtccb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bottle.wvapp.activitys.NativeActivity;

/**
 * Created by Leeping on 2019/7/1.
 * email: 793065165@qq.com
 */
public class CCBYQTPayEntryActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, NativeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        finish();
    }
}
