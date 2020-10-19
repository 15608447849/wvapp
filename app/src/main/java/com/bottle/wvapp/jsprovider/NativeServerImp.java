package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

import androidx.fragment.app.Fragment;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.R;
import com.bottle.wvapp.fragments.WebFragment;
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;

import java.lang.ref.SoftReference;
import java.util.Map;

import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtil;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp{
    private NativeServerImp(){ };

    public final static NativeMethodCallImp caller = new NativeMethodCallImp();//伴生类;
    public final static IBridgeImp iBridgeImp = new NativeBridgeImp();

    public static Application app;
    public static String DEVID = "unknown";
    public static final String DEVTYPE = "PHONE";
    public static IceClient ic;
    public static Ice.ObjectAdapter localAdapter;
    public static SharedPreferences sp ;
    public static SoftReference<Fragment> fragment;
    public static CommunicationServerImp notifyImp; //长连接
    public static IJsBridge iJsBridge;

    /* 文件服务地址信息 */
    private static Map fileServerMap;
    /* 服务器配置 */
    public static AppUploadConfig config;

    public static void bindApplication(Application application){
        if (application == null) throw new RuntimeException("应用初始化失败");
        app = application;
        sp = application.getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        //初始化设备标识
        DEVID = StringUtils.strMD5(AppUtils.devOnlyCode(app));
        LLog.print("当前设备唯一标识 : " + getDevSID());
        boolean isStart = startServer();
        if (isStart){
            //加载升级服务配置
            updateServerConfigJson();
            //更新线程执行
            UpdateVersionServerImp.execute(true);
        }
        //长连接检测线程
        checkCommunicationThread.setDaemon(true);
        checkCommunicationThread.start();
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

    /* 绑定web显示层碎片 */
    public static void buildFragment(Fragment fragmentInstance){
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
        fragment = new SoftReference<>(fragmentInstance);
    }

    public static void buildJSBridge(IJsBridge bridge) {
        iJsBridge  = bridge;
    }

    public static void reopenWeb() {
        if (fragment.get() !=null && fragment.get() instanceof WebFragment) {
            WebFragment wf = (WebFragment) fragment.get();
            wf.loadView();
        }
    }




    //更新启动页图片
    /*public static void updateLaunchImage(){
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                try {
                    String prev = getSpecFileUrl("downPrev");
                    while (prev == null ){
                        Thread.sleep(1000);
                        prev = getSpecFileUrl("downPrev");
                    }
                    String url = prev + "/" +LAUNCH_IMAGE;
                    HttpServerImp.downloadFile(url, new File(app.getFilesDir(),LAUNCH_IMAGE).toString(),null);
                    LLog.print("已更新启动页: " + url);
                } catch (Exception e) {
                    LLog.print("启动页更新失败: " + e);
                }
            }
        });
    }*/
    //获取启动页图片
    public static String getLaunchImage() {
        try {
            //网络获取流数据流
            String prev = getSpecFileUrl("downPrev");
            int tryIndex = 0;
            while (prev == null || tryIndex<10 ){
                Thread.sleep(100);
                prev = getSpecFileUrl("downPrev");
                tryIndex++;
            }
            return prev + "/launch.png";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //初始化文件服务地址
    private static boolean initFileServerInfo(){
        try {
            String json = ic.setServerAndRequest("userServer","FileInfoModule","fileServerInfo").execute();
            Map map = GsonUtils.jsonToJavaBean(json,Map.class);
            assert map != null;
            fileServerMap = (Map)map.get("data");
            LLog.print("获取基础文件服务信息:\n" + json);
            return true;
        } catch (Exception e) {
           LLog.print("获取基础文件服务信息失败:\n" + e);
        }
        return false;
    }

    /* 文件上传的地址 上传-upUrl 下载-downPrev 删除-deleteUrl */
    public static String getSpecFileUrl(String type){
        try {
            if (fileServerMap!=null){
                return String.valueOf(fileServerMap.get(type));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    //更新服务配置
    static void updateServerConfigJson(){
        String json = loadServerConfigJson(0,10);
        config  =  GsonUtils.jsonToJavaBean(json, AppUploadConfig.class);
        if (config == null) config = new AppUploadConfig();
        if (!StringUtils.isEmpty(config.apkLink) && !config.apkLink.startsWith("http")){
            config.apkLink = getSpecFileUrl("downPrev")+"/"+ config.apkLink;
        }
    }

    //加载服务器配置信息URL
    private static String loadServerConfigJson(int retry,int max) {
        try {
            String url = getSpecFileUrl("downPrev")+"/config.json";
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
    static String transfer(String serverName, String cls, String method, int page, int count, String extend, String json) {

        IceClient client =
                NativeServerImp.ic.settingProxy(serverName).
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
        if (time > 1000L){
            LLog.print("接口:\t"+ serverName+" , "+ cls+" "+ method+"\n参数:\t"+json+"\n时间:\t"+time+" 毫秒 ");
        }

        return resultJson;
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
            json = ic.setServerAndRequest(getDevSID(),"userServer","LoginRegistrationModule","appStoreInfo").execute();
            isNetwork = true;
        }

        if (json.equals("{\"code\":-1}")){
            return 0;
        }

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
    public static void threadWait(){
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
                    if (compid == 0) throw new IllegalStateException("当前用户未登录");
                    if (notifyImp == null) notifyImp = new CommunicationServerImp();
                    if(checkCommunication(compid+"")) return;
                    InterfacesPrx prx = ic.settingProxy("order2Service" + getOrderServerNo()+"_1").getProxy();
                    notifyImp.identity = new Ice.Identity(compid+"","android");
                    localAdapter.add(notifyImp,notifyImp.identity );
                    prx.ice_getConnection().setAdapter(localAdapter);
                    prx.online( notifyImp.identity);
                    notifyImp.online = true;
                    LLog.print("IM 已连接服务器");
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

    //长连接检测线程
    private static final Thread checkCommunicationThread = new Thread(){
        @Override
        public void run() {
            while (true){
                try {
                    Thread.sleep(10 * 1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                communication("start");
            }
        }
    };


    //检查长连接是否有效
    private static boolean checkCommunication(String compid) {
        try {
            if (notifyImp!=null && notifyImp.online){
                notifyImp.ice_ping();
                if (notifyImp.identity.name.equals(compid)){
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (notifyImp!=null) notifyImp.online = false;
        }
        return false;
    }



    /**
     * app支付
     * json = { orderno=订单号,paytype=付款方式,flag客户端类型0 web,1 app }
     */
    public static Map payHandle(String json,String payType){

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
    public static Activity payPrevHandle(){
        if (fragment.get() == null) return null;
        final Activity activity = fragment.get().getActivity();
        if (activity==null) return null;
        final boolean isConnState = (notifyImp!=null && notifyImp.online);
        LLog.print("连接情况： " + isConnState );
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.toast(activity,"支付环境"+ (isConnState?"正确,可以支付":"错误,请稍后尝试"));
            }
        });

        //communication("start");//强制打开连接
        return isConnState ? activity : null;
    }

    /** 推送消息 */
    public static void pushMessageToJs(final String message){
        if (iJsBridge == null) return;
        iJsBridge.requestJs("communicationSysReceive",message, null);
    }

    /** 消息通知栏点击进入 */
    public static void notifyEntryToJs(String path){
        if (iJsBridge == null) return;
        if (path == null) path = "/message";
        iJsBridge.requestJs("notifyEntry",path, null);
    }

    /** 推送支付结果 */
    public static void pushPaySuccessMessageToJs(final String message){
        if (iJsBridge == null) return;
        iJsBridge.requestJs("communicationPayReceive",message, null);
    }

    /* 强制登出 */
    public static void forceLogout() {
        final Fragment fragment = NativeServerImp.fragment.get();
        if (fragment == null) return;

        final Activity activity = NativeServerImp.fragment.get().getActivity();
        if (activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //刷新首页
                if (fragment instanceof WebFragment){
                    WebFragment webFragment = (WebFragment) fragment;
                    webFragment.loadView();
                }

                DialogUtil.build(activity,
                        "安全提示",
                        "尊敬的用户∶\n\t\t\t\t您的账号已在其他设备登录,如非本人操作请联系您的商务经理或致电客服热线。",
                        R.drawable.ic_update_version,
                        "确定",
                        null,
                        null,
                        0,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

            }
        });

//        iJsBridge.requestJs("logoutHandle",null, null);
    }

}
