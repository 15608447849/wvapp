package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;

import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

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

    public final MethodCallImp caller = new MethodCallImp(this);

    public static Application app;

    private static SharedPreferences sp;

    private static String DEVID = "unknown@PHONE";

    private static IceClient ic = null;

    private static Ice.ObjectAdapter localAdapter;

    private IJsBridge jsBridgeImp;

    static SoftReference<Fragment> fragment;

    private CommunicationServerImp notifyImp; //长连接

    public static void bindApplication(Application application){
        app = application;
        sp = app.getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        initDEVID();
        initServerCommunication();
    }

    public static void initDEVID() {
        DEVID = AppUtils.devOnlyCode(app) + "@PHONE";
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
        try(InputStream in =
                    app.getAssets().open("server.properties")){
            Properties properties = new Properties();
            properties.load(in);
            String tag = properties.getProperty("tag");
            String address = properties.getProperty("address");
            String args = properties.getProperty("args");
            launchICE(new IceClient(tag,address,args));
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private NativeServerImp() { }

    public static final NativeServerImp INSTANCE = new NativeServerImp();

    public static NativeServerImp createServer(Fragment fragmentInstance){
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
        fragment = new SoftReference<>(fragmentInstance);
        return INSTANCE;
    }

    public static class ServerConfig{
        int serverVersion;
        String updateMessage;
        String apkLink;

        boolean isUseServerProp;
        Map<String,String> serverProp;

        public int webPageVersion;
        String zipLink;

        public RegisterCentre.Bean page;
    }
    public static ServerConfig config;
    //获取页面配置信息JSON
    public static RegisterCentre.Bean[] dynamicPageInformation(){
    if (app == null) throw new RuntimeException("应用初始化失败");
    //网络获取
    String json = serverConfigJson();
    //本地获取
    if (json == null) json = AppUtils.assetFileContentToText(app, "config.json");

    LLog.print(json);

    config =  GsonUtils.jsonToJavaBean(json,ServerConfig.class);
    if (config == null || config.page == null) throw new RuntimeException("没有配置信息");
    //检查服务器信息
    checkServerInfo();
    //解压缩html文件到缓存目录
    transferWebPageToDir();
    //更新线程执行
    UpdateVersionServerImp.execute(true);
    return new RegisterCentre.Bean[]{ config.page };
    }

    static void updateServerConfigJson(){
        //网络获取
        String json = serverConfigJson();
        if (json != null){
            config =  GsonUtils.jsonToJavaBean(json,ServerConfig.class);
        }
    }

    //转移html页面到缓存
    private static void transferWebPageToDir() {

        int webPageVersion = config.webPageVersion;

        //判断是否使用网络url
        if (webPageVersion <0 ) return;

        if (webPageVersion == 0){
            checkWebPageByAssets();
            return;
        }
        //判断是否已存在
        int recode = sp.getInt("webPageVersion",0);
        if (recode<webPageVersion){
            // 后台执行更新页面操作
            checkWebPageUpdate(true);
        }
    }

    private static void checkWebPageByAssets() {
        //直接解压缩access中的文件到缓存目录
        try(InputStream in = app.getAssets().open("dist.zip");){
            //解压缩
            boolean flag = AppUtils.unZipToFolder(in,app.getFilesDir());
            if (!flag) throw new IOException("无法解压缩页面资源");
        }catch (Exception e){
            e.printStackTrace();
        }
        sp.edit().remove("webPageVersion").apply();
    }

    private static boolean isUpdateWebPageIng = false;

    public static void checkWebPageUpdate(final boolean isAuto) {
        if (isUpdateWebPageIng) return;

        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                isUpdateWebPageIng = true;
                if (!isAuto) {
                    NativeServerImp.updateServerConfigJson();
                }
                //下载最新zip
                File file = HttpServerImp.downloadFile(config.zipLink, app.getCacheDir()+"/dist.zip",null);
                if (file != null) {
                    try(InputStream in = new FileInputStream(file)){
                        //解压缩
                        boolean flag = AppUtils.unZipToFolder(in,app.getFilesDir());
                        if (flag){
                            sp.edit().putInt("webPageVersion",config.webPageVersion).apply();
                        }else{
                            throw new IOException("web页面升级失败,无法解压缩页面资源");
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                        checkWebPageByAssets();
                    }
                }
                isUpdateWebPageIng = false;
            }
        });
    }

    private static String serverConfigJson() {
        String json = null;
        try {
            json = ic.setServerAndRequest("globalServer","WebAppModule","config").execute();
            if (StringUtils.isEmpty(json) || json.equals("null")) return null;
            if (json.contains("code") && json.contains("message")) return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private static void checkServerInfo() {
        if (config.isUseServerProp){
            Map<String,String> map = config.serverProp;
            String tag = map.get("tag");
            String address = map.get("address");
            String args = map.get("args");
            NativeServerImp.launchICE(new IceClient(tag,address,args));
        }
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

    private static class UserInfo {
        int compId;//企业ID
    }

    //获取公司码
    private int getCompId() {
        boolean isNetwork = false;
        String json = null;
        //对比服务器环境信息指纹
        if (checkServerEnv()){
            //尝试本地缓存获取
            json = jsBridgeImp.getData("USER_INFO");
        }
        if (StringUtils.isEmpty(json)){
            //网络获取
            json = ic.setServerAndRequest(DEVID,"userServer","LoginRegistrationModule","appStoreInfo").execute();
            isNetwork = true;
        }
        UserInfo info = GsonUtils.jsonToJavaBean(json,UserInfo.class);
        if (info!=null && info.compId>0) {
            if (isNetwork) jsBridgeImp.putData("USER_INFO",json);
            return info.compId;
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
    private String transfer(String serverName, String cls, String method,int page,int count,String json) {
        IceClient client =
                NativeServerImp.ic.settingProxy(serverName).
                        settingReq(DEVID,cls,method).
                        setPageInfo(page,count);
        if (json!=null){
            String[] arrays = null;
            if (GsonUtils.checkJsonIsArray(json)) arrays = GsonUtils.jsonToJavaBean(json,String[].class);
            if (arrays != null) client.settingParam(arrays);
            else client.settingParam(json);
        }
        return client.execute();
    }

    //根据企业码 获取 分库分表的订单服务的下标序列
    private static int getOrderServerNo(int compid){
        return compid / 8192 % 65535;
    }

    @Override
    public void setIJsBridge(IJsBridge bridge) {
        this.jsBridgeImp = bridge;
    }

    @Override
    public Object invoke(String methodName, String data) throws Exception{
        if (isDebug) LLog.print("本地方法: "+ methodName +" ,数据: "+ data );

        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名@分页页码@分页条数
            String temp = methodName.replace("ts:","");
            String[] args = temp.split("@");
            return transfer(args[0],args[1],args[2],Integer.parseInt(args[3]),Integer.parseInt(args[4]),data);
        }
        //反射调用方法
        if(data == null){
            Method m = caller.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            return m.invoke(caller);
        }else{
            Method m = caller.getClass().getDeclaredMethod(methodName,String.class);
            m.setAccessible(true);
            return m.invoke(caller,data);
        }
    }

    public void threadNotify() {
       synchronized (jsBridgeImp){
            jsBridgeImp.notify();
        }
    }

    void threadWait(){
        synchronized (jsBridgeImp) {
            try {
                jsBridgeImp.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        caller.onActivityResult(requestCode,resultCode,data);
        threadNotify();
    }

    /** 打开/关闭连接 */
    void communication(String type){
        if (type.equals("launchICE") && notifyImp == null){
            //获取用户信息
            int compid = getCompId();
            if (compid > 0){
                if (notifyImp == null){
                    notifyImp = new CommunicationServerImp(this);
                }
                try {
                    if(checkCommunication()) return;
                    InterfacesPrx prx = ic.settingProxy("orderServer" + getOrderServerNo(compid)).getProxy();
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
        }else if (type.equals("close") && notifyImp !=null){
            notifyImp = null;
            localAdapter.remove(notifyImp.identity);
        }
    }

    //检查长连接是否有效
    private boolean checkCommunication() {
        try {
            if (notifyImp.online){
                notifyImp.ice_ping();
                return true;
            }
        } catch (Exception e) {
            notifyImp.online = false;
            e.printStackTrace();
        }
        return false;
    }

    /** 推送消息 */
    void pushMessageToJs(final String message){
        jsBridgeImp.requestJs("communicationSysReceive",message, null);
    }

    /** 推送支付结果 */
    void pushPaySuccessMessageToJs(final String message){
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
        json = transfer("orderServer"+getOrderServerNo(getCompId()),"PayModule","prePay",0,0,GsonUtils.javaBeanToJson(map));
        map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map.get("data") !=null ){
            map = (Map) map.get("data");
        }
        LLog.print(map);
        return map;
    }
    //支付前准备
    Activity payPrevHandle(){
        if (fragment.get() == null) return null;
        Activity activity = fragment.get().getActivity();
        if (activity==null) return null;
        communication("open");//强制打开连接
        return notifyImp.online? activity : null;
    }

}
