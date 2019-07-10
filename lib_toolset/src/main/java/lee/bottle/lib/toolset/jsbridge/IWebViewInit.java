package lee.bottle.lib.toolset.jsbridge;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import lee.bottle.lib.toolset.util.ObjectRefUtil;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public abstract class IWebViewInit<V extends View> {

    private IJsBridge proxy;

    private V webView;

    public IJsBridge getProxy() {
        return proxy;
    }

    public V getWebView(){
        return webView;
    }

    public JSInterface genJSInterface(V webview){
        return new JSInterface(webview);
    }

    public IWebViewInit(Context appContext,ViewGroup group, IBridgeImp bridge) throws Exception{
        initPrev(appContext);
        ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
        Type[] typeArr = parameterizedType.getActualTypeArguments();
        webView = (V)ObjectRefUtil.createObject((Class) typeArr[0],new Class[]{Context.class},group.getContext());
        group.addView(webView,ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
        initSetting(webView);
        proxy = genJSInterface(webView).setIBridgeImp(bridge);
    }

    protected  void initPrev(Context appContext){

    }

    public abstract void initSetting(V webview);

    public abstract boolean onBackPressed();

    public abstract void clear();

    public abstract void close(ViewGroup view);

}
