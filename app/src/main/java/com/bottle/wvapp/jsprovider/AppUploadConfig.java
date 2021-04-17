package com.bottle.wvapp.jsprovider;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

import static com.bottle.wvapp.jsprovider.HttpServerImp.downURLPrev;

/** 服务APP升级配置 */
public class AppUploadConfig {
    int serverVersion = 0;
    String updateMessage = "发现新版本,请更新!";
    String apkLink = "/drug.apk";
    int forceUpdate = 0; //是否强制更新

    // 加载服务器配置信息URL
    private static String _loadServerConfigJson(int retry,int max) {
        String url = downURLPrev()+"/config.json";
        try {
            LLog.print("加载服务器配置信息: " + url);
            String json = HttpServerImp.text(url).trim().replaceAll("\\s*","");
            //LLog.print("获取服务器配置信息:\n"+json);
            return json;
        } catch (Exception e) {
            LLog.print(retry+"/"+max+" 加载服务器配置信息失败,URL="+ downURLPrev() +" ,错误原因:\n"+e);
            if (retry < max){
                return _loadServerConfigJson(++retry,max);
            }
        }
        return null;
    }

    // 更新服务配置
    static AppUploadConfig load(){

        String json = _loadServerConfigJson(0,10);

        AppUploadConfig config  =  GsonUtils.jsonToJavaBean(json, AppUploadConfig.class);

        if (config == null) config = new AppUploadConfig();

        if (!StringUtils.isEmpty(config.apkLink)
                && (!config.apkLink.startsWith("http")
                && !config.apkLink.startsWith("https"))){
            config.apkLink = downURLPrev() + config.apkLink;
        }

        return config;
    }

}
