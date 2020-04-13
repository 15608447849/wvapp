package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.fragments.WebFragment;
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Map;

import lee.bottle.lib.singlepageframwork.use.RegisterCentre;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

import static lee.bottle.lib.toolset.jsbridge.JSInterface.isDebug;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp implements IBridgeImp {

    public final MethodCallImp caller;

    private NativeServerImp() {
        caller = new MethodCallImp(this);//伴生类
        checkCommunicationThread.setDaemon(true);
        checkCommunicationThread.start();//长连接检测线程
    }

    public static final NativeServerImp INSTANCE = new NativeServerImp();

    public static Application app;

    private static String DEVID = "unknown@PHONE";

    private static IceClient ic = null;

    private static Ice.ObjectAdapter localAdapter;

    private IJsBridge jsBridgeImp;

    static SharedPreferences sp ;

    static SoftReference<Fragment> fragment;

    private CommunicationServerImp notifyImp; //长连接

    public static void bindApplication(Application application){
        app = application;
        sp = NativeServerImp.app.getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        initDEVID();
        initServerCommunication();
    }

    public static void initDEVID() {
        DEVID = AppUtils.devOnlyCode(app) + "@PHONE";
//        LLog.print("当前设备唯一标识 : " + DEVID);
    }

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

    //设置,连接服务器
    private static void initServerCommunication() {
        String tag = BuildConfig._ICE_TAG;
        String address = BuildConfig._ADDRESS;
        String args = BuildConfig._ARGS;
        launchICE(new IceClient(tag,address,args));
    }

    public static NativeServerImp buildServer(Fragment fragmentInstance){
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
        fragment = new SoftReference<>(fragmentInstance);
        return INSTANCE;
    }

    public static void reopenWeb() {
        if (fragment.get() !=null && fragment.get() instanceof WebFragment) {
            WebFragment wf = (WebFragment) fragment.get();
            wf.loadView();
        }
    }
    private final static String LAUNCH_IMAGE = "launch.png";
    //获取启动页图片
    public static InputStream getLaunchImage() {
        try {
            File image = new File(app.getFilesDir(),LAUNCH_IMAGE);
            if (image.exists()){
                return new FileInputStream(image);
            }
            return app.getAssets().open(LAUNCH_IMAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //更新启动页图片
    public static void updateLaunchImage(){
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                try {
                    String url = fileDownloadUrl()+"/" +LAUNCH_IMAGE;
                    HttpServerImp.downloadFile(url, new File(app.getFilesDir(),LAUNCH_IMAGE).toString(),null);
                    LLog.print("已更新启动页: " + url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /* 强制登出 */
    public void forceLogout() {
        jsBridgeImp.requestJs("logoutHandle",null, null);
    }

    /** 服务配置 */
    public static class ServerConfig{
        String backVersion="v1.0.0";
        int serverVersion = 0;
        String updateMessage = "发现新版本,请更新";
        String apkLink = "";
        public int webPageVersion = 0;
        String zipLink = "";
        public RegisterCentre.Bean page =
                new RegisterCentre.Bean("com.bottle.wvapp.fragments.WebFragment","web","content")
                        .addParam("url","/dist/index.html");
        @Override
        public String toString() {
            return "{" +
                    "backVersion='" + backVersion + '\'' +
                    ", serverVersion=" + serverVersion +
                    ", updateMessage='" + updateMessage + '\'' +
                    ", apkLink='" + apkLink + '\'' +
                    ", webPageVersion=" + webPageVersion +
                    ", zipLink='" + zipLink + '\'' +
                    ", page=" + page +
                    '}';
        }
    }

    public static ServerConfig config;

    //获取页面配置信息JSON
    public static RegisterCentre.Bean[] dynamicPageInformation(){
    if (app == null) throw new RuntimeException("应用初始化失败");
    updateServerConfigJson();
    if (config == null || config.page == null) throw new RuntimeException("没有配置信息");
    UpdateWebPageImp.transferWebPageToDir(true);
    //更新线程执行
    UpdateVersionServerImp.execute(true);

    return new RegisterCentre.Bean[]{ config.page };
    }

    //更新服务配置
    static void updateServerConfigJson(){
        String json = loadServerConfigJson();
        config  =  GsonUtils.jsonToJavaBean(json,ServerConfig.class);
        if (config == null) config = new ServerConfig();
        if (!StringUtils.isEmpty(config.apkLink) && !config.apkLink.startsWith("http")){
            config.apkLink = fileDownloadUrl()+"/"+ config.apkLink;
        }
        if (!StringUtils.isEmpty(config.zipLink) && !config.zipLink.startsWith("http")){
            config.apkLink = fileDownloadUrl()+"/"+ config.zipLink;
        }
//        LLog.print("配置信息:\n"+config);
    }


    private static String loadServerConfigJson() {
        String json;
        try {
            //网络获取
            json = ic.setServerAndRequest("globalServer","WebAppModule","config").execute();
            json = json.trim().replaceAll("\\s*","");
            if (StringUtils.isEmpty(json) || json.equals("null")) throw new NullPointerException();
            if (json.contains("code") && json.contains("message")) throw new NullPointerException();
        } catch (Exception e) {
            LLog.print("加载服务器配置信息错误,没有服务器没有配置信息");
           json = null;
        }
        return json;
    }

    //获取地区信息
    public static String areaJson(long areaCode){
        return  ic.setServerAndRequest("globalServer","WebAppModule","appAreaAll").setArrayParams(areaCode).execute();
    }

    //获取地区全名
    public static String getAreaFullName(long areaCode){
        return ic.setServerAndRequest("globalServer","CommonModule","getCompleteName").setArrayParams(areaCode).execute();
    }

    //文件上传的地址
    public static String fileUploadUrl(){
        try {
            String json = ic.setServerAndRequest("globalServer","FileInfoModule","fileServerInfo").execute();
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            map = (Map)map.get("data");
            return map.get("upUrl").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //文件下载地址
    public static String fileDownloadUrl(){
        try {
            String json = ic.setServerAndRequest("globalServer","FileInfoModule","fileServerInfo").execute();
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            map = (Map)map.get("data");
            return map.get("downPrev").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    //文件删除地址
    public static String fileDeleteUrl(){
        try {
            String json = ic.setServerAndRequest("globalServer","FileInfoModule","fileServerInfo").execute();
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            map = (Map)map.get("data");
            return map.get("deleteUrl").toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //获取公司码
    public int getCompId(boolean passLocal) {
        if (jsBridgeImp != null){
            boolean isNetwork = false;
            String json = null;
            //不跳过本地,并且检测服务器环境信息指纹
            if (!passLocal && checkServerEnv()){
                //尝试本地缓存获取
                json = jsBridgeImp.getData("USER_INFO");
            }
            if (StringUtils.isEmpty(json)){
                //网络获取
                json = ic.setServerAndRequest(DEVID,"userServer","LoginRegistrationModule","appStoreInfo").execute();
//                LLog.print("网络获取用户信息完成: "+ json);
                isNetwork = true;
            }
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            if (map!=null){
                Object _compId = map.get("compId");
                Object _roleCode = map.get("roleCode");
                if (_compId!=null) {
                    int compId = GsonUtils.convertInt(_compId);
//                    LLog.print("公司码 "+ compId);
                    if (compId > 0){
                        if (isNetwork) jsBridgeImp.putData("USER_INFO",json);
                        int roleCode = GsonUtils.convertInt(_roleCode);
                        if ((roleCode & 2) > 0){
                            return compId;
                        }
                    }
                }
            }
        }
        return 0;
    }

    //效验环境
    private boolean checkServerEnv() {
        String recode = sp.getString("envInfo",null);
        String current = ic.getEnvId();
        if (current.equals(recode))return true;
        sp.edit().putString("envInfo",current).apply();
        return false;
    }

    //转发
    private String transfer(String serverName, String cls, String method,int page,int count,String extend,String json) {
        IceClient client =
                NativeServerImp.ic.settingProxy(serverName).
                        settingReq(DEVID,cls,method).
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
        return client.execute();
    }

    //根据企业码 获取 分库分表的订单服务的下标序列
    private static int getOrderServerNo(){
        //获取用户公司码
        int compid = INSTANCE.getCompId(false);
        return ( compid / 65535 )   % 8192;
    }

    @Override
    public void setIJsBridge(IJsBridge bridge) {
        this.jsBridgeImp = bridge;
        getCompId(true);
    }

    @Override
    public Object invoke(String methodName, String data) throws Exception{
        if (isDebug) LLog.print("本地方法: "+ methodName +" ,数据: "+ data );
        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名@分页页码@分页条数@扩展字段
            String temp = methodName.replace("ts:","");
            String[] args = temp.split("@");
            return transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),args[5],data);
        }
        Object val;
        //反射调用方法
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

    public void threadNotify() {
       synchronized (caller){
           caller.notify();
        }
    }

    void threadWait(){
        synchronized (caller) {
            try { caller.wait(); } catch (InterruptedException ignored) { }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        caller.onActivityResultHandle(requestCode,resultCode,data);
        threadNotify();
    }

    /** 打开/关闭连接 */
    void communication(String type){

        if (type.equals("start")){
            //获取用户公司码
            int compid = getCompId(false);
            if (compid > 0){
                if (notifyImp == null) notifyImp = new CommunicationServerImp(this);
                try {
                    if(checkCommunication(compid+"")) return;
                    LLog.print("order2Server" + getOrderServerNo());
                    InterfacesPrx prx = ic.settingProxy("order2Server" + getOrderServerNo()).getProxy();
                    notifyImp.identity = new Ice.Identity(compid+"","android");
                    localAdapter.add(notifyImp,notifyImp.identity );
                    prx.ice_getConnection().setAdapter(localAdapter);
                    prx.online( notifyImp.identity);
                    notifyImp.online = true;

                } catch (Exception e) {
                    e.printStackTrace();
                    notifyImp = null;
                }
            }
        }else if (type.equals("close")){
            if (notifyImp ==null) return;
            localAdapter.remove(notifyImp.identity);
            notifyImp = null;
        }
    }

    //长连接检测线程
    private Thread checkCommunicationThread = new Thread(){
        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(5 * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                communication("start");
                LLog.print("连接情况： " +  (notifyImp!=null && notifyImp.online));
            }
        }
    };


    //检查长连接是否有效
    private boolean checkCommunication(String compid) {
        try {
            if (notifyImp.online){
                notifyImp.ice_ping();
                if (notifyImp.identity.name.equals(compid)){
                    return true;
                }
            }
        } catch (Exception e) {
            notifyImp.online = false;
            e.printStackTrace();
        }
        return false;
    }

    /** 推送消息 */
    void pushMessageToJs(final String message){
        if (jsBridgeImp == null) return;
        jsBridgeImp.requestJs("communicationSysReceive",message, null);
    }

    /** 消息通知栏点击进入 */
    public void notifyEntryToJs(){
        if (jsBridgeImp == null) return;
        jsBridgeImp.requestJs("notifyEntry",null, null);
    }

    /** 推送支付结果 */
    void pushPaySuccessMessageToJs(final String message){
        if (jsBridgeImp == null) return;
        jsBridgeImp.requestJs("communicationPayReceive",message, null);
    }

    /**
     * app支付
     * json = { orderno=订单号,paytype=付款方式,flag客户端类型0 web,1 app }
     */
    Map payHandle(String json,String payType){

        Map map  = GsonUtils.jsonToJavaBean(json,Map.class);
        map.put("paytype",payType);
        map.put("flag",1);
        LLog.print("预支付信息: " + map);
        json = transfer("order2Server"+getOrderServerNo(),"PayModule","prePay",0,0,null,GsonUtils.javaBeanToJson(map));
        map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map.get("data") !=null ){
            map = (Map) map.get("data");
        }
        return map;
    }

    //支付前准备
    Activity payPrevHandle(){
        if (fragment.get() == null) return null;
        Activity activity = fragment.get().getActivity();
        if (activity==null) return null;
        communication("start");//强制打开连接
        return notifyImp.online? activity : null;
    }

}
