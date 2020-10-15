package com.bottle.wvapp.jsprovider;

import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import lee.bottle.lib.toolset.http.HttpRequest;
import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.util.ImageUtils;

/**
 * Created by Leeping on 2019/6/5.
 * email: 793065165@qq.com
 * 提供给移动端的文件上传
 */
public final class HttpServerImp {
     public static final class JSFileItem{
        public String remotePath;
        public String fileName;
        public String uri;//上传的文件的本地路径
    }

     public static final class JSUploadFile{
        public boolean isCompress = true;
        public boolean isLogo = true;
        public boolean isThumb = true;
        public String url;
        public List<JSFileItem> files;

         public void addFile(JSFileItem item) {
             if (files == null) files = new ArrayList<>();
             files.add(item);
         }
     }

     private static final LinkedList<HttpServerImp.JSUploadFile> waitQueue = new LinkedList<>();
     private static Thread uploadThread = null;

     //多个文件上传
     public static void updateFile(JSFileItem... item){
         HttpServerImp.JSUploadFile jsUploadFile = new HttpServerImp.JSUploadFile();
         for (JSFileItem it : item){
             jsUploadFile.addFile(it);
         }
//         加入上传队列
         waitQueue.add(jsUploadFile);
         if (uploadThread==null){
             uploadThread = new Thread(){
                 @Override
                 public void run() {
                     while (true){
                         if (waitQueue.size()>0){
                             HttpServerImp.JSUploadFile bean = waitQueue.removeFirst();
                             bean.url = NativeServerImp.getSpecFileUrl("upUrl");
                             if (bean.url == null){
                                 waitQueue.addLast(bean);
                             }else{
                                 String uploadResult = updateFile(bean);
                                 //LLog.print("文件上传结果 : "+ uploadResult);
                             }
                         }else{
                                 try {
                                     synchronized (waitQueue){
                                        waitQueue.wait();
                                     }
                                 } catch (InterruptedException ignored) { }
                         }
                     }
                 }
             };
             uploadThread.start();
         }
         synchronized (waitQueue){
             waitQueue.notifyAll();
         }
     }

    //文件上传
    public static String updateFile(JSUploadFile bean){
        HttpRequest httpRequest = new HttpRequest();
        for (JSFileItem item : bean.files){
            try {
                Uri uri = Uri.parse(item.uri);
                String path = uri.getPath();
                if (path!=null){
                    File file = new File(path);
                    if (file.exists()){
                        if ("image".equals(uri.getScheme())){
                            if (NativeServerImp.app!=null){
                                file = ImageUtils.imageCompression(NativeServerImp.app,file,500);
                                httpRequest.setCompress(bean.isCompress);//服务器压缩
                                httpRequest.setLogo(bean.isLogo);//图片水印
                                httpRequest.setThumb(bean.isThumb);//图片略缩图
                                httpRequest.setCompressLimitSieze(5*1024*1024L);
                            }
                        }
                        httpRequest.addFile(file,item.remotePath ,item.fileName);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  httpRequest.fileUploadUrl(bean.url).getRespondContent();
    }
    //文件下载
    static File downloadFile(String url, String storePath, final HttpUtil.Callback callback){
        File file = new File(storePath);
       return new HttpRequest(){
           @Override
           public void onProgress(File file, long progress, long total) {
               if (callback!= null) callback.onProgress(file,progress,total);
           }
       }.download(url,file) ? file : null;
    }
    static String text(String url) throws Exception{
        HttpRequest httpRequest = new HttpRequest().accessUrl(url);
        if (httpRequest.getException()!=null) throw httpRequest.getException();
        return httpRequest.getRespondContent();
    }
    //文件删除
    static String deleteFileOnRemoteServer(String url,List<String> pathList){
        List<String> paths = new ArrayList<>();
        for (String path : pathList){
            Uri uri = Uri.parse(path);
            if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())){
                paths.add(uri.getPath());
            }
        }
        if (paths.size() > 0){
            return new HttpRequest().deleteFile(url,paths).getRespondContent();
        }
        return null;
    }

}
