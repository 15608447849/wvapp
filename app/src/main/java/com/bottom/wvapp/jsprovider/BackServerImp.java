package com.bottom.wvapp.jsprovider;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import lee.bottle.lib.toolset.util.GsonUtils;
import com.bottom.wvapp.jsbridge.IJsBridge;
import com.bottom.wvapp.jsbridge.ITransferServer;
import com.onek.client.IceClient;

import java.util.ArrayList;
import java.util.List;

import lee.bottle.lib.singlepageframwork.use.RegisterCentre;


/**
 * 提供给JS调用的后台接口
 * lzp
 */
public class BackServerImp implements ITransferServer {
    private static Application app;
    private static IceClient ic = null;

    private IJsBridge jsBridgeImp;

    public static void start(IceClient client) {
        if (ic == null){
            ic = client.startCommunication();
        }
    }

    public static void bindApplication(Application application){
        app = application;
    }

    public BackServerImp() {
        if (ic == null) throw new RuntimeException("ICE 连接未初始化");
    }

    public void settingBridge(IJsBridge jsBridge){
        this.jsBridgeImp = jsBridge;
    }

    //获取页面配置信息JSON
    public static RegisterCentre.Bean[] dynamicPageInformation(){
        String json = ic.setServerAndRequest("globalServer","WebAppModule","pageInfo").execute();
        List<RegisterCentre.Bean> list = GsonUtils.json2List(json,RegisterCentre.Bean.class);
        if (list == null || list.size() == 0){
            //添加默认页面
            list = new ArrayList<>();
        }
        return list.toArray(new RegisterCentre.Bean[list.size()]);
    }

    //转发
    @Override
    public String transfer(String serverName, String cls, String method, String json) {
        return ic.setServerAndRequest(serverName,cls,method).settingParam(json).execute();
    }

    //读取手机通讯录
    public List<String> readContacts(String json){
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

}
