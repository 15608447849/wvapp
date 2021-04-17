package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import com.alipay.sdk.app.PayTask;
import com.bottle.wvapp.activitys.BaseActivity;
import com.bottle.wvapp.wxapi.WXPayEntryActivity;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.os.CrashHandler;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static android.Manifest.permission.CALL_PHONE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


import static com.bottle.wvapp.app.ApplicationDevInfo.getMemDevToken;
import static lee.bottle.lib.toolset.util.AppUtils.checkPermissionExist;
import static lee.bottle.lib.toolset.util.AppUtils.getVersionName;


/**
 * 提供给前端调用的本地方法
 */
public class NativeMethodCallImp{

    /* 支付结果 0 成功, -1 失败 */
    private volatile int currentPayResultCode = -1;

    /** 获取设备信息 */
    private String getDeviceInfoMap(){
        Map<String,String> devInfoMap = CrashHandler.getInstance().getDevInfoMap();

        HashMap<String,String> map = new HashMap<>();
        map.put("devCPU",devInfoMap.get("cpu"));
        map.put("devHardwareManufacturer",devInfoMap.get("硬件制造商"));
        map.put("devOS","android-"+devInfoMap.get("安卓系统版本号"));
        map.put("devModel",devInfoMap.get("型号"));
        map.put("devToken", getMemDevToken());// token
        LLog.print("JS获取本机设备信息: "+ map);
        return GsonUtils.javaBeanToJson(map);
    }

    /** 文件上传 */
    private String fileUpload(String json){
        HttpServerImp.UploadTask bean = GsonUtils.jsonToJavaBean(json, HttpServerImp.UploadTask.class);
        if (bean == null) return null;
        return HttpServerImp.updateFile(bean);
    }

    /** 拨号 */
    private void callPhone(String phone){
        Activity activity = NativeServerImp.getBaseActivity();
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
        Activity activity = NativeServerImp.getBaseActivity();
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
        Activity activity = NativeServerImp.getBaseActivity();
        if (activity==null) return "-x";
        return "-"+getVersionName(activity);
    }

    /** web页面加载完成 */
    public void pageLoadComplete(String url){
        NativeServerImp.webPageLoadComplete(url);
    }

    /** 版本更新*/
    private void versionUpdate(){
        Activity activity = NativeServerImp.getBaseActivity();
        if (activity!=null){
            UpdateVersionServerImp.execute(activity,true);
        }
    }

    /** 支付宝支付 */
    public int alipay(String json){
        currentPayResultCode = -1;
        Activity activity = NativeServerImp.getBaseActivity();
        if (activity != null){
            //获取支付信息
            Map<String,String> map = NativeServerImp.payHandle(json,"alipay");
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
                    if (map != null && map.get("resultStatus")!=null && map.get("resultStatus").equals("9000")) {
                        currentPayResultCode = 0;
                    }

                }catch (Exception ignored){ }
            }
        }
        return currentPayResultCode;
    }

    //线程休眠
    public void wxpayWait(){
        synchronized (WXPayEntryActivity.class) {
            try {
                WXPayEntryActivity.class.wait(3 * 60 * 1000L);
            } catch (InterruptedException ignored) { }
        }
    }

    //线程活动
    public void wxpayNotify(int resCode) {
        currentPayResultCode = resCode;
        synchronized (WXPayEntryActivity.class){
            WXPayEntryActivity.class.notifyAll();
        }
    }

    /** 微信支付 */
    public int wxpay(String json){
        currentPayResultCode = -1;
        Activity activity = NativeServerImp.getBaseActivity();
        if (activity != null) {
            //获取支付信息 https://www.jianshu.com/p/84eac713f007
            Map<String,String> map = NativeServerImp.payHandle(json,"wxpay");
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
        return currentPayResultCode;
    }

    /** 内置浏览器打开链接 */
    public void localBrowserOpenUrl(final String url) {
        final BaseActivity activity = NativeServerImp.getBaseActivity();
        if (activity==null) return;

        if (url.endsWith("/pages/index/index?.pdf")) return; // 移除BD平台

        LLog.print("JS请求打开链接: "+url);
        // 外部连接打开
        if (url.endsWith("?openType=outBrowser")){
            Intent intent=new Intent();
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("android.intent.action.VIEW");
            Uri content_url  = Uri.parse(url);
            intent.setData(content_url );
            activity.startActivity(intent);
            return;
        }

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.openWebPage(url);
            }
        });
    }

    /** 清理缓存 */
    public void clearCache(){
        final BaseActivity activity = NativeServerImp.getBaseActivity();
        if (activity==null) return;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.clearWeb(true);
            }
        });
    }

}
