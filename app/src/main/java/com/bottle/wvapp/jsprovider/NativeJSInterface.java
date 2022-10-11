package com.bottle.wvapp.jsprovider;

import android.webkit.JavascriptInterface;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.ErrorUtils;
import lee.bottle.lib.webh5.JSInterface;
import lee.bottle.lib.webh5.interfaces.JSResponseCallback;

import static lee.bottle.lib.toolset.util.GsonUtils.javaBeanToJson;
import static lee.bottle.lib.toolset.util.StringUtils.getDecodeJSONStr;

public class NativeJSInterface extends JSInterface.DefaultFunction {
    private static final String DOM_NAME = "native";

    private final static String NATIVE_INVOKE_JS_NAME = "JNB._invoke('%s','%s','%s')";// native invoke js function

    private final static String NATIVE_RESPONSE_JS_NAME = "JNB._callbackInvoke('%s','%s')";// response js callback  function

    // 错误请求列表
    private final BlockingQueue<String[]> errorQueue = new LinkedBlockingQueue<>();

    // 处理错误请求再次访问
    private Thread errorLoopThread = new Thread(){
        @Override
        public void run() {
            while (true){
                try{
                    String[] reqArr = errorQueue.take();
                    LLog.print("错误重试: "+ Arrays.toString(reqArr));
                    invoke(reqArr[0],reqArr[1],reqArr[2]);

                }catch (Exception e){
                    LLog.error(e);
                }
            }
        }
    };


    public interface NativeInvoke{
        Object jsInvokeFunction(final String methodName, final String data) throws Exception;
    }

    private final NativeInvoke hImp;

    public NativeJSInterface(JSInterface jsInterface,NativeInvoke nativeServerImp) {
        super(jsInterface);
        jsInterface.addJavascriptInterface(this);
        this.hImp = nativeServerImp;
        errorLoopThread.setDaemon(true);
        errorLoopThread.start();
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
                    value = hImp.jsInvokeFunction(methodName,data);
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
                        value instanceof String ? getDecodeJSONStr(value.toString()) : javaBeanToJson(value);

                if (targetEx!=null){
                    if (targetEx.getCause() instanceof IOException){
                        try {
                            errorQueue.put(new String[]{methodName,data,callback_id});
                            return;
                        } catch (InterruptedException e) {
                            LLog.error(e);
                        }
                    }else{
                        LLog.print("js调用native错误"
                                + "\nmethodName = "+ methodName
                                + "\ndata = " + data
                                + "\nresult = "+ result
                                + "\n"+ ErrorUtils.printExceptInfo(targetEx)
                        );
                    }
                }

                loadJavaScript(String.format(define__onResponseJSInvokeNative(),callback_id ,result));
            }
        });
    }

    /**
     * native -> js ,js回调
     */
    @JavascriptInterface
    public void callbackInvoke(String callback_id,String data){
        super._nativeInvokeJsResponse(callback_id,data);
    }

    @Override
    public String getInterfaceName() {
        return DOM_NAME;
    }
    @Override
    protected String define_nativeInvokeJS(){
        return NATIVE_INVOKE_JS_NAME;
    }
    @Override
    protected String define__onResponseJSInvokeNative(){
        return NATIVE_RESPONSE_JS_NAME;
    }
}
