package com.bottle.lib.tbsx5;

import android.content.Context;
import android.view.ViewGroup;

import com.tencent.smtt.sdk.QbSdk;
import com.tencent.smtt.sdk.WebView;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class X5Core extends IWebViewInit<WebView> {


    public static void tbsInit(final Context appContext) {

     /*   QbSdk.setTbsListener(new TbsListener() {
            @Override
            public void onDownloadFinish(int i) {
                LLog.print("onDownloadFinish " + i);
            }

            @Override
            public void onInstallFinish(int i) {
                LLog.print("onInstallFinish " + i);
            }

            @Override
            public void onDownloadProgress(int i) {
                LLog.print("onDownloadProgress " + i);
            }
        });

        TbsDownloader.needDownload(appContext, false);

        LLog.print("QbSdk.isTbsCoreInited() = " + QbSdk.isTbsCoreInited());*/

        QbSdk.setDownloadWithoutWifi(true);

      /*  if (!QbSdk.isTbsCoreInited()){
            QbSdk.preInit(appContext, new QbSdk.PreInitCallback() {
                @Override
                public void onCoreInitFinished() {
                    LLog.print("X5 onCoreInitFinished");
                }

                @Override
                public void onViewInitFinished(boolean b) {
                    LLog.print("X5 preInit onViewInitFinished = " + b);
                }
            });
        } */

        //x5内核初始化接口
        QbSdk.initX5Environment(appContext,   new QbSdk.PreInitCallback() {
            @Override
            public void onCoreInitFinished() {
                LLog.print("X5内核初始化完成");
            }
            @Override
            public void onViewInitFinished(boolean flag) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
//                LLog.print("X5内核使用: "+ flag);
//                toast(appContext,"当前使用X5内核: "+ flag);
            }
        });
    }

    public static boolean isX5CoreUse(com.tencent.smtt.sdk.WebView webView){
        return webView.getX5WebViewExtension() != null;
    }

    public X5Core(ViewGroup group, IBridgeImp bridge) throws Exception {
        super(group, bridge);
    }

    @Override
    protected void initPrev(ViewGroup group) {
        tbsInit(group.getContext());
    }

    @Override
    public void initSetting(WebView webview) {
        com.tencent.smtt.sdk.WebSettings settings = webview.getSettings();
        Context context = webview.getContext();
        //https://blog.csdn.net/a2241076850/article/details/52983939

        //设置WebView是否允许执行JavaScript脚本，默认false
        settings.setJavaScriptEnabled(true);
        //设置js可以直接打开窗口，如window.open()，默认为false
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        //是否需要用户的手势进行媒体播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        //允许访问文件
        settings.setAllowFileAccess(true);
        //是否允许运行在一个context of pay_result file scheme URL环境中的JavaScript访问来自其他URL环境的内容
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

        webview.setWebChromeClient(new X5WebChromeClient());
        webview.setWebViewClient(new X5WebViewClient());

        //去除滚动条
        webview.setHorizontalFadingEdgeEnabled(false);
        webview.setVerticalScrollBarEnabled(false);
        if (isX5CoreUse(webview)) webview.getX5WebViewExtension().setScrollBarFadingEnabled(false);
    }

    @Override
    public boolean onBackPressed() {
        if (getWebView()!=null){
            if (getWebView().canGoBack()) {
                getWebView().goBack();
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        getWebView().clearCache(true);
        getWebView().clearFormData();

    }

    @Override
    public void close(ViewGroup viewGroup) {
        viewGroup.removeView(getWebView());
        getWebView().pauseTimers();
        getWebView().stopLoading();
    }
}
