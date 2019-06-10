package com.bottom.wvapp.tbsx5;

import android.annotation.SuppressLint;
import android.content.Context;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.CookieSyncManager;
import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;

import lee.bottle.lib.toolset.log.LLog;

import static lee.bottle.lib.toolset.util.AppUtils.getLocalFileByte;
import static lee.bottle.lib.toolset.util.ImageUtils.imageCompression;

;
;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class TbsWebViewUtil {

    public static void tbsInit(final Context appContext) {

        File file = new File("/storage/emulated/0/tencent/tbs/backup/com.bottom.wvapp/x5.tbs.decouple");
        if (!file.exists()){
            file.mkdirs();
        }

        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核
        final QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                LLog.print("X5内核初始化完成");
            }

            @Override
            public void onViewInitFinished(boolean flag) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                LLog.print("X5内核使用: "+ flag);

            }
        };

//        QbSdk.setDownloadWithoutWifi(true);
        //x5内核初始化接口
        QbSdk.initX5Environment(appContext,  cb);

        LLog.print("开始初始化X5内核");
    }

    public static boolean isX5CoreUse(com.tencent.smtt.sdk.WebView webView){
        return webView.getX5WebViewExtension() != null;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    public static void initViewWeb(com.tencent.smtt.sdk.WebView webView, String name, Object jsBridge){
        initSetting(webView.getContext() , webView.getSettings());

        webView.setWebChromeClient(new X5WebChromeClient());
        webView.setWebViewClient(new X5WebViewClient());

        //去除滚动条
        webView.setHorizontalFadingEdgeEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        if (isX5CoreUse(webView)) webView.getX5WebViewExtension().setScrollBarFadingEnabled(false);

        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        webView.addJavascriptInterface(jsBridge, name);

        CookieSyncManager.createInstance(webView.getContext());
        CookieSyncManager.getInstance().sync();
    }

    private static void initSetting(Context context,com.tencent.smtt.sdk.WebSettings settings) {
        //https://blog.csdn.net/a2241076850/article/details/52983939

        //设置WebView是否允许执行JavaScript脚本，默认false
        settings.setJavaScriptEnabled(true);
        //设置js可以直接打开窗口，如window.open()，默认为false
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        //是否需要用户的手势进行媒体播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        //允许访问文件
        settings.setAllowFileAccess(true);
        //是否允许运行在一个context of a file scheme URL环境中的JavaScript访问来自其他URL环境的内容
        settings.setAllowFileAccessFromFileURLs(false);
        //是否允许运行在一个file schema URL环境下的JavaScript访问来自其他任何来源的内容
        settings.setAllowUniversalAccessFromFileURLs(false);

        settings.setLayoutAlgorithm(com.tencent.smtt.sdk.WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

        //不支持使用屏幕上的缩放控件和手势进行缩放
        settings.setSupportZoom(false);
        //是否使用内置的缩放机制
        settings.setSupportZoom(false);

        //是否支持HTML的“viewport”标签或者使用wide viewport
        settings.setUseWideViewPort(true);
        //是否允许WebView度超出以概览的方式载入页面
        settings.setLoadWithOverviewMode(true);

        //设置WebView是否支持多窗口
        settings.setSupportMultipleWindows(false);

        //应用缓存API是否可用
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(context.getDir("appcache", 0).getPath());

        //数据库
        settings.setDatabaseEnabled(true);

        //DOM存储API是否可用
        settings.setDomStorageEnabled(true);
        //地图
        settings.setGeolocationEnabled(true);
        //定位数据库的保存路径，为了确保定位权限和缓存位置的持久化，该方法应该传入一个应用可写的路径。
        settings.setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());

        /**
         * 当一个安全的来源试图从一个不安全的来源加载资源时配置WebView的行为
         * 默认情况下，KITKAT及更低版本默认值为MIXED_CONTENT_ALWAYS_ALLOW，LOLLIPOP版本默认值MIXED_CONTENT_NEVER_ALLOW，WebView首选的最安全的操作模式为MIXED_CONTENT_NEVER_ALLOW ，不鼓励使用MIXED_CONTENT_ALWAYS_ALLOW
         */
        settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        //是否保存表单数据，默认值true
        settings.setSaveFormData(true);

        //图片自动下载
        settings.setLoadsImagesAutomatically(true);
        //不禁止网络图片加载
        settings.setBlockNetworkImage(false);

    }

    //媒体文件加载
    public static WebResourceResponse mediaLoad(WebView view, WebResourceRequest request){
        String scheme = request.getUrl().getScheme();
        try {
            if ("image".equalsIgnoreCase(scheme)
                    || "audio".equalsIgnoreCase(scheme)
                    || "video".equalsIgnoreCase(scheme)){
                LLog.print("拦截URL处理\t" + request.getUrl());
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


/**
 *LOAD_CACHE_ONLY： 不使用网络，只读取本地缓存数据，
 * LOAD_DEFAULT：根据cache-control决定是否从网络上取数据，
 * LOAD_CACHE_NORMAL：API level 17中已经废弃, 从API level 11开始作用同- - LOAD_DEFAULT模式，
 * LOAD_NO_CACHE: 不使用缓存，只从网络获取数据，
 * LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。
 */


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