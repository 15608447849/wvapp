package com.bottle.wvapp.jsprovider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
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
import com.onek.client.IceClient;
import com.onek.server.inf.InterfacesPrx;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import lee.bottle.lib.imagepick.ImagePicker;
import lee.bottle.lib.singlepageframwork.use.RegisterCentre;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static lee.bottle.lib.toolset.jsbridge.JSInterface.isDebug;
import static lee.bottle.lib.toolset.util.StringUtils.mapToString;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp implements IBridgeImp {

    private static int REQUEST_SELECT_IMAGES_CODE = 255;

    private CommunicationServerImp notifyImp;

    private static Application app;

    private static String DEVID = null;

    private static IceClient ic = null;

    private static Ice.ObjectAdapter localAdapter;

    private IJsBridge jsBridgeImp;

    static SoftReference<Fragment> fragment;

    public static void start(IceClient client) {
        if (ic == null){
            ic = client.startCommunication();
            localAdapter = ic.iceCommunication().createObjectAdapter("");
            localAdapter.activate();
        }
    }

    public static void bindApplication(Application application){
        app = application;
        DEVID = AppUtils.devIMEI(app.getApplicationContext()) + "@PHONE" ;
        settingServerInfo();
    }

    //设置,连接服务器
    private static void settingServerInfo() {
        try {
            Properties properties = new Properties();
            properties.load(app.getAssets().open("server.properties"));
            String tag = properties.getProperty("tag");
            String address = properties.getProperty("address");
            NativeServerImp.start(new IceClient(tag,address));
        } catch (Exception e) {
            e.printStackTrace();
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

    //获取页面配置信息JSON
    public static RegisterCentre.Bean[] dynamicPageInformation(){
        if (app == null) throw new RuntimeException("应用初始化失败");
        //本地获取
        String json = AppUtils.assetFileContentToText(app.getApplicationContext(),"page.json");
        //网络获取
//String json = ic.setServerAndRequest("globalServer","WebAppModule","pageInfo").execute();
        List<RegisterCentre.Bean> list = GsonUtils.json2List(json,RegisterCentre.Bean.class);
        if (list == null || list.size() == 0){
            //添加默认页面
            list = new ArrayList<>();
        }
        return list.toArray(new RegisterCentre.Bean[list.size()]);
    }

    //获取地区信息
    public static String areaJson(long areaCode){
        return  ic.setServerAndRequest("globalServer","WebAppModule","appAreaAll").setArrayParams(areaCode).execute();
    }

    //获取地区全名
    public static String getAreaFullName(long areaCode){
        return ic.setServerAndRequest("globalServer","CommonModule","getCompleteName").setArrayParams(areaCode).execute();
    }

    //转换地区码->全称
    public String convertAreaCodeToFullName(String areaCode){
        return getAreaFullName(Long.parseLong(areaCode));
    }

    private static class UserInfo {
        public int userId; //用户ID
        public long roleCode; //角色复合码
        public String phone; //手机号码
        public String userName;//姓名
        public int compId;//企业ID
    }

    //获取公司码
    private int getCompId() {
        String json = ic.setServerAndRequest(DEVID,"userServer","LoginRegistrationModule","appStoreInfo").execute();
        UserInfo info = GsonUtils.jsonToJavaBean(json,UserInfo.class);
        if (info!=null) return info.compId;
        return 0;
    }
    /**
     * 根据企业码 获取 分库分表的订单服务的下标序列
     */
    private static int getOrderServerNo(int compid){
        return compid / 8192 % 65535;
    }

    /********************************************************************************************************/
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
            Method m = this.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            return m.invoke(this);
        }else{
            Method m = this.getClass().getDeclaredMethod(methodName,String.class);
            m.setAccessible(true);
            return m.invoke(this,data);
        }
    }

    public void exeNotify() {
       synchronized (jsBridgeImp){
            jsBridgeImp.notify();
        }
    }

    public void exeWait(){
        synchronized (jsBridgeImp) {
            try {
                jsBridgeImp.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //************************************nativi 方法调用****************************************//

    //图片选择结果集
    private ArrayList<String> imagePaths;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LLog.print("onActivityResult",  requestCode,  resultCode, data );
        if (resultCode == RESULT_OK){
            if (requestCode == REQUEST_SELECT_IMAGES_CODE) {
                imagePaths = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
            }else if (requestCode == CitySelectActivity.CONST.getREQUEST_SELECT_AREA_CODE()) {
                area.code = data.getLongExtra( CitySelectActivity.CONST.getAREA_CODE(),0);
            }else if (requestCode == ScanActivity.CONST.getSCAN_RESULT_CODE()){
                scanRes = data.getStringExtra( ScanActivity.CONST.getSCAN_RES());
            }
        }

        exeNotify();
    }

    //转发
    private String transfer(String serverName, String cls, String method,int page,int count,String json) {

        IceClient client = NativeServerImp.ic.settingProxy(serverName).settingReq(DEVID,cls,method);
        client.setPageInfo(page,count);
        String[] arrays = null;
        if (GsonUtils.checkJsonIsArray(json)) {
            arrays = GsonUtils.jsonToJavaBean(json,String[].class);
        }
        if (arrays != null){
            client.settingParam(arrays);
        }else{
            client.settingParam(json);
        }
        return client.execute();
    }

    /** 读取手机通讯录 */
    private List<String> readContacts(){
        List<String> list = new ArrayList<>();
        ContentResolver resolver  = app.getApplicationContext().getContentResolver();
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

    /** 打开图片选择器 */
    private String openImageSelect(){
        if (fragment.get() == null) throw new NullPointerException("fragment is null");
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
                .setImageLoader(new GlideLoader(app.getApplicationContext()))//设置自定义图片加载器
                .start(fragment.get(),REQUEST_SELECT_IMAGES_CODE);

        //等待结果
        exeWait();
        if (imagePaths!=null && imagePaths.size()==1){
            url = "image://" + imagePaths.get(0);
        }
        return url;
    }

    private static class AreaBean{
        private long code;
        private String fullName = "";
    }
    private AreaBean area = new AreaBean();


    /** 文件上传*/
    private String fileUpload(String json){
        HttpServerImp.JSUploadFile bean = GsonUtils.jsonToJavaBean(json, HttpServerImp.JSUploadFile.class);
        if (bean == null) return null;
        return HttpServerImp.updateFile(app.getApplicationContext(),bean);
    }

    /** 打开地区选择器 */
    private AreaBean areaSelect(){
        area.code = 0; //重置
        if (fragment.get() == null) throw new NullPointerException("fragment is null");
        Intent intent = new Intent(fragment.get().getContext(), CitySelectActivity.class);
        fragment.get().startActivityForResult(intent, CitySelectActivity.CONST.getREQUEST_SELECT_AREA_CODE());
        //等待结果
        exeWait();
        //获取全名
        if (area.code > 0) area.fullName = getAreaFullName(area.code);
//        return areaCode;
        return  area;
    }

    /** 拨号 */
    private void callPhone(String phone){
        if (fragment.get()==null || fragment.get().getContext()==null) return;
        LLog.print("phone - " + phone);
        AppUtils.callPhoneNo(fragment.get().getContext(),phone);
    }



    /** 打开/关闭连接 */
    public void communication(String type){
        if (type.equals("start") && notifyImp == null){
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
                    AppUtils.toast(fragment.get().getActivity(),"推送服务有效");
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
            LLog.print("ping...error: " + e);
            notifyImp.online = false;
            e.printStackTrace();
        }
        return false;
    }

    /** 推送消息 */
    public void pushMessageToJs(final String message){
        jsBridgeImp.requestJs("communicationSysReceive",message, null);
    }
    /** 推送支付结果 */
    public void pushPaySuccessMessageToJs(final String message){
        jsBridgeImp.requestJs("communicationPayReceive",message, null);
    }


    private static class PayParam{
        String orderno; //订单号
        String afsano; //售后单号
    }

    /**
     * app支付
     * json = { orderno=订单号,paytype=付款方式,flag客户端类型0 web,1 app }
     */
    private Map payHandle(String json,String payType){
        PayParam param = GsonUtils.jsonToJavaBean(json,PayParam.class);
        Map map = new HashMap<>();
            map.put("orderno",param.orderno);
            if (!StringUtils.isEmpty(param.afsano)) map.put("afsano",param.afsano);
            map.put("paytype",payType);
            map.put("flag",1);
        json = transfer("orderServer"+getOrderServerNo(getCompId()),"PayModule","prePay",0,0,GsonUtils.javaBeanToJson(map));
        map = GsonUtils.jsonToJavaBean(json,Map.class);
        if (map.get("data") !=null ){
            map = (Map) map.get("data");
        }
        return map;
    }

    //支付前准备
    private Activity payPrevHandle(){
        if (fragment.get() == null) return null;
        Activity activity = fragment.get().getActivity();
        if (activity==null) return null;
        communication("open");//强制打开连接
        return notifyImp.online? activity : null;
    }

    /** 支付宝支付 */
    public int alipay(String json){
        {}
        Activity activity = payPrevHandle();
        if (activity == null) return -1;
        //获取支付信息
        Map map = payHandle(json,"alipay");
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
        Activity activity = payPrevHandle();
        if (activity == null) return -1;
        //获取支付信息 https://www.jianshu.com/p/84eac713f007
        Map map = payHandle(json,"wxpay");
        if (wxapi==null) wxapi = WXAPIFactory.createWXAPI(activity, map.get("appid").toString());
        PayReq req = new PayReq();
            req.appId = map.get("appid").toString(); //微信appid
            req.partnerId = map.get("partnerid").toString(); //商户号
            req.prepayId= map.get("prepayid").toString();//预支付交易会话ID
            req.packageValue = map.get("package").toString();
            req.nonceStr= map.get("noncestr").toString();//随机字符串
            req.timeStamp= map.get("timestamp").toString();//时间戳
            req.sign= map.get("sign").toString();//签名
        boolean isSuccess = wxapi.sendReq(req);
        if (!isSuccess) return -1;
        exeWait();
        return -1;
    }

    private String scanRes;

    public String scan(){
        Fragment f  = fragment.get();
        if (f == null) return "-1";
        Intent intent = new Intent(f.getContext(), ScanActivity.class);
        f.startActivityForResult(intent,ScanActivity.CONST.getSCAN_RESULT_CODE());
        //等待结果
        exeWait();
        return StringUtils.isEmpty(scanRes) ? "0" : scanRes;
    }

    public void openTel(String qq){
        String url="mqqwpa://im/chat?chat_type=wpa&uin="+qq;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        i.setFlags(FLAG_ACTIVITY_NEW_TASK);
        app.startActivity(i);
    }
}
