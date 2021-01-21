package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.activitys.BaseActivity;
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static com.bottle.wvapp.jsprovider.HttpServerImp.downURLPrev;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp{
    private NativeServerImp(){ };

    public final static NativeMethodCallImp caller = new NativeMethodCallImp();//伴生类;
    public final static IBridgeImp iBridgeImp = new NativeBridgeImp();

    public static Application app;
    static String DEVID = "unknown";
    static final String DEVTYPE = "PHONE";
    private static IceClient ic;
    private static Ice.ObjectAdapter localAdapter;
    private static CommunicationServerImp notifyImp; //长连接
    static SharedPreferences sp ;
    static SoftReference<BaseActivity> activityRef;
    /* js交互接口 */
    private static IJsBridge iJsBridge;
    /* 服务器配置 */
    static AppUploadConfig config;
    private static boolean isPageLoadComplete = false;

    public static void bindApplication(Application application){
        if (application == null) throw new RuntimeException("应用初始化失败");
        app = application;
        sp = application.getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        //初始化设备标识
        DEVID = StringUtils.strMD5(AppUtils.devOnlyCode(app));
        LLog.print("当前设备唯一标识 : " + getDevSID());
    }

    public static void launch(){
        boolean isStart = startServer();
        if (isStart){
            //加载升级服务配置
            updateServerConfigJson();
            //更新线程执行
            UpdateVersionServerImp.execute(true);
        }
    }

    private static boolean startServer() {
        //初始化ice连接
        String tag = BuildConfig._ICE_TAG;
        String address = BuildConfig._ADDRESS;
        String args = BuildConfig._ARGS;
        launchICE(new IceClient(tag,address,args));
        //加载文件服务信息
        return initFileServerInfo();
    }

    private static String getDevSID() {
        return DEVID+"@"+DEVTYPE;
    }

    //启动ICE客户端
    private static void launchICE(IceClient client) {
        destroyICE();
        ic = client.startCommunication();
        localAdapter = ic.iceCommunication().createObjectAdapter("");
        localAdapter.activate();
    }

    private static void destroyICE() {
        if (localAdapter != null){
            localAdapter.deactivate();
            localAdapter.destroy();
        }
        if (ic != null) {
            ic.stopCommunication();
        }
    }

    /* 绑定web展示层 */
    public static void bindActivity(BaseActivity activity){
        if (activity!=null) activityRef = new SoftReference<>(activity);
    }

    static void buildJSBridge(IJsBridge bridge) {
        iJsBridge  = bridge;
    }

    //初始化文件服务地址
    private static boolean initFileServerInfo(){
        try {
            String json = ic.setServerAndRequest("userServer","FileInfoModule","fileServerInfo").execute();
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            assert map != null;

            HttpServerImp.initFileServerMap((Map)map.get("data"));

            LLog.print("获取基础文件服务信息:\n" + json);
            return true;
        } catch (Exception e) {
           LLog.print("获取基础文件服务信息失败:\n" + e);
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
            LLog.print("获取服务器配置信息JSON:\n"+json);
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
                NativeServerImp.ic
                        .settingProxy(serverName).
                        settingReq(getDevSID(),cls,method).
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

    //调用JS方法,前提: 需要JS注册接口
    private static void callJsFunction(String funName, String data, IJsBridge.JSCallback callback){
        if (iJsBridge!=null){
            iJsBridge.requestJs(funName,data,callback);
        }
    }

    //获取公司码
    static int refreshCompanyInfo(boolean passLocal) {
        boolean isNetwork = false;
        String json = null;
        //不跳过本地,并且检测服务器环境信息指纹
        if (iJsBridge!=null && !passLocal){
            //尝试本地缓存获取
            json = iJsBridge.getData("USER_INFO");
        }

        if (StringUtils.isEmpty(json)){
            //本地获取失败时->网络获取
            json = ic.setServerAndRequest(getDevSID(), "userServer","LoginRegistrationModule","appStoreInfo").execute();
            isNetwork = true;
        }

        DataResult result = GsonUtils.jsonToJavaBean(json,DataResult.class);
        if (result!=null && result.code == -1) return 0;

        if (isNetwork){
            LLog.print((passLocal?"禁止":"允许") + "本地获取数据"
                    + " ,数据来源: 服务器"
                    + " ,用户信息:\n\t" + json);
        }

        Map map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map!=null){
            Object _compId = map.get("compId");
            if (_compId==null) {
                //没有获取到客户信息
                if (iJsBridge!=null && isNetwork) iJsBridge.putData("USER_INFO",null);
                return 0;
            }
            int compId = GsonUtils.convertInt(_compId);
            if (compId > 0){
                if (iJsBridge!=null && isNetwork) iJsBridge.putData("USER_INFO",json);
                return compId;
            }
        }
        return 0;
    }

    //根据企业码 获取 分库分表的订单服务的下标序列
    private static int getOrderServerNo(){
        //获取用户公司码
        int compid = refreshCompanyInfo(false);
        return ( compid / 65535 )   % 8192;
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

    /** 打开/关闭连接 */
    static synchronized void communication(String type){
        try {
            if (type.equals("start")){
                try {
                    //获取用户公司码
                    int compid = refreshCompanyInfo(false);
                    if (compid == 0)  throw new IllegalStateException("当前用户未登录");
                    if (notifyImp == null) notifyImp = new CommunicationServerImp();
                    if(checkCommunication(compid)) return;
                    InterfacesPrx prx = ic.settingProxy("order2Service" + getOrderServerNo()+"_1").getProxy();
                    notifyImp.identity = new Ice.Identity(String.valueOf(compid),DEVTYPE);
                    localAdapter.add(notifyImp,notifyImp.identity );
                    prx.ice_getConnection().setAdapter(localAdapter);
                    prx.online( notifyImp.identity);
                    notifyImp.online = true;
                    LLog.print("IM 服务器 连接成功");
                } catch (Exception e) {
                    //LLog.print("IM错误: "+e);
                    type = "close";
                }
            }
            if (type.equals("close")){
                //断开本地长连接
                if (notifyImp !=null){
                    if (notifyImp.identity!=null) localAdapter.remove(notifyImp.identity);
                    notifyImp = null;
                    LLog.print("IM 服务器 断开连接");
                }
            }
        } catch (Exception e) {
            LLog.print("IM-communication-错误: "+e);
        }
    }

    //检查长连接是否有效
    private static boolean checkCommunication(int compid) {
        try {
            if (notifyImp!=null && notifyImp.online){
                notifyImp.ice_ping();
                if (notifyImp.identity.name.equals(String.valueOf(compid))){
                    return true;
                }
            }
        } catch (Exception e) {
            if (notifyImp!=null) notifyImp.online = false;
        }
        return false;
    }


    /** web页面加载完成 */
    public static void webPageLoadComplete(String url) {
        if (isPageLoadComplete) return;
        JSUtils.progressHandler(url,100,true);
        //首次进入,判断是否本地是否存在用户信息, 存在对比服务器是否相同,不相同通知JS用户被强制登出
        int compidLocl = refreshCompanyInfo(false);
        if (compidLocl > 0
                && compidLocl!=refreshCompanyInfo(true)){
            forceLogout();
        }
        //打开长连接监听
        Timer timer = ApplicationAbs.getApplicationObject(Timer.class);
        if (timer==null) return;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                communication("start");
            }
        },10 * 1000L,10 * 1000L);
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
        json = transfer("order2Server"+getOrderServerNo(),"PayModule","prePay",0,0,null, GsonUtils.javaBeanToJson(map));
        LLog.print("预支付结果: " + json);
        map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map == null) throw new IllegalArgumentException("无法获取预支付信息");
        if (map.get("data") !=null ){
            map = (Map) map.get("data");
        }
        return map;
    }

    //支付前准备
    static Activity payPrevHandle(){
        final Activity activity = activityRef.get();
        if (activity==null) return null;
        int compid = refreshCompanyInfo(false);
        final boolean isConnState = compid>0 && checkCommunication(compid);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.toast(activity,"支付环境"+ (isConnState?"正确,可以支付":"错误,请稍后尝试"));
            }
        });
        return isConnState ? activity : null;
    }

    /** 推送消息 */
    static void pushMessageToJs(final String message){
        callJsFunction("communicationSysReceive",message, null);
    }

    /** 消息通知栏点击进入 */
    public static void notifyEntryToJs(String path){
        if (path == null) path = "/message";
        callJsFunction("notifyEntry",path, null);
    }

    /** 推送支付结果 */
    static void pushPaySuccessMessageToJs(final String message){
        callJsFunction("communicationPayReceive",message, null);
    }

    /* 强制登出 */
    static void forceLogout() {
        LLog.print("用户登出提醒");
        callJsFunction("logoutHandle",null, null);
    }

    /** 监听短信验证码 */
    public static void sendSmsCodeToJS(String body) {
        if (body.contains("一块医药")){
            Pattern pattern = Pattern.compile("(\\d{4,6})");
            Matcher matcher = pattern.matcher(body);
            if (matcher.find()) {
                String code = matcher.group(0);
                callJsFunction("smsCodeReceive",code, null);
            }
        }
    }

}
