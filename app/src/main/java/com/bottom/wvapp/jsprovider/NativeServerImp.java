package com.bottom.wvapp.jsprovider;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.bottom.wvapp.tool.GlideLoader;
import com.onek.client.IceClient;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import lee.bottle.lib.imagepick.ImagePicker;
import lee.bottle.lib.singlepageframwork.use.RegisterCentre;
import lee.bottle.lib.toolset.jsbridge.IBridgeImp;
import lee.bottle.lib.toolset.jsbridge.IJsBridge;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static android.app.Activity.RESULT_OK;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class NativeServerImp implements IBridgeImp {

    private static int REQUEST_SELECT_IMAGES_CODE = 255;


    private static Application app;

    private static IceClient ic = null;

    private IJsBridge jsBridgeImp;

    private SoftReference<Fragment> fragment;

    public static void start(IceClient client) {
        if (ic == null){
            ic = client.startCommunication();
        }
    }

    public static void bindApplication(Application application){
        app = application;
    }

    public NativeServerImp(Fragment fragment) {
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
        this.fragment = new SoftReference<>(fragment);
    }

    //获取页面配置信息JSON
    public static RegisterCentre.Bean[] dynamicPageInformation(){
        if (app == null) throw new RuntimeException("应用初始化失败");
        //本地获取
        String json = AppUtils.assetFileContentToText(app.getApplicationContext(),"page.json");
        Log.e("初始化应用",json);
        //网络获取
//        String json = ic.setServerAndRequest("globalServer","WebAppModule","pageInfo").execute();
        List<RegisterCentre.Bean> list = GsonUtils.json2List(json,RegisterCentre.Bean.class);
        if (list == null || list.size() == 0){
            //添加默认页面
            list = new ArrayList<>();
        }
        return list.toArray(new RegisterCentre.Bean[list.size()]);
    }

    @Override
    public void setIJsBridge(IJsBridge bridge) {
        this.jsBridgeImp = bridge;
    }

    @Override
    public Object invoke(String methodName, String data) throws Exception{
        LLog.print("本地方法: "+ methodName +" ,数据: "+ data );

        if (methodName.startsWith("ts:")){
            //转发协议  ts:服务名@类名@方法名
            String temp = methodName.replace("ts:","");
            String[] args = temp.split("@");
            return transfer(args[0],args[1],args[2],data);
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


    private void exeNotify() {
       synchronized (jsBridgeImp){
            jsBridgeImp.notify();
        }
    }
    private void exeWait(){
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
        if (requestCode == REQUEST_SELECT_IMAGES_CODE && resultCode == RESULT_OK) {
            imagePaths = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
        }
        exeNotify();
    }



    //转发
    private String transfer(String serverName, String cls, String method, String json) {
        String devid =AppUtils.devIMEI(app.getApplicationContext());
        LLog.print("设备ID = " + devid);
        return ic.settingProxy(serverName).settingReq(devid,cls,method).settingParam(json).execute();
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



}
