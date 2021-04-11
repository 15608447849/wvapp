package lee.bottle.lib.toolset.jsbridge;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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

    public IWebViewInit(Context context, IBridgeImp bridge){
        try {
            ParameterizedType parameterizedType = (ParameterizedType)this.getClass().getGenericSuperclass();
            Type[] typeArr = parameterizedType.getActualTypeArguments();
            webView = (V)ObjectRefUtil.createObject((Class) typeArr[0],new Class[]{Context.class},context);
            initSetting(webView);
            proxy = new JSInterface(webView).setIBridgeImp(bridge);
        } catch (Exception e) {
            throw new RuntimeException("无法创建 "+ getClass());
        }
    }

    public IJsBridge getProxy() {
        return proxy;
    }

    public V getWebView(){
        return webView;
    }

    public void setCurrentBinder(Object binder){
        activitySoftReference = new SoftReference<>(binder);
    }

    // 获取当前绑定者
    public Object getCurrentBinder(){
        return activitySoftReference.get();
    }

    //关联图层
    public void bindDisplayLayer(ViewGroup group){
        if (group!=null){
            group.addView(webView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    public void unbindDisplayLayer(){
        ViewParent parent = getWebView().getParent();
        if (parent != null) {
            ((ViewGroup) parent).removeView(getWebView());
        }
        clearViews();
    }


    // 初始化
    protected abstract void initSetting(V webView);

    // 回退
    public abstract boolean onBackPressed();

    // 清理
    public abstract void clear(boolean includeDiskFiles);

    // 清理图层
    public abstract void clearViews();


    //关闭
    public abstract void close();

    //图片选择处理回调
    public abstract void onActivityResultHandle(int requestCode, int resultCode, Intent data);

    public abstract void setDownloadListener(DownloadListener listener);

    public static IWebViewInit createIWebView(String coreClassTypeName, Context context, IBridgeImp bridge){
        try {
            return (IWebViewInit) ObjectRefUtil.createObject(coreClassTypeName, new Class[]{Context.class, IBridgeImp.class},context, bridge);
        } catch (Exception e) {
            LLog.print("加载内核失败, 实现类: "+ coreClassTypeName+", 原因: "+e);
        }
        return null;
    }


    public void bind(Object binder,ViewGroup viewGroup,DownloadListener listener){
        unbind();
        this.setCurrentBinder(binder);
        this.setDownloadListener(listener);
        this.bindDisplayLayer(viewGroup);
        LLog.print(this+" 绑定 "+ binder);
    }

    public void unbind(){
        this.setCurrentBinder(null);
        this.setDownloadListener(null);
        this.unbindDisplayLayer();
//        LLog.print(this+" 解绑 ");
    }
}
