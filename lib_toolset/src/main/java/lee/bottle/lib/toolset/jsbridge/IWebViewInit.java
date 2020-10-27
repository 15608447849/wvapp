package lee.bottle.lib.toolset.jsbridge;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;

import java.lang.ref.SoftReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.ObjectRefUtil;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public abstract class IWebViewInit<V extends View> {

    private IJsBridge proxy;

    private V webView;

    private SoftReference<Object> activitySoftReference;

    public IWebViewInit(Object binder, ViewGroup group, IBridgeImp bridge) throws Exception{
        activitySoftReference = new SoftReference<>(binder);
        ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
        Type[] typeArr = parameterizedType.getActualTypeArguments();
        webView = (V)ObjectRefUtil.createObject((Class) typeArr[0],new Class[]{Context.class},group.getContext());
        group.addView(webView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        initSetting(webView);
        proxy = new JSInterface(webView).setIBridgeImp(bridge);
    }

    public IJsBridge getProxy() {
        return proxy;
    }

    public V getWebView(){
        return webView;
    }

    // 获取当前绑定者
    public Object getCurrentBinder(){
        return activitySoftReference.get();
    }

    // 初始化
    protected abstract void initSetting(V webView);

    // 回退
    public abstract boolean onBackPressed();

    // 清理
    public abstract void clear(boolean includeDiskFiles);

    //关闭
    public abstract void close(ViewGroup view);

    //图片选择处理回调
    public abstract void onActivityResultHandle(int requestCode, int resultCode, Intent data);

    public abstract void setDownloadListener(DownloadListener listener);

    public static IWebViewInit createIWebView(String coreClassTypeName,Object binder,ViewGroup group, IBridgeImp bridge){
        try {
            return (IWebViewInit) ObjectRefUtil.createObject(coreClassTypeName, new Class[]{Object.class,ViewGroup.class, IBridgeImp.class}, binder, group, bridge);
        } catch (Exception e) {
            e.printStackTrace();
            LLog.print("加载内核失败, 实现类: "+ coreClassTypeName+", 原因: "+e);
        }
        return null;
    }
}
