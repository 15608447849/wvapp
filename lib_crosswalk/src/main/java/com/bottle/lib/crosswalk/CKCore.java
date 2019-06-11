package com.bottle.lib.crosswalk;

import android.view.ViewGroup;

import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkSettings;
import org.xwalk.core.XWalkView;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IWebViewInit;
import lee.bottle.lib.toolset.jsbridge.JSInterface;

;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class CKCore extends IWebViewInit<XWalkView> {

    public CKCore(ViewGroup group, IBridgeImp bridge) throws Exception{
        super(group, bridge);
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
        };
    }

    @Override
    public void initSetting(XWalkView webview) {
        XWalkSettings settings = webview.getSettings();
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

        settings.setLayoutAlgorithm(XWalkSettings.LayoutAlgorithm.NARROW_COLUMNS);

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


        //数据库
        settings.setDatabaseEnabled(true);

        //DOM存储API是否可用
        settings.setDomStorageEnabled(true);

        //是否保存表单数据，默认值true
        settings.setSaveFormData(true);

        //图片自动下载
        settings.setLoadsImagesAutomatically(true);
        //不禁止网络图片加载
        settings.setBlockNetworkImage(false);

        webview.setResourceClient(new CKWebViewClient(webview));
        webview.setUIClient(new CKWebChromeClient(webview));

        //去除滚动条
        webview.setHorizontalFadingEdgeEnabled(false);
        webview.setVerticalScrollBarEnabled(false);
        webview.requestFocus();
    }

    @Override
    public boolean onBackPressed() {
        if (getWebView().getNavigationHistory().canGoBack()){
            getWebView().getNavigationHistory().navigate(XWalkNavigationHistory.Direction.BACKWARD, 1);
            return true;
        }
        return false;
    }
}
