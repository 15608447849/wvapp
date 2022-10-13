package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.alipay.sdk.app.PayTask;

import com.bottle.wvapp.activitys.WebActivity;
import com.bottle.wvapp.app.WebApplication;
import com.bottle.wvapp.beans.MapDataResult;
import com.bottle.wvapp.uptver.UpdateVersionServerImp;
import com.bottle.wvapp.wxapi.WXPayEntryActivity;
import com.google.gson.reflect.TypeToken;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lee.bottle.lib.toolset.http.FileServerClient;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.CrashHandler;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.DialogUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static android.Manifest.permission.CALL_PHONE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


import static com.bottle.wvapp.BuildConfig._WEB_HOME_URL;
import static com.bottle.wvapp.beans.BusinessData.getCurrentDevCompanyID;
import static com.bottle.wvapp.beans.BusinessData.getOrderServerNo;
import static lee.bottle.lib.toolset.os.ApplicationDevInfo.getMemoryDEVID;
import static lee.bottle.lib.toolset.util.AppUtils.checkPermissionExist;
import static lee.bottle.lib.toolset.util.AppUtils.getVersionName;
import static lee.bottle.lib.toolset.util.AppUtils.schemeJump;
import static lee.bottle.lib.toolset.util.AppUtils.schemeValid;
import static lee.bottle.lib.toolset.util.AppUtils.statusBarHeight;


/**
 * 提供给前端调用的本地方法
 */
public class NativeMethodCallImp {

    private final NativeServerImp nativeServerImp;

    protected NativeMethodCallImp(NativeServerImp nativeServerImp) {
        this.nativeServerImp = nativeServerImp;
        LLog.print("本地方法处理类: " + this+"  >>> "+ nativeServerImp );
    }

    /* 支付结果 0 成功, -1 失败 */
    private static volatile int currentPayResultCode = -1;

    /** 获取设备信息 */
    private String getDeviceInfoMap(){
        Map<String,String> devInfoMap = CrashHandler.getInstance().getDevInfoMap();

        HashMap<String,String> map = new HashMap<>();
        map.put("devCPU",devInfoMap.get("cpu"));
        map.put("devHardwareManufacturer",devInfoMap.get("硬件制造商"));
        map.put("devOS","android-"+devInfoMap.get("安卓系统版本号"));
        map.put("devModel",devInfoMap.get("型号"));
        map.put("devToken", getMemoryDEVID() + "@" + WebApplication.DEVTYPE);// token
        while (nativeServerImp.getNativeActivity() == null) ;
        map.put("statusBarHeight",String.valueOf(statusBarHeight(Objects.requireNonNull(nativeServerImp.getNativeActivity()))));
        LLog.print("JS获取本机设备信息: "+ map);
        return GsonUtils.javaBeanToJson(map);
    }

    /** 文件上传 */
    private String fileUpload(String json){
        FileServerClient.UploadTask bean = GsonUtils.jsonToJavaBean(json, FileServerClient.UploadTask.class);
        if (bean == null) return null;
        return FileServerClient.updateFile(bean);
    }

    /** 拨号 */
    private void callPhone(String phone){
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermissionExist(activity,CALL_PHONE)){
                activity.requestPermissions(new String[]{CALL_PHONE},127);
                return;
            }
        }
        AppUtils.callPhoneNo(activity,phone);
    }

    /** 打开qq */
    private void openTel(String qq){
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity==null) return;
        String url="mqqwpa://im/chat?chat_type=wpa&uin="+qq;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(i);
    }

    /** 打开/关闭连接 */
    public void communication(String type){
        // pass
    }

    /** 版本信息 */
    public String versionInfo(){
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity==null) return "-x";
        return "-"+getVersionName(activity);
    }

    private void phoneToast(final String msg) {
        final Activity activity = nativeServerImp.getNativeActivity();
        if (activity==null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppUtils.toastLong(activity, msg);
            }
        });
    }

    /** web页面初始化完成 */
    public void pageLoadComplete(String url){
        nativeServerImp.onJSPageInitialization();
    }

    /* web页面 展示首页 */
    public void onShowIndexBefore(){
        nativeServerImp.onIndexPageShowBefore();
    }


    /** 版本更新*/
    private void versionUpdate(){
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity!=null){
            UpdateVersionServerImp.checkVersion(activity,true);
        }
    }


    /**
     * app支付
     * json = { orderno=订单号, paytype=付款方式, flag客户端类型 0 web,1 app }
     */
    public synchronized Map<String,String> payHandle(String json, String payType){
        try{
            int companyID = getCurrentDevCompanyID(false,null);

            if ( companyID == 0) throw new IllegalArgumentException("登录信息异常");
            Map<String,Object> map  = GsonUtils.string2Map(json);
            if (map == null || map.get("orderno") == null) throw new IllegalArgumentException("支付订单号不正确");
            map.put("paytype",payType);
//            map.put("flag",1);
            if(map.get("flag") == null) map.put("flag",1);

            json = GsonUtils.javaBeanToJson(map);

            LLog.print(companyID+ " 预支付请求: " + json);
            json = nativeServerImp.queryICE("order2Server"+getOrderServerNo(companyID), "PayModule","prePay", 0,0,null, json);
            LLog.print(companyID+ " 预支付结果: " + json);

            MapDataResult<String,String> result =
                    GsonUtils.jsonToJavaBean(json, new TypeToken<MapDataResult<String,String>>(){}.getType());

            if (result == null || result.data == null) throw new IllegalArgumentException("无法获取预支付信息");

            return result.data;
        }catch (final Exception e){
            phoneToast(Objects.requireNonNull(e.getMessage()));
        }finally {
            nativeServerImp.connectIceIM();
        }
        return null;
    }


    // 微信支付 线程休眠 等待结果
    public static void wxpayWait(){
        synchronized (WXPayEntryActivity.class) {
            try {
                WXPayEntryActivity.class.wait(3 * 60 * 1000L);
            } catch (InterruptedException ignored) { }
        }
    }

    // 微信支付 线程执行 通知结果
    public static void wxpayNotify(int resCode) {
        currentPayResultCode = resCode;
        synchronized (WXPayEntryActivity.class){
            WXPayEntryActivity.class.notifyAll();
        }
    }


    /** 微信支付 */
    public int wxpay(String json){
        currentPayResultCode = -1;
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity != null) {
            //获取支付信息 https://www.jianshu.com/p/84eac713f007
            Map<String,String> map = payHandle(json,"wxpay");
            if (map != null) {

                IWXAPI wxapi = ApplicationAbs.getApplicationObject(IWXAPI.class);
                if (wxapi == null){
                    wxapi = WXAPIFactory.createWXAPI(activity,null);
                    wxapi.registerApp(map.get("appid"));
                    ApplicationAbs.putApplicationObject(IWXAPI.class,wxapi);
                }

                PayReq req = new PayReq();
                req.appId = map.get("appid"); //微信appid
                req.partnerId = map.get("partnerid"); //商户号
                req.prepayId = map.get("prepayid");//预支付交易会话ID
                req.packageValue = map.get("package");
                req.nonceStr = map.get("noncestr");//随机字符串
                req.timeStamp= map.get("timestamp");//时间戳
                req.sign = map.get("sign");//签名

                boolean isSuccess = wxapi.sendReq(req);

                if (isSuccess) {
                    wxpayWait();
                }
            }
        }
        LLog.print("微信付款结果 **************** "+ currentPayResultCode);
        return currentPayResultCode;
    }

    /** 支付宝支付 */
    public int alipay(String json){
        currentPayResultCode = -1;
        final Activity activity = nativeServerImp.getNativeActivity();
        if (activity != null){
            //获取支付信息
            Map<String,String> map = payHandle(json,"alipay");
            if (map != null) {
                //把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
                try{
                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, String> e : map.entrySet()) {
                        sb.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue() + "", "UTF-8")).append("&");
                    }
                    sb.deleteCharAt(sb.length() - 1);

                    //执行
                    PayTask alipay = new PayTask(activity);
                    map = alipay.payV2(sb.toString(),true);
                    if (map != null && map.get("resultStatus")!=null && Objects.equals(map.get("resultStatus"), "9000")) {
                        currentPayResultCode = 0;
                    }
                }catch (Exception ignored){ }
            }
        }
        LLog.print("支付宝付款结果 **************** "+ currentPayResultCode);
        return currentPayResultCode;
    }

    /** 易宝平台 */
    public int yeepay(String json){
        currentPayResultCode = -1;
        Activity activity = nativeServerImp.getNativeActivity();
        if (activity != null){
            //获取支付信息
            Map<String,String> map = payHandle(json,"yeepay");
            if (map != null) {

                String channel = map.get("channel");
                assert channel != null;

                if (channel.equals("ALIPAY")){
                    String qrcode = map.get("alipay_qr_url");
                    String jumpUrl = "alipays://platformapi/startapp?saId=10000007&qrcode="+qrcode;
                    LLog.print("易宝支付 支付宝方案 jumpUrl = "+ jumpUrl );

                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri contentUrl = Uri.parse(jumpUrl);
                    intent.setData(contentUrl);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }

                if (channel.equals("WECHAT")){
                    // {"code":1,"data":{"wx_appid":"wx8d45b8ae300bb465","wx_orgid":"gh_89d0b4f95a06","orderNo":"2209150013770034009","attr":"1663225395693@DRUG@order2Server0@PayModule@payCallBack@536894204","price":"118.0","subject":"一块医药"}}

                    String wx_appid = map.get("wx_appid");
                    String wx_orgid = map.get("wx_orgid");
//                    StringBuilder sb = new StringBuilder("pages/index/index?");
                    StringBuilder sb = new StringBuilder("pages/pay/pay?");
                    for (String k : map.keySet()){
                        String v = map.get(k);
                        sb.append(k).append("=").append(v).append("&");
                    }
                    String path = sb.deleteCharAt(sb.length()-1).toString();
                    LLog.print("易宝支付 微信小程序方案 path = "+path );

                    IWXAPI api = WXAPIFactory.createWXAPI(activity, wx_appid);
                    WXLaunchMiniProgram.Req req = new WXLaunchMiniProgram.Req();
                    req.miniprogramType = WXLaunchMiniProgram.Req.MINIPTOGRAM_TYPE_RELEASE;
                    req.userName = wx_orgid;
                    req.path =  path;
                    boolean isSuccess = api.sendReq(req);
                    if (isSuccess) {
                        wxpayWait();
                    }
                }
            }
        }
        LLog.print("易宝支付 当前支付结果返回值 currentPayResultCode = " + currentPayResultCode);
        return currentPayResultCode;
    }


    /** 内置浏览器打开链接 */
    public void localBrowserOpenUrl(final String url) {
        LLog.print("请求打开链接 = "+url);

        final Activity activity = nativeServerImp.getNativeActivity();
        if (activity==null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (url.endsWith("?openType=outBrowser")){
                    // 系统浏览器打开
                    String _url = url.replace("?openType=outBrowser","");
                    Intent intent = new Intent();
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    intent.setAction(Intent.ACTION_VIEW);
                    Uri content_url  = Uri.parse(_url);
                    intent.setData(content_url);
                    activity.startActivity(intent);
                }else {
                    // 二级 webview 打开
                    Intent intent = new Intent(activity, WebActivity.class);
                    intent.putExtra("url",url);
                    if (url.contains("isRollback=false")){
                        intent.putExtra("isRollback",false);
                    }
                    activity.startActivity(intent);

                }

            }
        });


    }

    /** 跳转第三方应用 */
    public void openPartyApplication(final String appName){
        final Activity activity = nativeServerImp.getNativeActivity();
        if (activity==null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Intent intent = null;
                if(appName.equals("sms")){
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setType("vnd.android-dir/mms-sms");
                }

                if (appName.equals("qq")){
                    intent = activity.getPackageManager().getLaunchIntentForPackage("com.tencent.mobileqq");
                }

                if (appName.equals("weixin")){
                    intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.setComponent(new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI"));
                }

                if (intent!=null){
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }

            }
        });
    }



    public void open_yqt_app(final String orderId_payorderUrl) {
        String[] arr = orderId_payorderUrl.split("&");
        String orderId  = arr[0];
        String payorderUrl  = arr[1];

        final Activity activity = nativeServerImp.getNativeActivity();
        if(activity == null) return;

         /*
         schema = yqt://eb-link.cn/loading/appcode-*-pbvAiJuSphiYAXWnKaUu?
         yqtOrderNo=20200402100047&schemaUrl=bbpay%3A%2F%2Fla.baibu.bbbuyer&packageCode=la.baibu.bbbuyer&appName=%E7%99%BE%E5%B8%83

        yqtOrderNo: 三方订单编号
        schemaUrl:三方app拉起参数（不可带参数）
        packageCode:三方app包名
        appName:三方app名称（需要转码）

        银企通包名: com.rthd.yqt
        */

//        final String apk_url = "https://file.zhanghc188.work/file/20210901/app-debug-yqt-uat-pl2-20210901V1.apk";
        final String apk_url = "http://upload.eb-link.cn/yqtapp.html";

        final String return_sechem = "onekdrug://wvapp_ccb_yqt:10000/return";
        final String packageName = "com.bottle.wvapp";
        final String appName = Uri.encode("一块医药");

        final String schema = String.format("yqt://eb222-link.cn/loading/%s?yqtOrderNo=%s&schemaUrl=%s&packageCode=%s&appName=%s",
                payorderUrl,orderId,return_sechem,packageName,appName);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isExistApp = schemeValid( activity,schema);

                LLog.print("跳转银企通:\n" + schema +"\n是否存在应用: "+ isExistApp);
                if(isExistApp){
                    schemeJump( activity,schema);
                }else {
                    DialogUtils.dialogSimple2(activity, "未安装应用'银企通'", "现在下载", new DialogUtils.Action0() {
                        @Override
                        public void onAction0() {
                            downloadApk(apk_url);
                        }
                    }, "我知道了", null).show();
                }
            }
        });
    }


    // 下载apk
    public void downloadApk(final String url){
        final Activity activity = nativeServerImp.getNativeActivity();
        if(activity == null) return;

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                if (intent.resolveActivity(activity.getPackageManager()) != null) {
                    activity.startActivity(Intent.createChooser(intent, "请选择浏览器"));
                } else {
                    AppUtils.toastLong(activity,"下载失败 "+url);
                }
            }
        });

    }

    /*
    * 配置状态栏背景颜色和字体颜色及状态栏
    * 'bgcolor': 背景颜色 RPG
    * 'isLightColor': true 字体白色 false：字体黑色
    * noStatusBarPage 布局延伸至状态栏底部
    * */
    public void setStatusBar(String json){
        final Activity activity = nativeServerImp.getNativeActivity();
        if (activity == null ) return;

        HashMap<String,Object> map = GsonUtils.string2Map(json);
        if (map==null) return;

        final String isLightColor = String.valueOf(map.get("isLightColor"));
        final String bgColor = String.valueOf(map.get("bgcolor")) ;
        final String noStatusBarPage = String.valueOf(map.get("noStatusBarPage"));

//        LLog.print("状态栏 背景颜色: " + bgColor +" 文本颜色: "+ isLightColor + " 是否延伸: "+ noStatusBarPage);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Window window = activity.getWindow();
                View decorView = window.getDecorView();
                int curSystemUiVisibility = decorView.getSystemUiVisibility();
                WindowManager.LayoutParams layoutParams = window.getAttributes();
                View contentViewGroup = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
                //                        contentViewGroup.setFitsSystemWindows(true);

                if (noStatusBarPage.equals("true")){
                    curSystemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                    window.setStatusBarColor(Color.TRANSPARENT);
//                        LLog.print("延伸至状态栏 状态栏背景色透明 ");
                }else{
                    curSystemUiVisibility = 0;
//                        LLog.print("不延伸至状态栏");
                    // 设置状态栏背景颜色
                    window.setStatusBarColor(Color.parseColor(bgColor));
//                            LLog.print("状态栏背景色: "+ bgColor);
                }

                if (isLightColor.equals("true")){
                    curSystemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
//                        LLog.print("文本白色");
                }else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        curSystemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
//                            LLog.print("文本黑色 ");
                    }
                }
                decorView.setSystemUiVisibility( curSystemUiVisibility);
            }
        });

    }

    public void exitApplication(){
        System.exit(0);
    }

    /** 清理缓存 */
    public void clearCache(){
       nativeServerImp.clearCache();
    }

}
