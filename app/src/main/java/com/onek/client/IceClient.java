package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import Ice.Communicator;
import Ice.ObjectAdapter;
import Ice.ObjectPrx;
import Ice.Util;
import lee.bottle.lib.toolset.log.LLog;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {

    private static class ReqStore{
        InterfacesPrx currentPrx;
        IRequest request;

        private ReqStore(InterfacesPrx currentPrx) {
            this.currentPrx = currentPrx;
            this.request = new IRequest();
        }
    }

    private final ThreadLocal<ReqStore> threadLocalStore = new ThreadLocal<>();
    private final ThreadLocal<Map<String,InterfacesPrx>> threadLocalStoreInterfacesPrx = new ThreadLocal<>();

    /* 通讯对象 */
    private Communicator ic = null;

    /* 本地通讯端点 */
    private Ice.ObjectAdapter localAdapter;

    private final String[] args ;

    private int timeout = 30000;

    public IceClient(String tag,String serverAdds,String argsStr) {
        args = initParams(tag,serverAdds,argsStr.split(","));
    }

    private String[] initParams(String tag,String serverAdds,String... iceArgs) {
        String[] arr;
        if (iceArgs == null) {
            arr = new String[1];
        }else{
            arr = new String[iceArgs.length + 1];
            System.arraycopy(iceArgs, 0, arr, 1, iceArgs.length);
        }
        StringBuilder address = new StringBuilder("--Ice.Default.Locator="+tag+"/Locator");
        String str = ":%s -h %s -p %s";
        String[] infos = serverAdds.split(";");
        for (String info : infos){
            String[] host_port = info.split(":");
            if (host_port.length == 3){
                address.append(String.format(Locale.CHINA,str, host_port[0],host_port[1],host_port[2]));
            }
            if (host_port.length == 2){
                address.append(String.format(Locale.CHINA,str, "tcp",host_port[0],host_port[1]));
            }
        }
        arr[0] = address.toString();
        return arr;
    }

    public ObjectAdapter getLocalAdapter(){
        if (ic == null) throw new IllegalStateException("ICE COMMUNICATION NOT START.");
        return localAdapter;
    }

    public IceClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Util.initialize(args);
            localAdapter = ic.createObjectAdapter("");
            localAdapter.activate();
        }
        return this;
    }

    synchronized
    public IceClient stopCommunication() {
        if (ic != null) {
            try {
                ic.destroy();
            } catch (Exception e) {
                LLog.error(e);
            }
        }
        return this;
    }

    public IceClient settingProxy(String serverName){
        Map<String,InterfacesPrx> map = threadLocalStoreInterfacesPrx.get();
        if (map == null){
            map = new HashMap<>();
            threadLocalStoreInterfacesPrx.set(map);
        }

        InterfacesPrx curPrx = map.get(serverName);
        if (curPrx == null){
            ObjectPrx base = ic.stringToProxy(serverName).ice_invocationTimeout(timeout);
            curPrx = InterfacesPrxHelper.checkedCast(base);
            map.put(serverName,curPrx);
        }

        // 设置当前线程的代理对象并创建请求
        threadLocalStore.set(new ReqStore(curPrx));
        return this;
    }

    public InterfacesPrx getProxy(){
        ReqStore store = threadLocalStore.get();
        return store == null ? null : store.currentPrx;
    }

    public IceClient settingReq(String token,String cls,String med){
        ReqStore store = threadLocalStore.get();
        if (store!=null && store.request!=null){
            store.request.cls = cls;
            store.request.method = med;
            store.request.param.token = token;
        }
        return this;
    }

    public IceClient setServerAndRequest(String token,String serverName,String clazz,String method){
        return settingProxy(serverName).settingReq(token,clazz,method);
    }

    public IceClient setServerAndRequest(String serverName,String clazz,String method){
        return setServerAndRequest("",serverName,clazz,method);
    }

    public IceClient setPageInfo(int index, int number) {
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store.request.param.pageIndex = index;
            store.request.param.pageNumber = number;
        }
        return this;
    }

    public IceClient setExtend(String extend){
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store. request.param.extend = extend;
        }
        return this;
    }

    public IceClient settingParam(String json){
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store. request.param.json = json;
        }
        return this;
    }

    public IceClient settingParam(String[] array){
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store.request.param.arrays = array;
        }
        return this;
    }

    public String execute() {
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.currentPrx!=null && store.request!=null){
            threadLocalStore.remove();
            return store.currentPrx.accessService(store.request);
        }
        throw new IllegalStateException("ICE 未开始连接或找不到远程代理或请求参数异常");
    }


    public void sendMessageToClient(String serverName, String identity, String message) {
        InterfacesPrx curPrx = null;
        if (serverName != null) {
            ObjectPrx base = ic.stringToProxy(serverName).ice_invocationTimeout(this.timeout);
            curPrx = InterfacesPrxHelper.checkedCast(base);
        }
        if (curPrx == null) {
            throw new IllegalArgumentException("请设置正确的IM服务名");
        } else {
            curPrx.sendMessageToClient(identity, message);
        }
    }

}
