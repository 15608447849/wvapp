package com.bottle.wvapp.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bottle.wvapp.R;

import java.util.ArrayList;

import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2020/6/15.
 * email: 793065165@qq.com
 */
public class WebActivity extends AppCompatActivity {

    private IWebViewInit iWebViewInit;

    private Button linkBtn;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        LLog.print(this+" , onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        View view = findViewById(R.id.fl_web);
        linkBtn = findViewById(R.id.btn_link);
        linkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toSingleActivity();
            }
        });
        iWebViewInit = IWebViewInit.createIWebView("lee.bottle.lib.toolset.web.SysCore",this, (ViewGroup) view,null);
        openWeb();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        LLog.print(this+" , onNewIntent()");
        openWeb();
    }

    private String loadUrl = null;
    ArrayList<String> paramList = new ArrayList<>();

    private void openWeb() {

        if (iWebViewInit == null) return;

        Intent i = getIntent();
        String _loadUrl = i.getStringExtra("loadUrl");
        String linkPath = i.getStringExtra("linkPath");
        paramList.clear();

        if (linkPath!=null){
            paramList.add(linkPath);
            linkBtn.setVisibility(View.VISIBLE);
        }else{
            linkBtn.setVisibility(View.GONE);
        }

        if (loadUrl == null || !loadUrl.equals(_loadUrl)){
            loadUrl = _loadUrl;
            LLog.print("WEB ACTIVITY 加载: "+ loadUrl);
            iWebViewInit.getProxy().loadUrl(loadUrl);
        }
    }

    @Override
    public void onBackPressed() {
       //返回activity
        boolean isBackSuccess = false;
        if (iWebViewInit!=null){
            isBackSuccess = iWebViewInit.onBackPressed();
        }
        if (!isBackSuccess){
            super.onBackPressed();
        }
    }

    private void toSingleActivity(){
        Intent intent = new Intent(this, SingleActivity.class);
        if (paramList!=null){
            intent.putStringArrayListExtra("notify_param",paramList);
        }
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        if (iWebViewInit!=null){
            iWebViewInit.clear(false);
            iWebViewInit = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (iWebViewInit!=null) iWebViewInit.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode, resultCode, data);
    }
}
