package com.bottle.wvapp.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import com.bottle.wvapp.jsprovider.NativeJSInterface;
import com.bottle.wvapp.jsprovider.NativeServerImp;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.webh5.SysWebView;
import lee.bottle.lib.webh5.interfaces.WebProgressI;

import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;

public class GlobalMainWebView {
    @SuppressLint("StaticFieldLeak")
    private static SysWebView webView;

    /* 连接业务服务器及本地方法实现 */
    private static final NativeServerImp nativeServerImp = new NativeServerImp();

    /* js交互接口,自动绑定js dom对象 */
    private static NativeJSInterface nativeJSInterface;

    /* 浏览器已打开页面 */
    private static boolean isUrlLoading;

    public static void init(final Context context){
        webView = new SysWebView(context);
        nativeJSInterface = new NativeJSInterface(webView.jsInterface, GlobalMainWebView.getNativeServerImp());
        webView.webProgressI = new WebProgressI() {
            @Override
            public void updateProgress(String url, int current, boolean isManual) {
                LLog.print("浏览器监听进度 : "+ current +" URL = "+ url);
                if (current == 100) {
                    nativeServerImp.onJSPageInitialization();
                    webView.webProgressI = null;
                }
            }
        };
        open(_WEB_HOME_URL);
        LLog.print( webView + " GlobalMainWebView 初始化完成");
    }

    public static SysWebView getInstance(){
        return webView;
    }

    public static boolean isIsUrlLoading(){
        return isUrlLoading;
    }


    public static void open(String url){
        // 打开首页
        if (isUrlLoading) return;
        webView.open(url);
        isUrlLoading = true;
    }

    public static NativeServerImp getNativeServerImp() {
        return nativeServerImp;
    }

    public static NativeJSInterface getNativeJSInterface() {
        return nativeJSInterface;
    }
}
