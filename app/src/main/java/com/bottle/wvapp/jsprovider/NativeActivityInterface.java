package com.bottle.wvapp.jsprovider;

import android.app.Activity;

import lee.bottle.lib.webh5.interfaces.JSResponseCallback;

public interface NativeActivityInterface {

    Activity getNativeActivity();
    // 打开IM服务
    void connectIceIM();
    // JS页面元素初始化完成
    void onJSPageInitialization();
    // 首页展示之前
    void onIndexPageShowBefore();
    // 清理缓存
    void clearCache();
    // 调用JS方法
    void callJsFunction(String funName, String data, JSResponseCallback callback);

}
