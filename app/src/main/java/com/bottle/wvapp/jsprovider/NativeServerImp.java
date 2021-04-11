package com.bottle.wvapp.jsprovider;

import android.app.Application;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.activitys.BaseActivity;
import com.onek.client.IceClient;

import java.lang.ref.SoftReference;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static com.bottle.wvapp.app.ApplicationDevInfo.getMemDevToken;
import static com.bottle.wvapp.app.BusinessData.getOrderServerNo;
import static com.bottle.wvapp.app.BusinessData.refreshCompanyInfoAndOutput;
import static com.bottle.wvapp.jsprovider.HttpServerImp.downURLPrev;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp {

    private NativeServerImp(){ };

    /* 伴生类 */
    public final static NativeMethodCallImp caller = new NativeMethodCallImp();

    /* 类实例 */
    public final static NativeBridgeImp iBridgeImp = new NativeBridgeImp();

    /* ICE连接客户端 */
    public static IceClient client = new IceClient(BuildConfig._ICE_TAG,BuildConfig._ADDRESS,BuildConfig._ARGS);

    /* activity引用 */
    private static SoftReference<BaseActivity> activityRef;

    /* 服务器配置 */
    static AppUploadConfig config;

    /* 判断页面是否加载完毕 */
    private static boolean isPageLoadComplete = false;

    public static void init(Application application){
        //初始化ice连接
        client.startCommunication();
        // 初始化更新线程
        UpdateVersionServerImp.init(application);
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                //加载文件服务信息
                if (initFileServerInfo()){
                    // 加载服务配置
                    updateServerConfigJson();
                    // 异步触发更新APP
                    UpdateVersionServerImp.execute(true);
                }
            }
        });

    }



    public static BaseActivity getBaseActivity(){
        if (activityRef!=null){
            if (activityRef.get()!=null){
                return activityRef.get();
            }
        }
        return null;
    }




    /* 绑定web展示层 */
    public static void bindActivity(BaseActivity activity){
        isPageLoadComplete = false;
        activityRef = new SoftReference<>(activity);

        LLog.print(NativeServerImp.class.getSimpleName() + " 绑定 activity " + activity);
    }

    public static void unbindActivity(){
        activityRef = null;
        isPageLoadComplete = false;
//        LLog.print(NativeServerImp.class.getSimpleName() + " 解绑 activity");
    }


    //初始化文件服务地址
    private static boolean initFileServerInfo(){
        try {
            String json = client.setServerAndRequest("userServer","FileInfoModule","fileServerInfo").execute();

            Map map = GsonUtils.jsonToJavaBean(json,Map.class);

            assert map != null;

            HttpServerImp.initFileServerMap((Map)map.get("data"));

//            LLog.print("获取基础文件服务信息:\n" + json);
            return true;
        } catch (Exception e) {
           LLog.print("获取基础文件服务信息失败:\t" + e.getMessage());
        }
        return false;
    }

    //更新服务配置
    static void updateServerConfigJson(){
        String json = loadServerConfigJson(0,10);
        config  =  GsonUtils.jsonToJavaBean(json, AppUploadConfig.class);
        if (config == null) config = new AppUploadConfig();

        if (!StringUtils.isEmpty(config.apkLink)
                && (!config.apkLink.startsWith("http")
                && !config.apkLink.startsWith("https"))){
            config.apkLink = downURLPrev() + config.apkLink;
        }
    }

    //加载服务器配置信息URL
    private static String loadServerConfigJson(int retry,int max) {
        try {
            String url = downURLPrev()+"/config.json";
            LLog.print("当前次数: "+ retry+" 加载服务器配置信息URL: " + url);
            String json = HttpServerImp.text(url).trim().replaceAll("\\s*","");
            //LLog.print("获取服务器配置信息:\n"+json);
            return json;
        } catch (Exception e) {
            LLog.print("加载服务器配置信息失败:\n"+e);
            if (retry < max){
                return loadServerConfigJson(++retry,max);
            }
        }
        return null;
    }

    //转发ICE
    static String transfer(final String serverName,final String cls, final String method, final int page, final int count,final String extend,final String json) {
        IceClient client =
                NativeServerImp.client
                        .settingProxy(serverName).
                        settingReq(getMemDevToken(),cls,method).
                        setPageInfo(page,count);
        if (json!=null){
            String[] arrays = null;
            if (GsonUtils.checkJsonIsArray(json)) arrays = GsonUtils.jsonToJavaBean(json,String[].class);
            if (arrays != null) {
                client.settingParam(arrays);
            }
            client.settingParam(json);
        }
        client.setExtend(extend);
        long time = System.currentTimeMillis();
        String resultJson = client.execute();
        time = System.currentTimeMillis() - time;
        if (time > 2000L){
            LLog.print("接口:\t"+ serverName+" , "+ cls+" , "+ method+ " , 第"+page+"页,共"+ count+"条"+
                    "\n参数:\t"+json+
                    "\n时间:\t"+time+" 毫秒 ");
        }
        return resultJson;
    }

    //线程活动
    public static void threadNotify() {
       synchronized (caller){
           caller.notifyAll();
        }
    }

    //线程休眠
    static void threadWait(){
        synchronized (caller) {
            try { caller.wait(); } catch (InterruptedException ignored) { }
        }
    }

    /** web页面加载完成 */
    public static void webPageLoadComplete(String url) {
        LLog.print("JS页面加载完成通知: "+ url + " , isPageLoadComplete = "+ isPageLoadComplete);
        if (isPageLoadComplete) return;
        // 设置进度完成
        JSUtils.progressHandler(url,100,true);

        //首次进入,判断本地是否存在用户信息, 存在对比服务器是否相同,不相同通知JS用户被强制登出
        int compidLocl = refreshCompanyInfoAndOutput(true,client);

        if (compidLocl > 0 && compidLocl!= refreshCompanyInfoAndOutput(true,client)){
            forceLogout();
        }

        isPageLoadComplete = true;
    }

    /**
     * app支付
     * json = { orderno=订单号,paytype=付款方式,flag客户端类型 0 web,1 app }
     */
    static Map payHandle(String json, String payType){
        LLog.print("预支付参数: " + json);

        Map map  = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map == null) throw new IllegalArgumentException("支付订单号不正确");
        map.put("paytype",payType);
        map.put("flag",1);

        int compid = refreshCompanyInfoAndOutput(false,null);
        json = transfer("order2Server"+getOrderServerNo(compid),
                "PayModule","prePay",
                0,0,null,
                GsonUtils.javaBeanToJson(map));

        LLog.print("预支付结果: " + json);

        map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map == null) throw new IllegalArgumentException("无法获取预支付信息");
        if (map.get("data") !=null ){
            map = (Map) map.get("data");
        }
        return map;
    }

    /** 推送消息 */
    public static void pushMessageToJs(final String message){
        iBridgeImp.callJsFunction("communicationSysReceive",message, null);
    }

    /** 消息通知栏点击进入 */
    public static void notifyEntryToJs(String path){
        if (path == null) path = "/message";
        iBridgeImp.callJsFunction("notifyEntry",path, null);
    }

    /** 推送支付结果 */
    public static void pushPaySuccessMessageToJs(final String message){
        iBridgeImp.callJsFunction("communicationPayReceive",message, null);
    }

    /* 强制登出 */
    public static void forceLogout() {
        iBridgeImp.callJsFunction("logoutHandle",null, null);
    }

}
