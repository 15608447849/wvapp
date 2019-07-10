package com.bottle.lib.crosswalk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewGroup;

import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkView;
import org.xwalk.core.internal.XWalkSettingsInternal;
import org.xwalk.core.internal.XWalkViewBridge;

import java.lang.reflect.Method;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.jsbridge.JSInterface;
import lee.bottle.lib.toolset.log.LLog;

import static org.xwalk.core.internal.XWalkSettingsInternal.LayoutAlgorithmInternal.NARROW_COLUMNS;

;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class CKCore extends IWebViewInit<XWalkView> {


    public CKCore(Context appContext,ViewGroup group, IBridgeImp bridge) throws Exception{
        super(appContext,group, bridge);
    }

    @Override
    public JSInterface genJSInterface(XWalkView webview) {
        return new JSInterface(webview){
            @Override
            @ org.xwalk.core.JavascriptInterface
            public void invoke(String methodName, String data, String callback_id) {
                super.invoke(methodName, data, callback_id);
            }

            @Override
            @ org.xwalk.core.JavascriptInterface
            public void callbackInvoke(String callback_id, String data) {
                super.callbackInvoke(callback_id, data);
            }

            @Override
            @ org.xwalk.core.JavascriptInterface
            public void putData(String key, String val) {
                super.putData(key, val);
            }

            @Override
            @ org.xwalk.core.JavascriptInterface
            public String getData(String key) {
                return super.getData(key);
            }

            @Override
            @ org.xwalk.core.JavascriptInterface
            public void delData(String key) {
                super.delData(key);
            }
        };
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void initSetting(XWalkView webview) {
        try {
            Method _getBridge;
            _getBridge = XWalkView.class.getDeclaredMethod("getBridge");
            _getBridge.setAccessible(true);
            XWalkViewBridge xWalkViewBridge = (XWalkViewBridge)_getBridge.invoke(webview);
            XWalkSettingsInternal settings = xWalkViewBridge.getSettings();

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

            settings.setLayoutAlgorithm(NARROW_COLUMNS);

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

            //设置为可以缓存
            settings.setAppCacheEnabled(true);

            //数据库
            settings.setDatabaseEnabled(true);
            XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
            settings.setGeolocationEnabled(true);
            //DOM存储API是否可用
            settings.setDomStorageEnabled(true);

            //是否保存表单数据，默认值true
            settings.setSaveFormData(true);

            //图片自动下载
            settings.setLoadsImagesAutomatically(true);
            //不禁止网络图片加载
            settings.setBlockNetworkImage(false);

            settings.setCacheMode(XWalkSettingsInternal.LOAD_DEFAULT);

            webview.setResourceClient(new CKWebViewClient(webview));
            webview.setUIClient(new CKWebChromeClient(webview));

            //去除滚动条
            webview.setHorizontalFadingEdgeEnabled(false);
            webview.setVerticalScrollBarEnabled(false);
            webview.requestFocus();
            clear();
            LLog.print("crosswalk 初始化浏览器设置完成");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onBackPressed() {
        if (getWebView().getNavigationHistory().canGoBack()){
            getWebView().getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        getWebView().clearCache(true);
        getWebView().clearFormData();
        getWebView().getNavigationHistory().clear();
    }

    @Override
    public void close(ViewGroup viewGroup) {
        viewGroup.removeView(getWebView());
        getWebView().pauseTimers();
        getWebView().stopLoading();
        getWebView().onDestroy();
    }
}
