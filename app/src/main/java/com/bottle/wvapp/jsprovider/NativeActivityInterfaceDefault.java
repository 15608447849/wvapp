package com.bottle.wvapp.jsprovider;

import android.app.Activity;

import lee.bottle.lib.webh5.SysWebView;
import lee.bottle.lib.webh5.interfaces.JSResponseCallback;

public class NativeActivityInterfaceDefault implements NativeActivityInterface{

    private Activity activity;
    private SysWebView webView;
    private NativeJSInterface jsInterface;

    public NativeActivityInterfaceDefault(Activity activity, SysWebView webView, NativeJSInterface jsInterface) {
        this.activity = activity;
        this.webView = webView;
        this.jsInterface = jsInterface;
    }

    @Override
    public Activity getNativeActivity() {
        return activity;
    }

    @Override
    public void clearCache() {
        if (activity==null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //if (webView !=null) webView.clearCache(true,false);
            }
        });

    }

    @Override
    public void callJsFunction(String funName, String data, JSResponseCallback callback) {
        if (jsInterface!=null) jsInterface._nativeInvokeJS(funName,data,callback);

    }


    @Override
    public void connectIceIM() {

    }




    @Override
    public void onJSPageInitialization() {

    }

    @Override
    public void onIndexPageShowBefore() {

    }
}
