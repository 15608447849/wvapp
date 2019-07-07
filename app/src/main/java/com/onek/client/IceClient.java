package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

import java.util.Arrays;
import java.util.Locale;

import Ice.Communicator;
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
    private static final ThreadLocal<ReqStore> threadLocalStore = new ThreadLocal<>();

    private Communicator ic = null;

    private final String[] args ;

    private int timeout = 30000;

    public IceClient(String tag,String serverAdds,String argsStr) {
        args = initParams(tag,serverAdds,argsStr.split(","));
        LLog.print("服务器信息:" + getEnvId() );
    }

    private String[] initParams(String tag,String serverAdds,String... iceArgs) {
        StringBuffer sb = new StringBuffer("--Ice.Default.Locator="+tag+"/Locator");
        String str = ":tcp -h %s -p %s";
        String[] infos = serverAdds.split(";");
        for (String info : infos){
            String[] host_port = info.split(":");
            sb.append(String.format(Locale.CHINA,str, host_port[0],host_port[1]));
        }
        String[] arr;
        if (iceArgs == null) {
            arr = new String[1];
        }else{
            arr = new String[iceArgs.length + 1];
            System.arraycopy(iceArgs, 0, arr, 1, iceArgs.length);
        }
        arr[0] = sb.toString();
        return arr;
    }

    public Communicator iceCommunication(){
        return ic;
    }

    public String getEnvId(){
        return Arrays.toString(args);
    }
    public IceClient setTimeout(int timeout) {
        this.timeout = timeout;
        return this;
    }


    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Util.initialize(args);
        }
        return this;
    }

    synchronized
    public IceClient stopCommunication() {
        if (ic != null) {
            ic.destroy();
        }
        return this;
    }

    public IceClient settingProxy(String serverName){
        ObjectPrx base = ic.stringToProxy(serverName).ice_invocationTimeout(timeout);
        InterfacesPrx curPrx =  InterfacesPrxHelper.checkedCast(base);
        threadLocalStore.set(new ReqStore(curPrx));
        return this;
    }

    public InterfacesPrx getProxy(){
        ReqStore store = threadLocalStore.get();
        return store == null? null : store.currentPrx;
    }


    public IceClient settingReq(String token,String cls,String med){
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
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
        return settingProxy(serverName).settingReq("",clazz,method);
    }

    public IceClient setArrayParams(Object... objects){
        String[] arr = new String[objects.length];
        for (int i = 0; i< objects.length; i++) {
            arr[i] = String.valueOf(objects[i]);
        }
        return settingParam(arr);
    }



    public IceClient settingParam(String json){
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store. request.param.json = json;
        }
        return this;
    }

    public IceClient setPageInfo(int index, int number) {
        ReqStore store = threadLocalStore.get();
        if (store!=null  && store.request!=null){
            store.request.param.pageIndex = index;
            store.request.param.pageNumber = number;
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




}
