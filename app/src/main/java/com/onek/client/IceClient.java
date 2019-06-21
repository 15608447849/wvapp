package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

import java.util.HashMap;
import java.util.Locale;

import lee.bottle.lib.toolset.util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {

    private  Ice.Communicator ic = null;

    public Ice.Communicator iceCommunication(){
        return ic;
    }

    //接口代理缓存
    private final HashMap<String,InterfacesPrx> prxMaps;

    private final String[] args ;

    public IceClient(String tag, String host, int port) {
        String str = "--Ice.Default.Locator=%s/Locator:tcp -h %s -p %d";
        args = new String[]{String.format(Locale.CHINA,str,tag,host,port)};
        prxMaps = new HashMap<>();
    }

    synchronized
    public IceClient startCommunication() {
        if (ic == null) {
            ic = Ice.Util.initialize(args);
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

    public InterfacesPrx curPrx;

    public IceClient settingProxy(String serverName){
        InterfacesPrx prx = prxMaps.get(serverName);
        if (prx == null){
            Ice.ObjectPrx base = ic.stringToProxy(serverName);
            prx =  InterfacesPrxHelper.checkedCast(base);

            if (prx!=null) {
                prx.ice_invocationTimeout(5000);
                prxMaps.put(serverName,prx);
            }
        }
        curPrx = prx;
        return this;
    }

    public InterfacesPrx getProxy(){
        return curPrx;
    }
    private IRequest request;

    public IceClient settingReq(String token,String cls,String med){
        request = new IRequest();
        request.cls = cls;
        request.method = med;
        request.param.token = token;
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

    public IceClient setJsonParams(Object obj){
        return settingParam(GsonUtils.javaBeanToJson(obj));
    }

    public IceClient settingParam(String json, int index, int number){
        request.param.json = json;
        request.param.pageIndex = index;
        request.param.pageNumber = number;
        return this;
    }
    public IceClient settingParam(String json){
        request.param.json = json;
        return this;
    }
    public IceClient setPageInfo(int index, int number) {
        request.param.pageIndex = index;
        request.param.pageNumber = number;
        return this;
    }
    public IceClient settingParam(String[] array, int index, int number){
        request.param.arrays = array;
        request.param.pageIndex = index;
        request.param.pageNumber = number;
        return this;
    }
    public IceClient settingParam(String[] array){
        request.param.arrays = array;
        return this;
    }

    public String execute() {
        if (curPrx!=null && request!=null){
            return curPrx.accessService(request);
        }
        throw new RuntimeException("ICE 未开始连接或找不到远程代理或请求参数异常");
    }

    public void sendMessageToClient(String identity,String message){
        if (curPrx!=null){
             curPrx.sendMessageToClient(identity,message);
        }
    }


}
