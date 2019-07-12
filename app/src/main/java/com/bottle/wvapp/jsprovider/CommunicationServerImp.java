package com.bottle.wvapp.jsprovider;

import com.bottle.wvapp.tool.NotifyUer;
import com.onek.server.inf._PushMessageClientDisp;

import Ice.Current;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;

/**
 * Created by Leeping on 2019/6/17.
 * email: 793065165@qq.com
 */
public class CommunicationServerImp extends _PushMessageClientDisp {
    private NativeServerImp bridgeImp;

    public Ice.Identity identity;

    public boolean online = false;


    public CommunicationServerImp(NativeServerImp bridgeImp) {
        this.bridgeImp = bridgeImp;
    }

    /**
     * 客户端接受服务端 消息
     *
     * @param message
     * @param __current The Current object for the invocation.
     **/
    @Override
    public void receive(final String message, Current __current) {
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                LLog.print("服务器推送消息:" + message);
                String msg = message.substring(4);
                //解析
                if (message.startsWith("sys")){
                    //刷新用户/企业信息
                    int compid = bridgeImp.getCompId(true);
                    if (compid > 0){
                        bridgeImp.pushMessageToJs(msg);
                        NotifyUer.createMessageNotify(bridgeImp.fragment.get().getContext(), msg); //打开广播
                    }
                }
                else if (message.startsWith("pay")){
                    bridgeImp.pushPaySuccessMessageToJs(msg);
                }
            }
        });
    }

}
