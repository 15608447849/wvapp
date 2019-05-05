package com.bottom.wvapp.service;

import com.bottom.wvapp.bridge.IJsBridge;
import com.onek.client.IceClient;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class BackServerImp implements ITransferServer{
    private static IceClient ic = null;

    public IJsBridge jsBridgeImp;

    public static void start(IceClient client) {
        if (ic == null){
            ic = client;
            ic.startCommunication();
        }
    }

    public BackServerImp() {
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
    }

    public void settingBridge(IJsBridge jsBridge){
        this.jsBridgeImp = jsBridge;
    }

    public String fsInfo(String json){
        return transfer("globalServer","FileInfoModule","fileServerInfo",json);
    }


    @Override
    public String transfer(String serverName, String cls, String method, String json) {
        return ic.setServerAndRequest(serverName,cls,method).settingParam(json).execute();
    }
}
