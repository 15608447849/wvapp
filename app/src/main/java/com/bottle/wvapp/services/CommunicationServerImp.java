package com.bottle.wvapp.services;

import android.content.Intent;

import com.bottle.wvapp.activitys.NativeActivity;
import com.bottle.wvapp.app.ApplicationDevInfo;
import com.bottle.wvapp.tool.NotifyUer;
import com.onek.server.inf._PushMessageClientDisp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import Ice.Current;
import lee.bottle.lib.toolset.jsbridge.JSInterface;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.bottle.wvapp.app.BusinessData.refreshCompanyInfoAndOutput;

/**
 * Created by Leeping on 2019/6/17.
 * email: 793065165@qq.com
 */
public class CommunicationServerImp extends _PushMessageClientDisp {

    private static final String KEEP_ALIVE_SIGNAL = "keep_alive";

    public Ice.Identity identity;

    private final IMService im;

    volatile boolean isRunning = true;

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


    //检查长连接是否有效
    boolean checkCommunication(String fn,String identityName) {
        try {

            if (identity!=null && identity.name.equals(identityName) ){
                ice_ping();
                im.client.sendMessageToClient(fn,identityName,KEEP_ALIVE_SIGNAL);
                im.communicationCountSet(-1);
                LLog.print("checkCommunication...");
                return true;
            }
        } catch (Exception e) {
            LLog.print("长连接检测异常:" + e.getMessage());
        }
        return false;
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
                LLog.print(Thread.currentThread()+ " 收到服务器推送消息:" + message);

                if (message.equals(KEEP_ALIVE_SIGNAL)) {
                    im.communicationCountSet(1);
                    return;
                }

                messageQueue.offer(message);
            }
        });

    }

}
