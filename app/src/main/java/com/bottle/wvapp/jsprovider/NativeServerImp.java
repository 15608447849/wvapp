package com.bottle.wvapp.jsprovider;

import android.app.Activity;

import com.bottle.wvapp.app.WebApplication;
import com.onek.client.IceClient;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.webh5.interfaces.JSResponseCallback;

import static lee.bottle.lib.toolset.os.ApplicationDevInfo.getMemoryDEVID;
import static com.bottle.wvapp.beans.BusinessData.getCurrentDevCompanyID;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp implements NativeActivityInterface,NativeJSInterface.NativeInvoke {

    /* JS需要调用的方法 */
    public final NativeMethodCallImp caller = new NativeMethodCallImp(this);

    /* activity接口引用 */
    protected SoftReference<NativeActivityInterface> nativeActivityInterfaceRef = new SoftReference<>(null);

    /* 存在协议的JS页面初始化完成 */
    public boolean isJsPageLoadComplete = false;

    /* 标记IM服务是否允许启动 */
    public boolean isImServerAcceptStart = false;

    /* 关联activity等活动层 */
    public void setNativeActivityInterface(NativeActivityInterface nativeActivityInterfaceImp){
        this.nativeActivityInterfaceRef = new SoftReference<>(nativeActivityInterfaceImp);
    }

    // js调用本地方法
    private Object callLocalMethod(String methodName, String data) throws Exception {
        Object val;
        //反射调用本地方法
//        LLog.print("本地方法***********>> " + methodName+" , 参数: "+ data);
        if(data == null){
            Method m = caller.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            val =  m.invoke(caller);
        }else{
            Method m = caller.getClass().getDeclaredMethod(methodName,String.class);
            m.setAccessible(true);
            val = m.invoke(caller,data);
        }
        return val;
    }

    // js调用方法入口
    @Override
    public Object jsInvokeFunction(String methodName, String data) throws Exception {
        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名@分页页码@分页条数@扩展字段
            String temp = methodName.replace("ts:","");
            final String[] args = temp.split("@");
//            String result = NativePrevLoad.prevTransfer(args[0],args[1],args[2],Integer.parseInt(args[3]),args[4],args[5],data);
//            if (result == null) result = NativeServerImp.transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
            return queryICE(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
        }
        return callLocalMethod(methodName,data);
    }

    //请求ICE
    protected String queryICE(final String serverName, final String cls, final String method,
                              final int page, final int count,
                              final String extend, final String json) {
        IceClient client = WebApplication.iceClient
                        .settingProxy(serverName).
                        settingReq(getMemoryDEVID()+ "@" + WebApplication.DEVTYPE ,cls,method).
                        setPageInfo(page,count)
                        .setExtend(extend);

        if (json!=null){
            if (GsonUtils.checkJsonIsArray(json)) {
                String[] arrays = GsonUtils.jsonToJavaBean(json,String[].class);
                if (arrays != null) client.settingParam(arrays);
            }
            client.settingParam(json);
        }

        long time = System.currentTimeMillis();
        String resultJson = client.execute();
        time = System.currentTimeMillis() - time;
        if (time >= 1000){
            LLog.print("接口:\t"+ serverName+" , "+ cls+" , "+ method+ " , 第"+page+"页,共"+ count+"条"+
                    "\n参数:\t"+json+
                    "\n时间:\t"+time+" 毫秒 ");
        }
        return resultJson;
    }



    /** web页面加载完成 */


    @Override
    public Activity getNativeActivity(){
        if (nativeActivityInterfaceRef.get()==null) return null;
        return nativeActivityInterfaceRef.get().getNativeActivity();
    }

    // 打开通讯
    @Override
    public void connectIceIM() {
        isImServerAcceptStart = true;
        if (nativeActivityInterfaceRef.get() == null) return;
        nativeActivityInterfaceRef.get().connectIceIM();
    }


    /** 清理缓存 */
    @Override
    public void clearCache(){
        if (nativeActivityInterfaceRef.get() == null) return;
        nativeActivityInterfaceRef.get().clearCache();
    }

    // JS页面加载完成
    @Override
    public void onJSPageInitialization() {
        if (isJsPageLoadComplete) return;
        LLog.print(this + " 存在本地协议的JS页面初始化完成通知" );

        if (nativeActivityInterfaceRef.get() == null) return;
        nativeActivityInterfaceRef.get().onJSPageInitialization();
        isJsPageLoadComplete = true;
        checkUserCache();// 检查用户本地缓存一致性
        connectIceIM();// 启动长连接
    }

    private void checkUserCache() {
        LLog.print(this + " 检查本地用户缓存一致性 ");
        // 首次进入页面,判断本地是否存在用户信息
        // 存在对比服务器是否相同,不相同通知JS用户被强制登出
        int compidLocal = getCurrentDevCompanyID(false, null);
        int compidRemote = getCurrentDevCompanyID(true, WebApplication.iceClient);
        if (compidLocal > 0 && compidLocal!=compidRemote){
            forceLogout();// 强制退出
        }

    }


    @Override
    public void onIndexPageShowBefore() {
        if (nativeActivityInterfaceRef.get() == null) return;
        nativeActivityInterfaceRef.get().onIndexPageShowBefore();
    }

    // 调用JS方法,前提: 需要JS注册接口
    @Override
    public void callJsFunction(String funName, String data, JSResponseCallback callback){
        if (nativeActivityInterfaceRef.get() == null || !isJsPageLoadComplete) return;
        nativeActivityInterfaceRef.get().callJsFunction(funName,data,callback);
    }

    /***************************************************************************************************/

    /** 推送消息 */
    public void pushMessageToJs(final String message){
        callJsFunction("communicationSysReceive",message, null);
    }

    /** 消息通知栏点击进入 */
    public void notifyEntryToJs(String path){
        if (path == null) path = "/message";
        callJsFunction("notifyEntry",path, null);
    }

    /** 推送支付结果 */
    public void pushPaySuccessMessageToJs(final String message){
        callJsFunction("communicationPayReceive",message, null);
    }

    /* 强制登出 */
    public void forceLogout() {
        callJsFunction("logoutHandle",null, null);
    }

    /* 通知JS触发用户信息更新 */
    public void userChangeByJS(){
        callJsFunction("userChange",null, null);
    }

    /* 进入APP 传递剪切板的分享内容 */
    public void enterApp(String data) {
        callJsFunction("enterApp",data, null);
    }

}
