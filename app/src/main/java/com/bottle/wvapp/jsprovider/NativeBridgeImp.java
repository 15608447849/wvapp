package com.bottle.wvapp.jsprovider;
import android.content.Intent;

import java.lang.reflect.Method;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2020/6/15.
 * email: 793065165@qq.com
 */
public class NativeBridgeImp implements IBridgeImp {

    /* js交互接口 */
    private IJsBridge iJsBridge;

    @Override
    public void setIJsBridge(IJsBridge bridge) {
        this.iJsBridge = bridge;
    }

    @Override
    public Object invoke(String methodName, String data) throws Exception {
        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名@分页页码@分页条数@扩展字段
            String temp = methodName.replace("ts:","");
            final String[] args = temp.split("@");
//            String result = NativePrevLoad.prevTransfer(args[0],args[1],args[2],Integer.parseInt(args[3]),args[4],args[5],data);
//            if (result == null) result = NativeServerImp.transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
            return NativeServerImp.transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
        }
        return callLocalMethod(methodName,data);
    }

    private Object callLocalMethod(String methodName, String data) throws Exception {
        Object val;
        //反射调用本地方法
//        LLog.print("本地方法***********>> " + methodName+" , 参数: "+ data);

        if(data == null){
            Method m = NativeServerImp.caller.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            val =  m.invoke(NativeServerImp.caller);
        }else{
            Method m = NativeServerImp.caller.getClass().getDeclaredMethod(methodName,String.class);
            m.setAccessible(true);
            val = m.invoke(NativeServerImp.caller,data);
        }
        return val;
    }

    public IJsBridge get() {
        return iJsBridge;
    }

    //调用JS方法,前提: 需要JS注册接口
    public void callJsFunction(String funName, String data, IJsBridge.JSCallback callback){
        if (iJsBridge!=null){
//            LLog.print("调用JS: " + funName+" "+ data+" "+ callback);
            iJsBridge.requestJs(funName,data,callback);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        NativeServerImp.caller.onActivityResultHandle(requestCode,resultCode,data);
        NativeServerImp.threadNotify();
    }

}
