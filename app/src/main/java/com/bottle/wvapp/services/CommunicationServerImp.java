package com.bottle.wvapp.services;

import com.bottle.wvapp.app.ApplicationDevInfo;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf._PushMessageClientDisp;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Ice.Current;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;

/**
 * Created by Leeping on 2019/6/17.
 * email: 793065165@qq.com
 */
public class CommunicationServerImp extends _PushMessageClientDisp {
    public Ice.Identity identity;
    public InterfacesPrx prx;
    private final IMService im;
    volatile boolean isRunning = true;
    volatile long lastHeartbeatTime;
    // 待处理消息列表
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    public CommunicationServerImp(IMService service) {
       this.im = service;
       start();
    }

    private void start() {
        final Runnable HANDLE_MESSAGE_RUN = new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        String message = messageQueue.take();
                        im.handlerMessage(message);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(HANDLE_MESSAGE_RUN);
        thread.setDaemon(true);
        thread.start();
    }
    /**
     * 客户端接收服务端 消息
     *
     * @param message
     * @param __current The Current object for the invocation.
     **/
    @Override
    public void receive(final String message, Current __current) {
        LLog.print(Thread.currentThread()+" 接收长连接消息: "+ message);
        messageQueue.offer(message);
    }

}
