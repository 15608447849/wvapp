package com.bottle.wvapp.jsprovider;

import android.content.Intent;

import java.lang.reflect.Method;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;

/**
 * Created by Leeping on 2020/6/15.
 * email: 793065165@qq.com
 */
public class NativeBridgeImp implements IBridgeImp {

    @Override
    public void setIJsBridge(IJsBridge bridge) {
        NativeServerImp.buildJSBridge(bridge);
    }

    @Override
    public Object invoke(String methodName, String data) throws Exception {
        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名@分页页码@分页条数@扩展字段
            String temp = methodName.replace("ts:","");
            String[] args = temp.split("@");
            return NativeServerImp.transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
        }
        Object val;
        //反射调用方法
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        NativeServerImp.caller.onActivityResultHandle(requestCode,resultCode,data);
        NativeServerImp.threadNotify();
    }

}
