package com.bottle.wvapp.app;

import android.app.Application;
import android.content.SharedPreferences;
import com.onek.client.IceClient;
import lee.bottle.lib.toolset.jsbridge.JSInterface;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

public class BusinessData {

    private BusinessData (){ }

    private static Application app;

    public static void settingApplication(Application application){
       app = application;
    }

    //获取公司码
    public static int getCurrentDevCompanyID(boolean passLocal, IceClient iceClient) {
        if (app!=null){
            SharedPreferences sp = JSInterface.sharedStorage(app);
            if ( sp != null) {
                String devToken = ApplicationDevInfo.getShareDevToken(app);
                return _getCompanyIDByToken(sp,devToken,passLocal,iceClient);
            }
        }
        return 0;
    }

    private static int _getCompanyIDByToken(SharedPreferences sp, String devToken, boolean passLocal, IceClient iceClient) {
        // 是否通过后台获取
        boolean isNetwork = false;

        String json = null;

        // 不跳过本地,并且检测服务器环境信息指纹
        if (!passLocal){
            //尝试本地缓存获取
            json = sp.getString("USER_INFO",null);
            //LLog.print("本地\t公司信息:\t" + json);
        }

        if (StringUtils.isEmpty(json) && iceClient!=null){
            //本地不存在时->网络获取
            json = iceClient
                    .setServerAndRequest(devToken,
                    "userServer", "LoginRegistrationModule", "appStoreInfo")
                    .execute();
            isNetwork = true;
//                LLog.print("服务器\t公司信息:\t" + json);
        }


        BaseResult result = GsonUtils.jsonToJavaBean(json, BaseResult.class);

        if (result!=null && result.code!=-1 && result.compId>0) {
            if (isNetwork) {
                SharedPreferences.Editor editor = sp.edit();
                if (editor!=null){
                    editor.putString("USER_INFO",json);
                    editor.apply();
                }
            }
            return result.compId;
        }
        return 0;
    }

    //根据企业码 获取 分库分表的订单服务的下标序列
    public static int getOrderServerNo(int companyID){
        if (companyID == 0) return 0;
        return ( companyID / 65535 )   % 8192;
    }
}
