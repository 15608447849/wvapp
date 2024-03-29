package com.bottle.wvapp.activitys;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import lee.bottle.lib.toolset.os.ApplicationDevInfo;
import com.bottle.wvapp.app.GlobalMainWebView;
import com.bottle.wvapp.app.WebApplication;
import com.bottle.wvapp.jsprovider.NativeActivityInterface;
import com.bottle.wvapp.jsprovider.NativeActivityInterfaceDefault;
import com.bottle.wvapp.jsprovider.NativeJSInterface;
import com.bottle.wvapp.jsprovider.NativeServerImp;
import com.bottle.wvapp.uptver.UpdateVersionServerImp;
import java.util.ArrayList;
import java.util.List;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.BaseActivity;
import lee.bottle.lib.toolset.os.PermissionApply;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.IntentUtils;
import lee.bottle.lib.toolset.util.DialogUtils;
import lee.bottle.lib.webh5.SysWebView;
import lee.bottle.lib.webh5.interfaces.LoadErrorI;
import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;
import static com.bottle.wvapp.app.GlobalMainWebView.getNativeServerImp;
import static com.bottle.wvapp.beans.BusinessData.getCurrentDevCompanyID;
import static com.bottle.wvapp.services.IMServiceSDK26FontServerUtil.startIMService;
import static lee.bottle.lib.toolset.util.AppUtils.getClipboardContent;
import static lee.bottle.lib.toolset.util.AppUtils.setClipboardContent;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 主入口
 */


public class NativeActivity extends BaseActivity implements PermissionApply.Callback {

    //权限数组
    private static final String[] permissionArray = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写sd卡
//            Manifest.permission.MANAGE_EXTERNAL_STORAGE, // 写sd卡-android11
//            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.READ_CONTACTS,//读取联系人
//            Manifest.permission.READ_PHONE_STATE, // 获取手机状态
//            Manifest.permission.CALL_PHONE // 拨号
    };

    //权限申请
    private final PermissionApply permissionApply =  new PermissionApply(this, permissionArray,this);
    /* web页面容器层 */
    private FrameLayout frameLayout;
    /* webView实现 */
    private SysWebView webView = null;
    /* 权限最后检查时间 */
    private long lastPermissionTime;
    /*******************************************************************************************************************************************/

    public static final String ACTION_BROADCAST_RECEIVE = "NATIVE_ACTION_RECEIVE_MESSAGE";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            LLog.print("广播接收: "+ intent);
            intentHandler(intent);
        }
    };
    private void registerBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_BROADCAST_RECEIVE);
        registerReceiver(broadcastReceiver,intentFilter);
    }
    private void unregisterBroadcast(){
        unregisterReceiver(broadcastReceiver);
    }

    /*
     * ****************************************************************************************************************************************
     * ****************************************************************************************************************************************
     * */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LLog.print(this+" ************************************ onCreate" );
        super.onCreate(savedInstanceState);
        // 注册广播
        registerBroadcast();
        // 触发应用更新
        UpdateVersionServerImp.checkVersion(this,false);

        // 创建图层
        frameLayout = new FrameLayout(this);
        // 设置大小
        if (GlobalMainWebView.isIsUrlLoading() && GlobalMainWebView.getNativeServerImp().isImServerAcceptStart){
            getWindow().getDecorView().setBackgroundResource(0); // 移除背景资源
            getWindow().getDecorView().setSystemUiVisibility(0);// 清空UI选项
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// 清除全屏
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));// 填满屏幕
        }else{
            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(0,0));
        }

        // 设置图层
        setContentView(frameLayout);

        webView = GlobalMainWebView.getInstance();

//         activity -业务服务 -js互操作对象 三者关联
        NativeActivityInterface nativeActivityInterfaceImp = createNativeActivityInterface(GlobalMainWebView.getNativeJSInterface());
        GlobalMainWebView.getNativeServerImp().setNativeActivityInterface( nativeActivityInterfaceImp );

        // web页面错误监听
        webView.loadErrorI = createWebViewErrorHandler();

        // 绑定webview
        webView.bind(this,frameLayout);

        // 加载首页
        GlobalMainWebView.open(_WEB_HOME_URL);
    }



    @Override
    protected void onResume() {
        LLog.print(this+"************************************ onResume" );
        super.onResume();
        try {
            // 尝试进行权限检查
            checkPermissionQuery();
            // 获取剪切板分享内容
            accessSharedContent();
            // 处理intent信息
            intentHandler(getIntent());
            // 打开IM
            if (GlobalMainWebView.getNativeServerImp().isImServerAcceptStart) {
                startIMService(this);
            }
        } catch (Exception e) {
            LLog.error(e);
        }

    }

    private void checkPermissionQuery() {
        if (  System.currentTimeMillis() - lastPermissionTime > 3 * 60 *1000L){
            lastPermissionTime = System.currentTimeMillis();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 权限检测
                    permissionQuery();
                }
            },30 * 1000);

        }
    }

    @Override
    protected void onPause() {
        LLog.print(this+"************************************ onPause" );
        super.onPause();
        overridePendingTransition(0, 0);
    }
    @Override
    protected void onDestroy() {
        LLog.print(this+"************************************ onDestroy" );
        unregisterBroadcast();
        GlobalMainWebView.getNativeServerImp().setNativeActivityInterface(null);
        if (webView != null){
            webView.loadErrorI=null;
            webView.unbind();
            webView =null;
        }

        super.onDestroy();
    }

    /* 其他activity退出返回响应结果 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        LLog.print("onActivityResult " +  requestCode +" "+ resultCode);
        if (permissionApply != null) permissionApply.onActivityResult(requestCode, resultCode, data);
        if (webView !=null) webView.onActivityResultHandle(requestCode,resultCode,data);
        super.onActivityResult(requestCode,resultCode,data);
    }

    // 捕获返回键 处理
    @Override
    public void onBackPressed() {
        if (webView.onBackPressed()) return;
        super.onBackPressed();
    }

    /*
    * ****************************************************************************************************************************************
    * ****************************************************************************************************************************************
    * */

    /*  webview 加载错误处理 */
    private LoadErrorI createWebViewErrorHandler() {
    return new LoadErrorI() {
        @Override
        public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
            Uri uri =  webResourceRequest.getUrl();
            if (uri == null) return;

            LLog.print("打开网页错误: " + uri);

            if (!uri.toString().startsWith(_WEB_HOME_URL)) return;
            AppUtils.toastShort(NativeActivity.this,"网络不可用" );

            if (uri.toString().endsWith("css") || uri.toString().endsWith("js") || uri.toString().endsWith("ico")) return;


            if (!AppUtils.isNetworkAvailable(NativeActivity.this)){
                String str = "file:///android_asset/error.html?reloadUrl="+ _WEB_HOME_URL;
                webView.loadUrl(str);
            }

        }
    };
    }
    /* 创建native服务实现 */
    private NativeActivityInterface createNativeActivityInterface(NativeJSInterface nativeJSInterface) {
        return new NativeActivityInterfaceDefault(this,this.webView,nativeJSInterface){

            @Override
            public void connectIceIM() {
                // 允许连接长连接
                startIMService(NativeActivity.this);
            }

            @Override
            public void onJSPageInitialization() {
                LLog.print("页面初始化完成...");
                // 打开图层
                visibleWebViewLayout();
            }

            @Override
            public void onIndexPageShowBefore(){
                LLog.print("广告页播放完成...");
                // 移除全屏
                removeFullScreen();
                // 获取一次剪切板分享内容
                accessSharedContent();
            }

        };
    }
    /*  web页面元素初始化完成,显示web view */
    private void visibleWebViewLayout() {
        LLog.print(this +" ************************************ 展开webView的layout层");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setBackgroundResource(0); // 移除背景资源

                ViewGroup.LayoutParams layoutParams = frameLayout.getLayoutParams();
                if (!(layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT
                        && layoutParams.height == ViewGroup.LayoutParams.MATCH_PARENT)){
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    frameLayout.setLayoutParams(layoutParams);
                }
            }
        });

    }
    /* 移除全屏 */
    private void removeFullScreen() {
        LLog.print(this +" ************************************ 移除全屏效果");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWindow().getDecorView().setBackgroundResource(0); // 移除背景资源
                getWindow().getDecorView().setSystemUiVisibility(0);// 清空UI选项
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// 清除全屏

                // 隐藏底部导航栏
                //  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

            }
        });
    }
    /* 授权请求 */
    private void permissionQuery(){

        final int compId = getCurrentDevCompanyID(true, WebApplication.iceClient);
        if (compId <= 0) return;
        boolean permission = permissionApply.permissionCheck(); //权限检测
        if (permission) permissionApply.requestNotify();// 请求通知栏

    }
    /* 权限审核回调 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (permissionApply != null) permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    /* 权限申请完成 */
    private void authorizationCompletion(){
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                // 转移文件夹(兼容动作)
                ApplicationDevInfo.transferDictCompatible(getApplication());

                // 刷新本地企业信息
                final int compId = getCurrentDevCompanyID(true, WebApplication.iceClient);
                LLog.print("授权成功 当前设备已登录的企业编码: "+ compId);
                // 重新加载页面
                if (compId > 0) {
                    GlobalMainWebView.getNativeServerImp().userChangeByJS(); // JS触发用户信息更新
                    GlobalMainWebView.getNativeServerImp().notifyEntryToJs("/home");
                }
            }
        });

    }
    /* 授权成功回调*/
    @Override
    public void onPermissionsGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissionApply.requestNotify();
            authorizationCompletion();
        }else {
            //android11 文件授权需要进入系统授权页并获取返回结果
            permissionApply.sdk30_isExternalStorageManager();
        }

    }
    /* android11文件存储授权 */
    @Override
    public void onSDK30FileStorageRequestResult(boolean isGrant) {
        if (isGrant){
            authorizationCompletion();
            permissionApply.requestNotify();
        }
    }

    /* 忽略电源回调 */
    @Override
    public void onPowerIgnoreGranted() { }

    /* 处理intent的信息 */
    private void intentHandler(Intent intent) {
        final NativeServerImp nativeServerImp = getNativeServerImp();
        if (intent != null && nativeServerImp!=null){

            Bundle bundle = intent.getExtras();
            if (bundle != null){
                for (String key: bundle.keySet())
                {
                    LLog.print("Intent Extra Data >> " + key + " = " +bundle.getString(key));
                }
            }

            // 通知栏点击进入
            IntentUtils.getStringArrayListExtra(intent, "notify_param", new IntentUtils.ListCallback() {
                @Override
                public void callback(List<String> list) {
                    nativeServerImp.notifyEntryToJs(list.get(0)); //跳转到指定页面
                }
            });

            // 长连接 强制退出
            IntentUtils.getStringExtra(intent, "forceLogout", new IntentUtils.StringCallback() {
                @Override
                public void callback(String v) {
                    if (Boolean.parseBoolean(v)) nativeServerImp.forceLogout();
                }
            });

            // 长连接 支付结果
            IntentUtils.getStringExtra(intent, "pushPaySuccessMessageToJs", new IntentUtils.StringCallback() {
                @Override
                public void callback(String v) {
                    nativeServerImp.pushPaySuccessMessageToJs(v);
                }
            });

            // 长连接 推送消息
            IntentUtils.getStringExtra(intent, "pushMessageToJs", new IntentUtils.StringCallback() {
                @Override
                public void callback(String v) {
                    nativeServerImp.pushMessageToJs(v);
                }
            });

            // 长连接 弹窗提醒
            IntentUtils.getStringExtra(intent, "alertTipWindow", new IntentUtils.StringCallback() {
                @Override
                public void callback(String v) {
                    nativeServerImp.alertTipWindow(v);
                }
            });
        }
    }

    /* 获取分享内容 */
    private void accessSharedContent() {
        // LLog.print(this+ " 获取剪切板内容");
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String content = getClipboardContent(NativeActivity.this);
                if (content.endsWith("-onekdrug")){
                    GlobalMainWebView.getNativeServerImp().enterApp(content);
                    setClipboardContent(getApplicationContext(),"");
                }
            }
        },500);
    }


}
