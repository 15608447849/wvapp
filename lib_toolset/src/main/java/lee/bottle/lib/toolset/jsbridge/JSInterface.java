package lee.bottle.lib.toolset.jsbridge;

import android.annotation.SuppressLint;
import android.view.View;
import android.webkit.JavascriptInterface;

import java.lang.reflect.Method;
import java.util.HashMap;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static lee.bottle.lib.toolset.util.StringUtils.getDecodeJSONStr;

/**
 * js / native 通讯接口
 */
@SuppressLint("JavascriptInterface")
public class JSInterface implements IJsBridge {

    private static final String NAME = "native";

    private final static String JAVA_SCRIPT = "javascript:";

    private final static String JS_INTERFACE_INVOKE_NAME = JAVA_SCRIPT + "JNB._invoke('%s','%s','%s')";// invoke js function

    private final static String JS_INTERFACE_NAME = JAVA_SCRIPT + "JNB._callbackInvoke('%s','%s')";// js callback function

    private final View webView;

    private IBridgeImp hImp;

    public JSInterface(View webView) {
        this.webView = webView;
        addJavascriptInterface();
    }

    private void addJavascriptInterface() {
        try {
            if (webView == null) return;
            Method m = webView.getClass().getDeclaredMethod("addJavascriptInterface",Object.class,String.class);
            m.setAccessible(true);
            m.invoke(webView,this,NAME);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //关联一个实现
    public IJsBridge setIBridgeImp(IBridgeImp imp){
        this.hImp = imp;
        this.hImp.setIJsBridge(this);
        return this;
    }

    private final HashMap<String, IJsBridge.JSCallback> jsCallbackMap = new HashMap<>();

    /**
     * js -> native
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json/text),js回调函数的ID - function(response)
     */
    @JavascriptInterface
    public void invoke(final String methodName, final String data, final String callback_id) {

        IOUtils.run(new Runnable() {
            @Override
            public void run() {

                //异步执行
                Object value = null;
                try {
                    if (hImp!=null){
                        value = hImp.invoke(methodName,data);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    value = "bridge execute error:\t"+ e;
                }

                if (callback_id == null) return;

                final String result  = value == null ? null :
                        value instanceof String ? getDecodeJSONStr(value.toString()) : GsonUtils.javaBeanToJson(value);

                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        LLog.print(methodName+" 参数:"+data+" js的回调函数:"+callback_id+"回调数据: "+result);
                        loadUrl(String.format(JS_INTERFACE_NAME,callback_id ,result));
                    }
                });

            }
        });

    }

    @Override
    public void loadUrl(String content) {
        try {
            if (webView == null) return;
            Method m = webView.getClass().getDeclaredMethod("loadUrl",String.class);
            m.setAccessible(true);
            m.invoke(webView,content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 主动调用js方法
    @Override
    public void requestJs(final String method, final String data, IJsBridge.JSCallback callback){
        String callbackId = null;
        if (callback!=null){
            callbackId = "java_callback_"+System.currentTimeMillis();
            jsCallbackMap.put(callbackId,callback);
        }
        final String _callbackId = callbackId;
        webView.post(new Runnable() {
            @Override
            public void run() {
                loadUrl(String.format(JS_INTERFACE_INVOKE_NAME,method ,data,_callbackId));
            }
        });
    }

    /**
     * native -> js ,js回调
     */
    @JavascriptInterface
    public void callbackInvoke(String callback_id,String data){
        try {
            LLog.print("callback_id = "+ callback_id+" , "+ data);
            IJsBridge.JSCallback callback = jsCallbackMap.remove(callback_id);
            if (callback==null) throw new Exception(callback_id + " callback function doesn\'t exist!");
            callback.callback(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
