package com.bottle.wvapp.jsprovider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import androidx.fragment.app.Fragment;

import com.alipay.sdk.app.PayTask;
import com.bottle.wvapp.activitys.CitySelectActivity;
import com.bottle.wvapp.activitys.ScanActivity;
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
public class MethodCallImp {

    private NativeServerImp nsi;

    public MethodCallImp(NativeServerImp nsi) {
        this.nsi = nsi;
    }

    /** 转换地区码->全称 */
    public String convertAreaCodeToFullName(String areaCode){
        return nsi.getAreaFullName(Long.parseLong(areaCode));
    }

    /** 读取手机通讯录
     * <uses-permission android:name="android.permission.READ_CONTACTS"/>
     * */
    private List<String> readContacts(){
        List<String> list = new ArrayList<>();
        ContentResolver resolver  = nsi.app.getContentResolver();
        //用于查询电话号码的URI
        Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        // 查询的字段
        String[] projection = {ContactsContract.CommonDataKinds.Phone._ID,//Id
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,//通讯录姓名
                ContactsContract.CommonDataKinds.Phone.DATA1, "sort_key",//通讯录手机号
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,//通讯录Id
                ContactsContract.CommonDataKinds.Phone.PHOTO_ID,//手机号Id
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY};
        @SuppressLint("Recycle") Cursor cursor = resolver.query(phoneUri, projection, null, null, null);
        assert cursor != null;
        while ((cursor.moveToNext())) {
            String name = cursor.getString(1);
            String phone = cursor.getString(2);
            list.add(name+":"+phone);
        }
        return list;
    }

    //图片选择结果集
    private ArrayList<String> imagePaths;

    private static int REQUEST_SELECT_IMAGES_CODE = 255;

    /** 打开图片选择器 */
    private String openImageSelect(){
        if (nsi.fragment.get() == null) throw new NullPointerException("fragment is null");
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
                .setImageLoader(new GlideLoader(nsi.app.getApplicationContext()))//设置自定义图片加载器
                .start(nsi.fragment.get(),REQUEST_SELECT_IMAGES_CODE);
        //等待结果
        nsi.threadWait();
        if (imagePaths!=null && imagePaths.size()==1){
            url = "image://" + imagePaths.get(0);
        }
        return url;
    }

    private static class AreaBean{
        private long code;
        private String fullName = "";
    }

    private final AreaBean area = new AreaBean();

    /** 打开地区选择器 */
    private AreaBean areaSelect(){
        area.code = 0; //重置
        area.fullName = "";
        if (nsi.fragment.get() == null) throw new NullPointerException("fragment is null");
        Intent intent = new Intent(nsi.fragment.get().getContext(), CitySelectActivity.class);
        nsi.fragment.get().startActivityForResult(intent, CitySelectActivity.CONST.getREQUEST_SELECT_AREA_CODE());
        //等待结果
       nsi.threadWait();
        //获取全名
        if (area.code > 0) area.fullName = nsi.getAreaFullName(area.code);
        return  area;
    }

    /** 文件上传*/
    private String fileUpload(String json){
        HttpServerImp.JSUploadFile bean = GsonUtils.jsonToJavaBean(json, HttpServerImp.JSUploadFile.class);
        if (bean == null) return null;
        return HttpServerImp.updateFile(nsi.app,bean);
    }

    /** 拨号 */
    private void callPhone(String phone){
        if (nsi.fragment.get()==null || nsi.fragment.get().getContext()==null) return;
        AppUtils.callPhoneNo(nsi.fragment.get().getActivity(),phone);
    }

    private String scanRes;

    /** 扫码 */
    private String scan(){
        Fragment f  = nsi.fragment.get();
        if (f == null) return "-1";
        Intent intent = new Intent(f.getContext(), ScanActivity.class);
        f.startActivityForResult(intent,ScanActivity.CONST.getSCAN_RESULT_CODE());
        //等待结果
        nsi.threadWait();
        return StringUtils.isEmpty(scanRes) ? "0" : scanRes;
    }


    /** 打开qq */
    private void openTel(String qq){
        String url="mqqwpa://im/chat?chat_type=wpa&uin="+qq;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        nsi.app.startActivity(i);
    }

    public void onActivityResultHandle(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == REQUEST_SELECT_IMAGES_CODE) {
                imagePaths = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
            }else if (requestCode == CitySelectActivity.CONST.getREQUEST_SELECT_AREA_CODE()) {
                area.code = data.getLongExtra( CitySelectActivity.CONST.getAREA_CODE(),0);
            }else if (requestCode == ScanActivity.CONST.getSCAN_RESULT_CODE()){
                scanRes = data.getStringExtra( ScanActivity.CONST.getSCAN_RES());
            }
        }
    }

    /**版本更新*/
    private void versionUpdate(){
        UpdateWebPageImp.transferWebPageToDir(false);
        UpdateVersionServerImp.execute(false);
    }

    /** 支付宝支付 */
    public int alipay(String json){
        Activity activity = nsi.payPrevHandle();
        if (activity == null) return -1;
        //获取支付信息
        Map map = nsi.payHandle(json,"alipay");
        LLog.print(GsonUtils.javaBeanToJson("尝试支付宝支付,后台结果： " + GsonUtils.javaBeanToJson(map)));
        final String orderInfo = mapToString(map);
        PayTask alipay = new PayTask(activity);
        //执行
        map = alipay.payV2(orderInfo,true);
        return  map.get("resultStatus").toString().equals("9000") ? 0 : -1;
    }

    public IWXAPI wxapi;
    public int wxpayRes = -1;

    /** 微信支付 */
    public int wxpay(String json){
        wxpayRes = -1;
        Activity activity = nsi.payPrevHandle();
        if (activity == null) return wxpayRes;
        //获取支付信息 https://www.jianshu.com/p/84eac713f007
        Map map = nsi.payHandle(json,"wxpay");
        LLog.print(GsonUtils.javaBeanToJson("尝试微信支付,后台结果： " + GsonUtils.javaBeanToJson(map)));
        if(wxapi == null){
            wxapi = WXAPIFactory.createWXAPI(nsi.app,null);
            wxapi.registerApp(map.get("appid").toString());
        }
        PayReq req = new PayReq();
        req.appId = map.get("appid").toString(); //微信appid
        req.partnerId = map.get("partnerid").toString(); //商户号
        req.prepayId= map.get("prepayid").toString();//预支付交易会话ID
        req.packageValue = map.get("package").toString();
        req.nonceStr= map.get("noncestr").toString();//随机字符串
        req.timeStamp= map.get("timestamp").toString();//时间戳
        req.sign= map.get("sign").toString();//签名
        boolean isSuccess = wxapi.sendReq(req);
        if (!isSuccess) return wxpayRes;
        nsi.threadWait();
        return wxpayRes;
    }

    /** 打开/关闭连接 */
    public void communication(String type){
        nsi.communication(type);
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
        return HttpServerImp.deleteFileOnRemoteServer(NativeServerImp.fileDeleteUrl(),list);
    }
}
