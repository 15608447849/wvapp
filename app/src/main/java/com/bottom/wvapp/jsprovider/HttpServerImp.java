package com.bottom.wvapp.jsprovider;

import android.net.Uri;

import java.io.File;
import java.util.List;

import lee.bottle.lib.toolset.http.HttpRequest;

/**
 * Created by Leeping on 2019/6/5.
 * email: 793065165@qq.com
 * 提供给移动端的文件上传
 */
class HttpServerImp {

    private static class JSFileItem{
        String remotePath;
        String fileName;
        String uri;
    }

     static class JSUploadFile{
        String url;
        List<JSFileItem> files;
    }

    //文件上传
    static String updateFile(JSUploadFile bean){
        HttpRequest httpRequest = new HttpRequest();
        for (JSFileItem item : bean.files){
            try {
                Uri uri = Uri.parse(item.uri);
                String path = uri.getPath();
                File file = new File(path);
                if (file.exists()){
                    httpRequest.addFile(file,item.remotePath ,item.fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  httpRequest.fileUploadUrl(bean.url).getRespondContent();
    }
}
