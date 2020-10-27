package lee.bottle.lib.toolset.web;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/7/7.
 * email: 793065165@qq.com
 */
public class SysCore extends IWebViewInit<WebView> implements DownloadListener {

    public SysCore(Object binder,ViewGroup group, IBridgeImp bridge) throws Exception {
        super(binder,group, bridge);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void initSetting(WebView webview) {
        Context context = webview.getContext();
        WebSettings settings = webview.getSettings();
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

        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);

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
//        settings.setBlockNetworkImage(false);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
//        settings.setBlockNetworkImage(true);
        settings.setDefaultTextEncodingName("UTF-8");

        webview.setWebChromeClient(new SysWebChromeClient(this));
        webview.setWebViewClient(new SysWebViewClient(this));

        webview.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        //去除滚动条
        webview.setHorizontalFadingEdgeEnabled(false);
        webview.setVerticalScrollBarEnabled(false);

        webview.setDownloadListener(this);
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
    public void clear(boolean includeDiskFiles) {
        getWebView().clearCache(includeDiskFiles);
        getWebView().clearFormData();
        getWebView().clearHistory();
        getWebView().clearMatches();
        getWebView().clearSslPreferences();
    }

    @Override
    public void close(ViewGroup viewGroup) {
        viewGroup.removeView(getWebView());
        getWebView().pauseTimers();
        getWebView().stopLoading();
    }

    @Override
    public void onActivityResultHandle(int requestCode, int resultCode, Intent data) {
        JSUtils.onActivityResultHandle(requestCode,resultCode,data);
    }

    private DownloadListener listenerImp;
    @Override
    public void setDownloadListener(DownloadListener listener) {
        if (listener!=null){
            listenerImp = listener;
        }
    }

    /**
     webview 文件下载
     */
    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
        LLog.print("浏览器下载url: "+ url+" , userAgent:"+userAgent+" , contentDisposition:"+contentDisposition+" , mimetype:"+mimetype+" , contentLength:"+contentLength  );
        if (listenerImp!=null){
            try {
                listenerImp.onDownloadStart(url,userAgent,contentDisposition,mimetype,contentLength);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            Object object = getCurrentBinder();
            if (object == null) return;
            if (object instanceof Activity){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(url));
                ((Activity)object).startActivity(intent);
            }
        }

    }
}
