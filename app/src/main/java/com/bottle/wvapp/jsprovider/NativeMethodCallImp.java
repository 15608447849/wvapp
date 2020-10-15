package com.bottle.wvapp.jsprovider;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;

import com.alipay.sdk.app.PayTask;
import com.bottle.wvapp.activitys.ScanActivity;
import com.bottle.wvapp.activitys.WebActivity;
import com.bottle.wvapp.tool.GlideLoader;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lee.bottle.lib.imagepick.ImagePicker;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static lee.bottle.lib.toolset.util.AppUtils.getVersionName;
import static lee.bottle.lib.toolset.util.StringUtils.mapToString;

/**
 * 提供给前端调用的本地方法
 */
public class NativeMethodCallImp {

    //图片选择结果集
    private ArrayList<String> imagePaths;
    private static int REQUEST_SELECT_IMAGES_CODE = 255;

    /** 打开图片选择器 */
    private String openImageSelect(){
        if (NativeServerImp.fragment.get() == null) throw new NullPointerException("fragment is null");
        String url = "";
        imagePaths = null;
        ImagePicker.getInstance()
                .setTitle("请选择图片")//设置标题
                .showCamera(true)//设置是否显示拍照按钮
                .showImage(true)//设置是否展示图片
                .showVideo(false)//设置是否展示视频
                .setSingleType(true)//设置图片视频不能同时选择
                .setMaxCount(1)//设置最大选择图片数目(默认为1，单选)
//                .setImagePaths(imagePaths)//保存上一次选择图片的状态，如果不需要可以忽略
                .setImageLoader(new GlideLoader(NativeServerImp.app.getApplicationContext()))//设置自定义图片加载器
                .start(NativeServerImp.fragment.get(),REQUEST_SELECT_IMAGES_CODE);
        //等待结果
        NativeServerImp.threadWait();
        if (imagePaths!=null && imagePaths.size()==1){
            url = "image://" + imagePaths.get(0);
        }
        return url;
    }

    /** 文件上传 */
    private String fileUpload(String json){
        HttpServerImp.JSUploadFile bean = GsonUtils.jsonToJavaBean(json, HttpServerImp.JSUploadFile.class);
        if (bean == null) return null;
        return HttpServerImp.updateFile(bean);
    }

    /** 拨号 */
    private void callPhone(String phone){
        if (NativeServerImp.fragment.get()==null) return;
        Activity activity = NativeServerImp.fragment.get().getActivity();
        if (activity!=null) AppUtils.callPhoneNo(activity,phone);
    }

    private String scanRes;
    /** 扫码 */
    private String scan(){
        Fragment f  = NativeServerImp.fragment.get();
        if (f == null) return "-1";
        Intent intent = new Intent(f.getContext(), ScanActivity.class);
        f.startActivityForResult(intent,ScanActivity.CONST.getSCAN_RESULT_CODE());
        //等待结果
        NativeServerImp.threadWait();
        return StringUtils.isEmpty(scanRes) ? "0" : scanRes;
    }

    /** 打开qq */
    private void openTel(String qq){
        String url="mqqwpa://im/chat?chat_type=wpa&uin="+qq;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        NativeServerImp.app.startActivity(i);
    }

    void onActivityResultHandle(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == REQUEST_SELECT_IMAGES_CODE) {
                imagePaths = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
            }else if (requestCode == ScanActivity.CONST.getSCAN_RESULT_CODE()){
                scanRes = data.getStringExtra( ScanActivity.CONST.getSCAN_RES());
            }
        }
    }

    /**版本更新*/
    private void versionUpdate(){
        UpdateVersionServerImp.execute(false);
    }

    /** 支付宝支付 */
    public int alipay(String json){
        Activity activity = NativeServerImp.payPrevHandle();
        if (activity == null) return -1;
        //获取支付信息
        Map map = NativeServerImp.payHandle(json,"alipay");
        LLog.print(GsonUtils.javaBeanToJson("尝试支付宝支付,后台结果： " + GsonUtils.javaBeanToJson(map)));
        final String orderInfo = mapToString(map);
        PayTask alipay = new PayTask(activity);
        //执行
        map = alipay.payV2(orderInfo,true);
        return  String.valueOf(map.get("resultStatus")).equals("9000") ? 0 : -1;
    }

    public IWXAPI wxapi;
    public int wxpayRes = -1;

    /** 微信支付 */
    public int wxpay(String json){
        wxpayRes = -1;
        Activity activity = NativeServerImp.payPrevHandle();
        if (activity == null) return wxpayRes;
        //获取支付信息 https://www.jianshu.com/p/84eac713f007
        Map map = NativeServerImp.payHandle(json,"wxpay");
        LLog.print(GsonUtils.javaBeanToJson("尝试微信支付,后台结果： " + GsonUtils.javaBeanToJson(map)));
        if(wxapi == null){
            wxapi = WXAPIFactory.createWXAPI(NativeServerImp.app,null);
            wxapi.registerApp(String.valueOf(map.get("appid")));
        }
        PayReq req = new PayReq();
        req.appId = String.valueOf(map.get("appid")); //微信appid
        req.partnerId = String.valueOf(map.get("partnerid")); //商户号
        req.prepayId = String.valueOf(map.get("prepayid"));//预支付交易会话ID
        req.packageValue = String.valueOf(map.get("package"));
        req.nonceStr = String.valueOf(map.get("noncestr"));//随机字符串
        req.timeStamp= String.valueOf(map.get("timestamp"));//时间戳
        req.sign = String.valueOf(map.get("sign"));//签名
        boolean isSuccess = wxapi.sendReq(req);
        if (!isSuccess) return wxpayRes;
        NativeServerImp.threadWait();
        return wxpayRes;
    }

    /** 打开/关闭连接 */
    public void communication(String type){
        NativeServerImp.communication(type);
    }

    /** 版本信息 */
    public String versionInfo(){
        return getVersionName(NativeServerImp.app)
                .replace("B", NativeServerImp.config.backVersion)
                .replace("W",NativeServerImp.sp.getInt("webPageVersion",0)+"");
    }

    /** 文件删除 */
    public String delFiles(String json){
        List<String> list = GsonUtils.json2List(json,String.class);
        LLog.print("删除文件: "+ list);
        return HttpServerImp.deleteFileOnRemoteServer(NativeServerImp.getSpecFileUrl("deleteUrl"),list);
    }

    /** 内置浏览器打开链接 */
    public void localBrowserOpenUrl(final String url) {
        LLog.print("浏览器打开链接: "+url);
        if (url.endsWith("?openType=outBrowser") || url.endsWith(".pdf") || url.endsWith(".png")){
            Intent intent=new Intent();
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("android.intent.action.VIEW");
            Uri content_url  = Uri.parse(url);
            intent.setData(content_url );
            NativeServerImp.app.startActivity(intent);
        }else{
            Intent intent = new Intent(NativeServerImp.fragment.get().getContext(), WebActivity.class);
            intent.putExtra("loadUrl",url);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            NativeServerImp.app.startActivity(intent);
        }

    }
}
