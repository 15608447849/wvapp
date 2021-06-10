package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.activitys.BaseActivity;
import com.bottle.wvapp.app.BaseResult;
import com.bottle.wvapp.app.MapDataResult;
import com.bottle.wvapp.services.IMService;
import com.google.gson.reflect.TypeToken;
import com.onek.client.IceClient;

import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Objects;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static com.bottle.wvapp.app.ApplicationDevInfo.getMemDevToken;
import static com.bottle.wvapp.app.BusinessData.getOrderServerNo;
import static com.bottle.wvapp.app.BusinessData.getCurrentDevCompanyID;
import static com.bottle.wvapp.jsprovider.HttpServerImp.startUploadThread;


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

    /* 判断页面是否加载完毕 */
    private static boolean isPageLoadComplete = false;

    /* 首次进入执行版本更新 */
    private static boolean executeVersionUpdate = false;

    public static void init(Application application){
        //初始化ice连接
        client.startCommunication();
        // 启动文件上传
        startUploadThread();
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
        // 触发应用更新
       if (!executeVersionUpdate) {
           UpdateVersionServerImp.execute(activity,false);
           executeVersionUpdate = true;
       }
    }

    /* 解绑web展示层 */
    public static void unbindActivity(){
        activityRef = null;
        isPageLoadComplete = false;
    }

    //转发ICE
    static String transfer(final String serverName,final String cls, final String method, final int page, final int count,final String extend,final String json) {
        IceClient client =
                NativeServerImp.client
                        .settingProxy(serverName).
                        settingReq(getMemDevToken(),cls,method).
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
        if (time > 2000L){
            LLog.print("接口:\t"+ serverName+" , "+ cls+" , "+ method+ " , 第"+page+"页,共"+ count+"条"+
                    "\n参数:\t"+json+
                    "\n时间:\t"+time+" 毫秒 ");
        }
        return resultJson;
    }

    /** web页面加载完成 */
    public static void webPageLoadComplete(String url) {
        //LLog.print("JS页面加载完成通知: "+ url + " , isPageLoadComplete = "+ isPageLoadComplete);
        if (isPageLoadComplete) return;
        // 设置进度完成
        JSUtils.progressHandler(url,100,true);

        //首次进入页面,判断本地是否存在用户信息, 存在对比服务器是否相同,不相同通知JS用户被强制登出
        int compidLocl = getCurrentDevCompanyID(true,client);

        if (compidLocl > 0 && compidLocl!= getCurrentDevCompanyID(true,client)){
            forceLogout();
        }
        BaseActivity activity = getBaseActivity();
        if (activity!=null){
            // 打开通讯
            Intent intent = new Intent(activity, IMService.class);
            activity.startService(intent);
        }

        isPageLoadComplete = true;
    }

    /**
     * app支付
     * json = { orderno=订单号, paytype=付款方式, flag客户端类型 0 web,1 app }
     */
    static synchronized Map<String,String> payHandle(String json, String payType){
        try{
            int companyID = getCurrentDevCompanyID(false,null);

            if ( companyID == 0) throw new IllegalArgumentException("登录信息异常");
            Map<String,Object> map  = GsonUtils.string2Map(json);
            if (map == null || map.get("orderno") == null) throw new IllegalArgumentException("支付订单号不正确");
            map.put("paytype",payType);
            map.put("flag",1);
            json = GsonUtils.javaBeanToJson(map);

            LLog.print(companyID+ " 预支付请求: " + json);
            json = transfer("order2Server"+getOrderServerNo(companyID),
                    "PayModule","prePay",
                    0,0,null,
                    json);
            LLog.print(companyID+ " 预支付结果: " + json);

            MapDataResult<String,String> result =
                    GsonUtils.jsonToJavaBean(json, new TypeToken<MapDataResult<String,String>>(){}.getType());

            if (result == null || result.data == null) throw new IllegalArgumentException("无法获取预支付信息");

            return result.data;
        }catch (final Exception e){
            final BaseActivity activity = getBaseActivity();
            if (activity!=null){
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AppUtils.toastLong(activity, Objects.requireNonNull(e.getMessage()));
                    }
                });
            }
        }
        return null;
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

    /* 进入APP */
    public static void enterApp(String data) {
//        LLog.print("enterApp : " +  data);
        if (isPageLoadComplete) {
            iBridgeImp.callJsFunction("enterApp",data, null);
        }
    }

}
