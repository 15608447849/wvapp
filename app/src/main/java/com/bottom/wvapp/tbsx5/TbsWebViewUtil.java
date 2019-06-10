package com.bottom.wvapp.tbsx5;

import android.annotation.SuppressLint;
import android.content.Context;
import android.webkit.WebSettings;

import com.tencent.smtt.export.external.extension.interfaces.IX5WebViewExtension;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;

import lee.bottle.lib.toolset.log.LLog;

import static lee.bottle.lib.toolset.util.AppUtils.getLocalFileByte;
import static lee.bottle.lib.toolset.util.ImageUtils.imageCompression;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class TbsWebViewUtil {
    public static void tbsInit(Context appContext) {
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean flag) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                LLog.print("X5 内核使用: "+ flag);
            }

            @Override
            public void onCoreInitFinished() {
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(appContext,  cb);

    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public static void initViewWeb(com.tencent.smtt.sdk.WebView webView, String name, Object jsBridge){
        Context context = webView.getContext();

        WebView.setWebContentsDebuggingEnabled(true);

        com.tencent.smtt.sdk.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);  //开启JavaScript支持
        settings.setDomStorageEnabled(true);//开启DOM Storage功能
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);//解决图片不显示
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);//设置js可以直接打开窗口，如window.open()，默认为false
        settings.setMediaPlaybackRequiresUserGesture(false);//播放音频，多媒体需要用户手动？设置为false为可自动播放
        settings.setGeolocationEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs (true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(webView.getContext().getCacheDir().getPath());

        //自适应页面
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.setWebChromeClient(new X5WebChromeClient());
        webView.setWebViewClient(new X5WebViewClient());

        //去除滚动条
        webView.setHorizontalFadingEdgeEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        IX5WebViewExtension extension = webView.getX5WebViewExtension();
        if (extension!=null) extension.setScrollBarFadingEnabled(false);


        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        webView.addJavascriptInterface(jsBridge, name);
    }

    //媒体文件加载
    public static WebResourceResponse mediaLoad(WebView view, WebResourceRequest request){
        String scheme = request.getUrl().getScheme();
        try {
            if ("image".equalsIgnoreCase(scheme)
                    || "audio".equalsIgnoreCase(scheme)
                    || "video".equalsIgnoreCase(scheme)){
                String path = request.getUrl().getPath();
                File file = new File(path);
                LLog.print("加载本地文件:" + path+" , " + file.exists());
                if (!file.exists()) throw new FileNotFoundException(path);
                if ("image".equalsIgnoreCase(request.getUrl().getScheme())){
                    file = imageCompression(view.getContext(),file,1000);//图片压缩
                }

                byte[] imageBuf = getLocalFileByte(file);
                String mimeType = request.getUrl().getScheme()+"/*";
                return new WebResourceResponse(mimeType, "UTF-8",
                        new ByteArrayInputStream(imageBuf));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
            return null;
    }

}



//cookie同步策略
//        CookieSyncManager.createInstance(context);
//        CookieSyncManager.getInstance().startSync();
//cookie停止同步：
//CookieSyncManager.getInstance().stopSync()
//cookie立即同步：调用了该方法会立即进行cookie的同步
//        CookieSyncManager.getInstance().sync();
//删除cookie操作
//        CookieSyncManager.createInstance(this);
//        CookieManager.getInstance().removeAllCookie();
//        CookieManager.getInstance().removeSessionCookie();
//        CookieSyncManager.getInstance().sync();
//        CookieSyncManager.getInstance().startSync();
//删除cookie操作：底层实现是异步清除数据库的记录
//CookieManager.getInstance().removeAllCookies(null);
//        CookieManager.getInstance().flush();