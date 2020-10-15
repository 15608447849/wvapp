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

    public Ice.Identity identity;
    public boolean online = false;

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
                try {
                    //刷新用户/ 企业信息
                    int compid = NativeServerImp.refreshCompanyInfo(true);
                    LLog.print("公司标识 " + compid + " ,收到服务器推送消息\t" + message);
                    String prop = message.substring(0,message.indexOf(":"));
                    String msg = message.substring(message.indexOf(":")+1);

                    //其他相同设备登录,强制下线
                    if(prop.equals("logout-force")  ){
                        String devID = msg.substring(0,msg.lastIndexOf("@"));
                        String devType = msg.substring(msg.lastIndexOf("@")+1);
                        //相同设备 并且 设备标识不相同
                        if (devType.equals(NativeServerImp.DEVTYPE) && !devID.equals(NativeServerImp.DEVID)){
                            if (compid == 0) {
                                LLog.print("用户登出提醒");
                                NativeServerImp.forceLogout();
                            }
                        }
                    }
                    if (message.equals("pay")){
                        NativeServerImp.pushPaySuccessMessageToJs(msg);
                    }
                    //推送消息
                    if (prop.equals("push") || prop.equals("custom")){
                        String content = msg;
                        String likePath = null;
                        if (message.startsWith("custom")){
                          // [内容,链接]
                            String[] arr = content.split(";");
                            if (arr.length >= 1){
                                content = arr[0];
                            }
                            if (arr.length >= 2){
                                likePath = arr[1];
                            }
                        }
                        NativeServerImp.pushMessageToJs(content);
                        //打开广播-跳转个人中心
                        NotifyUer.createMessageNotify(NativeServerImp.app.getApplicationContext(), content,likePath);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
