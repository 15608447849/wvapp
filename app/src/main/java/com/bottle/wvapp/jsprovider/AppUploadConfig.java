package com.bottle.wvapp.jsprovider;

/** 服务配置 */
public class AppUploadConfig {
    String backVersion="内置版本";
    int serverVersion = 0;
    String updateMessage = "发现新版本,请更新";
    String apkLink = "drug.apk";

    @Override
    public String toString() {
        return "{" +
                "backVersion='" + backVersion + '\'' +
                ", serverVersion=" + serverVersion +
                ", updateMessage='" + updateMessage + '\'' +
                ", apkLink='" + apkLink + '\'' +
                '}';
    }

}
