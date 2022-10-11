package com.bottle.wvapp.services;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf._PushMessageClientDisp;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import Ice.Current;
import lee.bottle.lib.toolset.log.LLog;


/**
 * Created by Leeping on 2019/6/17.
 * email: 793065165@qq.com
 */
public class CommunicationServer extends _PushMessageClientDisp implements Runnable {
    public Ice.Identity identity;
    public InterfacesPrx prx;

    public volatile long lastHeartbeatTime;

    /* 处理消息 */
    private final CommunicationHandler handler;

    // 待处理消息列表
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public CommunicationServer(IMService service) {
       handler = new CommunicationHandler(service);
       Thread thread = new Thread(this);
       thread.setDaemon(true);
       thread.start();

    }


    @Override
    public void run() {
        while (true) {
            try {
                String message = messageQueue.take();
                handler.handlerMessage(message);
            } catch (Exception e) {
                LLog.error(e);
            }
        }
    }

    /**
     * 客户端接收服务端 消息
     *
     * @param message
     * @param __current The Current object for the invocation.
     **/
    @Override
    public void receive(final String message, Current __current) {
        LLog.print(Thread.currentThread()+" 接收长连接推送消息: "+ message);
        messageQueue.offer(message);
    }

}
