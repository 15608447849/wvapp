package lee.bottle.lib.webh5;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;

import static lee.bottle.lib.toolset.os.ApplicationDevInfo.sharedStorage;

public class SysWebViewSetting {


    public static void initGlobalSetting(Application application){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 安卓9.0后不允许多进程使用同一个数据目录
            try {
                WebView.setDataDirectorySuffix(AppUtils.getCurrentProcessName(application.getApplicationContext()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 允许跨域
        CookieManager.getInstance().setAcceptCookie(true);
    }

    /* 保存cookie */
    public static void saveCurrentCookie(WebView webView, String url){
        CookieManager cookieManager = CookieManager.getInstance();
        String cookieStr = cookieManager.getCookie(url);
        SharedPreferences sp = sharedStorage(webView.getContext());
        sp.edit().putString("CURRENT_COOKIE",cookieStr).apply();
        LLog.print("保存最新cookie>> URL = "+ url+"\nCOOKIE = "+ cookieStr);
    }

    /* 使用cookie */
    public static void useCurrentCookie(WebView webView, String url){
        CookieManager cookieManager = CookieManager.getInstance();
        String cookieStrOld = cookieManager.getCookie(url);

        SharedPreferences sp = sharedStorage(webView.getContext());
        String cookieStr = sp.getString("CURRENT_COOKIE",null);
        if (cookieStr==null) return;
        cookieManager.removeAllCookie();
        String[] arr = cookieStr.split(";");
        for (String kv :arr){
            cookieManager.setCookie(url,kv);
        }

        cookieManager.flush();
        LLog.print("使用最新cookie>> URL = "+ url+"\nCOOKIE = "+ cookieStr
                +"\n原COOCKIE = "+cookieStrOld
                +"\n现COOCKIE = "+ cookieManager.getCookie(url));
    }


    @SuppressLint("SetJavaScriptEnabled")
    protected static void initSetting(SysWebView webview) {
        Context context = webview.getContext();
        WebSettings settings = webview.getSettings();

        //设置WebView是否允许执行JavaScript脚本，默认false
        settings.setJavaScriptEnabled(true);
        //设置js可以直接打开窗口，如window.open()，默认为false
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        //设置WebView是否支持多窗口
        settings.setSupportMultipleWindows(true);

        //是否需要用户的手势进行媒体播放
        settings.setMediaPlaybackRequiresUserGesture(false);

        //允许访问文件
        settings.setAllowFileAccess(true);
        //是否允许运行在一个context of pay_result file scheme URL环境中的JavaScript访问来自其他URL环境的内容
        settings.setAllowFileAccessFromFileURLs(false);
        //是否允许运行在一个file schema URL环境下的JavaScript访问来自其他任何来源的内容
        settings.setAllowUniversalAccessFromFileURLs(false);

        // 设置基础布局算法
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        //不支持使用屏幕上的缩放控件和手势进行缩放
        settings.setSupportZoom(false);
        //是否使用内置的缩放机制
        settings.setSupportZoom(false);
        //是否支持HTML的“viewport”标签或者使用wide viewport
        settings.setUseWideViewPort(true);
        //是否允许WebView度超出以概览的方式载入页面
        settings.setLoadWithOverviewMode(true);



        //应用缓存API是否可用
        settings.setAppCacheEnabled(true);
        //数据缓存
        settings.setAppCachePath( context.getDir("webcache",0).getPath());
        settings.setAppCacheMaxSize(100*1024*1024);

        //数据库
        settings.setDatabasePath(context.getDir("webdatabase",0).getPath());
        settings.setDatabaseEnabled(true);

        //DOM存储API是否可用
        settings.setDomStorageEnabled(true);

        //地图
        settings.setGeolocationEnabled(true);

        //定位数据库的保存路径，为了确保定位权限和缓存位置的持久化，该方法应该传入一个应用可写的路径。
        settings.setGeolocationDatabasePath(context.getDir("geolocation", 0).getPath());

        /**
         * 当一个安全的来源试图从一个不安全的来源加载资源时配置WebView的行为
         * 默认情况下，KITKAT及更低版本默认值为MIXED_CONTENT_ALWAYS_ALLOW，
         * LOLLIPOP版本默认值MIXED_CONTENT_NEVER_ALLOW，
         * WebView首选的最安全的操作模式为MIXED_CONTENT_NEVER_ALLOW ，
         * 不鼓励使用MIXED_CONTENT_ALWAYS_ALLOW
         */
        settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        //是否保存表单数据，默认值true
        settings.setSaveFormData(true);

        //图片自动下载
        settings.setLoadsImagesAutomatically(true);

        //是否禁止网络图片加载
        settings.setBlockNetworkImage(false);

        // 缓存模式
//        LOAD_CACHE_ONLY:不使用网络，只读取本地缓存数据
//        LOAD_DEFAULT:根据cache-control决定是否从网络上取数据。
//        LOAD_NO_CACHE: 不使用缓存，只从网络获取数据
//        LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setDefaultTextEncodingName("UTF-8");

        //滚动条样式
        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        //去除滚动条
        webview.setHorizontalFadingEdgeEnabled(false);
        webview.setVerticalScrollBarEnabled(false);

        //安全性漏洞
        webview.removeJavascriptInterface("searchBoxJavaBridge");
        webview.removeJavascriptInterface("accessibility");
        webview.removeJavascriptInterface("accessibilityTraversal");

        // 跨域cookie读取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true);
        }

    }


}
