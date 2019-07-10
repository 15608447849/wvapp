package com.bottle.wvapp.jsprovider;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lee.bottle.lib.toolset.http.HttpRequest;
import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.util.ImageUtils;

/**
 * Created by Leeping on 2019/6/5.
 * email: 793065165@qq.com
 * 提供给移动端的文件上传
 */
public class HttpServerImp {

    public static class JSFileItem{
        public String remotePath;
        public String fileName;
        public String uri;//上传的文件的本地路径
    }

     public static class JSUploadFile{
        public String url;
        public List<JSFileItem> files;

         public void addFile(JSFileItem item) {
             if (files == null) files = new ArrayList<>();
             files.add(item);
         }
     }

     public static boolean updateFile(Context context,JSFileItem... item){
         String url = NativeServerImp.INSTANCE.fileUploadUrl();
         if (url == null) return false;
         HttpServerImp.JSUploadFile jsUploadFile = new HttpServerImp.JSUploadFile();
         jsUploadFile.url = url;
         for (JSFileItem it : item){
             jsUploadFile.addFile(it);
         }
         //发送文件到服务器
         HttpServerImp.updateFile(context,jsUploadFile);
         return true;
     }

    //文件上传
    public static String updateFile(Context context, JSUploadFile bean){
        HttpRequest httpRequest = new HttpRequest();
        for (JSFileItem item : bean.files){
            try {
                Uri uri = Uri.parse(item.uri);
                String path = uri.getPath();
                File file = new File(path);
                if (file.exists()){
                    if ("image".equals(uri.getScheme())){
                        file = ImageUtils.imageCompression(context,file,1000);
                    }
                    httpRequest.addFile(file,item.remotePath ,item.fileName);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  httpRequest.fileUploadUrl(bean.url).getRespondContent();
    }

    public static File downloadFile(String url, String storePath, final HttpUtil.Callback callback){
        File file = new File(storePath);
       return new HttpRequest(){
           @Override
           public void onProgress(File file, long progress, long total) {
               if (callback!= null) callback.onProgress(file,progress,total);
           }
       }.download(url,file) ? file : null;
    }
}
