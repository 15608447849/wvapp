package com.onek.client;

import com.onek.server.inf.IRequest;
import com.onek.server.inf.InterfacesPrx;
import com.onek.server.inf.InterfacesPrxHelper;

import java.util.Arrays;
import java.util.Locale;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;

/**
 * @Author: leeping
 * @Date: 2019/4/9 14:15
 * ice客户端远程调用
 */
public class IceClient {

    private  Ice.Communicator ic = null;

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

    public Ice.Communicator iceCommunication(){
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
        Ice.ObjectPrx base = ic.stringToProxy(serverName);
        curPrx =  InterfacesPrxHelper.checkedCast(base);
        curPrx.ice_invocationTimeout(timeout);
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
            String res = curPrx.accessService(request);
            curPrx = null;
            request = null;
            return res;
        }
        throw new RuntimeException("ICE 未开始连接或找不到远程代理或请求参数异常");
    }

    public void sendMessageToClient(String identity,String message){
        if (curPrx!=null){
             curPrx.sendMessageToClient(identity,message);
        }
    }


}
