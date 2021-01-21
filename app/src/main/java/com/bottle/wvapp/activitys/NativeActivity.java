package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.telephony.SmsMessage;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.jsprovider.HttpServerImp;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.tool.WebResourceCache;

import java.io.File;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.web.JSUtils;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 主入口
 */
public class NativeActivity extends BaseActivity implements PermissionApply.Callback, DownloadListener {
    //权限数组
    private String[] permissionArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.READ_CONTACTS,//读取联系人
            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
            Manifest.permission.CALL_PHONE, // 拨号
            Manifest.permission.RECEIVE_SMS, //接收短信
            Manifest.permission.READ_SMS // 读取短信
    };

    //短信广播接收者
    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();//通过getExtras()方法获取短信内容
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");//根据pdus关键字获取短信字节数组，数组内的每个元素都是一条短信
                if (pdus!=null){
                    String format = intent.getStringExtra("format");
                    for (Object object : pdus) {

                        SmsMessage message;//将字节数组转化为Message对象
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            message = SmsMessage.createFromPdu((byte[])object,format);
                        }else{
                            message = SmsMessage.createFromPdu((byte[])object);
                        }
                        String sender = message.getOriginatingAddress();//获取短信手机号
                        String body = message.getMessageBody();
                        LLog.print("SMS广播接收:" + sender+">> 短信内容: "+ body);
                        NativeServerImp.sendSmsCodeToJS(body);
                    }
                }

            }
        }
    };

    //权限申请
    private PermissionApply permissionApply =  new PermissionApply(this,permissionArray,this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ApplicationAbs.putApplicationObject(new Timer());
        NativeServerImp.bindActivity(this);
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native);
        launchInit();
        /* web资源缓存处理 */
        JSUtils.setWebResourceRequestI(new WebResourceCache());
        /* web页面加载层 */
        final FrameLayout layout = findViewById(R.id.container);
        // 加载页面
        loadWebMainPage(BuildConfig._WEB_HOME_URL,layout,this);
        //加载短信接受广播
        bindSMSBroad();
    }

    private void bindSMSBroad() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED" );
        registerReceiver(receiver,filter);//注册广播接收器
    }

    private void unbindSMSBroad() {
        unregisterReceiver(receiver);//解绑广播接收器
    }

    private void launchInit(){
        //设置首次加载处理
        JSUtils.setWebProgressI(new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(String url,int current,boolean isForce) {
                if (current>=100){
                    if (isForce){
                        current = 0;
                    }else{
                        current = 500;
                    }
                    delayTimeStop(current);
                }
            }
        });

    }

    private TimerTask stopLaunchImageShowWebTimeTask = null;

    private void delayTimeStop(int delay){
        if (stopLaunchImageShowWebTimeTask!=null){
            stopLaunchImageShowWebTimeTask.cancel();
        }

        stopLaunchImageShowWebTimeTask = new TimerTask() {
            @Override
            public void run() {
                stopLaunch();
            }
        };

        //延时指定秒后销毁
        Timer timer =  ApplicationAbs.getApplicationObject(Timer.class);
        if (timer == null) return;
        timer.schedule(stopLaunchImageShowWebTimeTask,delay);
    }

    private void stopLaunch(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 设置activity不是全屏
                getWindow().getDecorView().setBackgroundResource(0);
                getWindow().getDecorView().setSystemUiVisibility(0);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

                //展开webview 容器层
                FrameLayout frameLayou = findViewById(R.id.container);
                frameLayou.setLayoutParams(
                        new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                loadWebPageProgress();
            }
        });
    }

    private void loadWebPageProgress() {
        //设置页面加载滚动条
        JSUtils.setWebProgressI(new JSUtils.WebProgressI() {
            @Override
            public void updateProgress(String url,final int current,boolean isForce) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /* 进度条 */
                        final ProgressBar progressBar = findViewById(R.id.progress_bar);
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
        NativeServerImp.iBridgeImp.onActivityResult(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    //授权成功回调
    @Override
    public void onPermissionsGranted() {
        ApplicationAbs.setApplicationDir(new File(Environment.getExternalStorageDirectory(), "1k.一块医药"));
    }

    //忽略电源回调
    @Override
    public void onPowerIgnoreGranted() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionApply.permissionCheck(); //权限检测
        Intent intent = getIntent();
        if (intent != null){
            ArrayList<String> list = intent.getStringArrayListExtra("notify_param");
            if (list!=null){
                intent.removeExtra("notify_param");
                NativeServerImp.notifyEntryToJs(list.get(0)); //跳转到指定页面
            }
        }
    }

    @Override
    protected void onDestroy() {
        unbindSMSBroad();
        Timer timer = ApplicationAbs.getApplicationObject(Timer.class);
        if (timer!=null){
            ApplicationAbs.delApplicationObject(Timer.class);
            timer.cancel();
        }
//        ApplicationAbs.delApplicationObject(Timer.class);
        super.onDestroy();
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {
        File rootDir = ApplicationAbs.getApplicationDIR("下载列表");
        if (rootDir == null) return;
        final String fileName = url.substring(url.lastIndexOf("/")+1);
        final File file = new File(rootDir,fileName);
        String msg =
                "文 件 名:\t"+fileName+"\n" +
                "文件大小:\t"+ (FileUtils.byteLength2StringShow(contentLength))+"\n" +
                "存储目录:\t"+rootDir.getAbsolutePath()+"\n\t";
        if (file.exists()){
            msg += "存在同名文件,下载将进行覆盖";
        }
        //下载弹窗
        DialogUtil.build(this,
                "是否立即下载",
                msg,
                R.drawable.ic_update_version,
                "其他应用打开",
                "进入后台下载",
                null,
                0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if (which == DialogInterface.BUTTON_POSITIVE){
                            //其他应用打开
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.addCategory(Intent.CATEGORY_BROWSABLE);
                            intent.setData(Uri.parse(url));
                            startActivity(intent);
                        }

                        if (which == DialogInterface.BUTTON_NEGATIVE){
                            //加入队列下载
                            HttpServerImp.addDownloadFileToQueue(new HttpServerImp.DownloadTask(url,file.getPath(),new HttpUtil.CallbackAbs(){

                                @Override
                                public void onResult(HttpUtil.Response response) {
                                    File storeFile = response.getData();
                                    final String resMsg = storeFile + " 下载完成";
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AppUtils.toast(NativeActivity.this,resMsg);
                                        }
                                    });
                                }
                            }));

                        }
                    }
                });

    }

}
