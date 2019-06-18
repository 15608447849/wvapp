package com.bottom.wvapp.jsprovider;

import com.onek.server.inf._PushMessageClientDisp;

import Ice.Current;
import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/17.
 * email: 793065165@qq.com
 */
public class LongConnectionServerImp extends _PushMessageClientDisp {
    private NativeServerImp bridgeImp;

    public LongConnectionServerImp(NativeServerImp bridgeImp) {
        this.bridgeImp = bridgeImp;

    }

    /**
     * 客户端接受服务端 消息
     *
     * @param message
     * @param __current The Current object for the invocation.
     **/
    @Override
    public void receive(String message, Current __current) {
        LLog.print("服务器推送消息:" + message);
        if (bridgeImp!=null) bridgeImp.pushMessageToJs(message);
    }
}
