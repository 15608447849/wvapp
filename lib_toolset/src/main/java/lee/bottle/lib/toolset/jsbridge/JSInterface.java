package lee.bottle.lib.toolset.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.webkit.JavascriptInterface;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.ErrorUtil;
import lee.bottle.lib.toolset.util.GsonUtils;

import static lee.bottle.lib.toolset.util.StringUtils.getDecodeJSONStr;

/**
 * js / native 通讯接口
 */
@SuppressLint("JavascriptInterface")
public class JSInterface extends Thread implements IJsBridge {
    public  static  boolean isDebug = false;

    private static final String DATA_STORAGE_FLAG = "web_store";

    public static SharedPreferences sharedStorage(Context context){
//        return context.getSharedPreferences(DATA_STORAGE_FLAG,Context.MODE_PRIVATE);
        return context.getSharedPreferences(DATA_STORAGE_FLAG,Context.MODE_MULTI_PROCESS);
    }


    private static final String NAME = "native";

    private final static String JAVA_SCRIPT = "javascript:";

    private final static String JS_INTERFACE_INVOKE_NAME = JAVA_SCRIPT + "JNB._invoke('%s','%s','%s')";// invoke js function

    private final static String JS_INTERFACE_NAME = JAVA_SCRIPT + "JNB._callbackInvoke('%s','%s')";// js callback function


    private final View webView;

    private IBridgeImp hImp;

    public JSInterface(View webView) {
        this.webView = webView;
        addJavascriptInterface();
        setDaemon(true);
        start();
    }

    private void addJavascriptInterface() {
        try {
            if (webView == null) return;
            @SuppressLint("PrivateApi") Method m = webView.getClass().getDeclaredMethod("addJavascriptInterface",Object.class,String.class);
            m.setAccessible(true);
            m.invoke(webView,this,NAME);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //关联一个实现
    public IJsBridge setIBridgeImp(IBridgeImp imp){
        if (imp!=null){
            this.hImp = imp;
            this.hImp.setIJsBridge(this);
        }
        return this;
    }

    private final HashMap<String, IJsBridge.JSCallback> jsCallbackMap = new HashMap<>();

    // 错误请求列表
    private final BlockingQueue<String[]> errorQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        while (true){
            try{
                String[] reqArr = errorQueue.take();
                invoke(reqArr[0],reqArr[1],reqArr[2]);

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * js -> native
     * 请求格式:
     * js需要调用的方法名 - method(String.class), js传递的参数信息(json/text),js回调函数的ID - function(response)
     */
    @JavascriptInterface
    public void invoke(final String methodName, final String data, final String callback_id) {
        if (hImp == null) return;
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                //异步执行
                Object value ;
                Throwable targetEx = null;
                try {
                  value = hImp.invoke(methodName,data);
                } catch (Exception e) {
                    targetEx = e;
                    if (e instanceof InvocationTargetException) {
                        targetEx =((InvocationTargetException)e).getTargetException();
                    }

                    HashMap<String,Object> map = new HashMap<>();
                    map.put("code",-1);
                    map.put("message","NATIVE ERROR");
                    map.put("error",targetEx.getMessage());
                    value = map;
                }

                if (callback_id == null) return;

                final String result  = value == null ? null :
                        value instanceof String ? getDecodeJSONStr(value.toString()) : GsonUtils.javaBeanToJson(value);

                if (targetEx!=null){
                    if (targetEx.getCause() instanceof IOException){
                        try {
                            errorQueue.put(new String[]{methodName,data,callback_id});
                            return;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        LLog.print("js调用native错误"
                                + "\nmethodName = "+ methodName
                                + "\ndata = " + data
                                + "\nresult = "+ result
                                + "\n"+ ErrorUtil.printExceptInfo(targetEx)
                        );

                        webView.post(new Runnable() {
                            @Override
                            public void run() {
                                AppUtils.toastShort(webView.getContext(),"网络异常或服务器连接失败");
                            }
                        });
                    }
                }

                webView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isDebug) {
                            LLog.print("JS->NATIVE:" + methodName + "\n参数: " + data + "\n回调数据: " + result);
                        }
                        try {
                            loadUrl(String.format(JS_INTERFACE_NAME,callback_id ,result));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });

    }

    @Override
    public void loadUrl(String content) {
        try {
            if (webView == null) return;
            @SuppressLint("PrivateApi") Method m = webView.getClass().getDeclaredMethod("loadUrl",String.class);
            m.setAccessible(true);
            try {
                m.invoke(webView,content);
            } catch (Exception e) {
                e.printStackTrace();
            }

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
                loadUrl(String.format(JS_INTERFACE_INVOKE_NAME,method ,data ,_callbackId));
            }
        });
    }

    /**
     * native -> js ,js回调
     */
    @JavascriptInterface
    public void callbackInvoke(String callback_id,String data){
        try {
            if (isDebug) LLog.print("callback_id = "+ callback_id+" , "+ data);
            IJsBridge.JSCallback callback = jsCallbackMap.remove(callback_id);
            if (callback==null) throw new Exception(callback_id + " callback function doesn\'t exist!");
            callback.callback(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



   // 存储
    @JavascriptInterface
    @Override
    public void putData(String key,String val){
        if (isDebug) LLog.print("web 存储 : " + key  + "=" + val);
        SharedPreferences sp = sharedStorage(webView.getContext());
        sp.edit().putString(key,val).apply();
    }

    //获取
    @JavascriptInterface
    @Override
    public String getData(String key){
        SharedPreferences sp = sharedStorage(webView.getContext());
        String val = sp.getString(key,"");
        if (isDebug) LLog.print("web 取值 : " + key +"="+val);
        return val;
    }

    //移除
    @JavascriptInterface
    @Override
    public void delData(String key){
        if (isDebug) LLog.print("web 删除 : " + key );
        SharedPreferences sp = sharedStorage(webView.getContext());
        sp.edit().remove(key).apply();
    }
}
