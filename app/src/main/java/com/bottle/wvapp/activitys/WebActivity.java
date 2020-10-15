package com.bottle.wvapp.activitys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bottle.wvapp.R;

import java.util.ArrayList;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.ObjectRefUtil;

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
        try {
            iWebViewInit = (IWebViewInit)ObjectRefUtil.createObject("lee.bottle.lib.toolset.web.SysCore",
                    new Class[]{Context.class, ViewGroup.class, IBridgeImp.class},
                    this, view,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            iWebViewInit.clear();
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
            iWebViewInit.clear();
            iWebViewInit = null;
        }
        super.onDestroy();
    }
}
