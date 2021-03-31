package com.bottle.wvapp.tool;

import android.net.Uri;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;

import com.bottle.wvapp.BuildConfig;
import com.bottle.wvapp.jsprovider.HttpServerImp;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import lee.bottle.lib.toolset.http.HttpUtil;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.StringUtils;
import lee.bottle.lib.toolset.web.JSUtils;

import static lee.bottle.lib.toolset.util.AppUtils.getBytesByFile;

/**
 * Created by Leeping on 2020/10/29.
 * email: 793065165@qq.com
 * 多媒体资源存储
 */
public class WebResourceCache extends TimerTask implements JSUtils.WebResourceRequestI {

    private static final long CLEAR_TIME = 3 * 60 * 60 * 1000L;

    private LruCache<String,byte[]> resourceMemCache = new LruCache<>(((int)Runtime.getRuntime().maxMemory())/1024/4);

    public WebResourceCache() {
        Timer timer = ApplicationAbs.getApplicationObject(Timer.class);
        if (timer==null) return;
        timer.schedule(this,1000L, CLEAR_TIME);
    }

    @Override
    public void run() {
        clearTimerStart();
    }

    private void clearTimerStart() {
//        LLog.print("缓存资源监控器 启动");
        try{
            File dir = ApplicationAbs.getApplicationDIR("资源缓存");
            if (dir != null){
                File[] files = dir.listFiles();
                for (File file : files){
                    String fn = file.getName();
                    if (System.currentTimeMillis() - file.lastModified() > CLEAR_TIME){
                        boolean delSuccess = file.delete();
                        if (!delSuccess){
                            LLog.print("删除缓存资源("+ fn +") 失败");
                        }
                    }else{
                        byte[] bytes = getBytesByFile(file);
                        if (bytes != null) resourceMemCache.put(fn,bytes);
                    }
                }
            }
        }catch (Exception ignored){
        }
    }

    @Override
    public WebResourceResponse resourceIntercept(String url) {
//        LLog.print("请求加载资源URL: "+ url);
        Uri uri = Uri.parse(url);

        String scheme = uri.getScheme();
        if (scheme == null) return null;
        int endingA = url.lastIndexOf(".");
        int endingB = url.lastIndexOf("?");

        //后缀
        final String endingStr = url.substring(endingA+1 ,  endingB>0 && endingB>endingA?  endingB : url.length());

        File resourceFile = null;
        String mimeType = null;
        String downloadLocalStorePath = null;
        String md5 = null;
        //判断协议类型
        if (scheme.equals("image")){
            //自定义图片协议头
            String path = uri.getPath();
            if (path == null) return null;
            mimeType = "image/*";
            resourceFile = new File(uri.getPath());
        }
        if (scheme.equals("http") || scheme.equals("https")){
            //网络资源
            if (endingStr.equals("png")
                    || endingStr.equals("jpg")
                    || endingStr.equals("jpeg")
                    || endingStr.equals("gif")
                    || endingStr.equals("ico")){
                mimeType = "image/*";
                if (endingB>0) {
                    url = url.substring(0,endingB);
                }
            }
            if (endingStr.equals("mp3")){
                mimeType = "audio/mpeg";
            }

//            if (endingStr.equals("js")){
//                mimeType = "application/javascript";
//            }
//            if (endingStr.equals("css")){
//                mimeType = "text/css";
//            }

            if (mimeType == null){
//                LLog.print("[缓存] 无法缓存URL: "+ url);
                return null;
            }
//                LLog.print("[缓存] 允许缓存URL: "+ url);

            md5 = StringUtils.strMD5(url);

            final File dir = ApplicationAbs.getApplicationDIR("资源缓存");
            if(dir == null || (!dir.exists() && !dir.mkdir())) return null;//无法创建缓存目录

            resourceFile = new File(dir,md5);
            downloadLocalStorePath = resourceFile.getPath()+".TEMP";
        }

        if (resourceFile == null || !resourceFile.exists() || resourceFile.length()==0) {
            backDownload(url,downloadLocalStorePath);
            return null;
        }

        try {
            final File readFile = resourceFile;
            //异步执行,读取文件
            final PipedOutputStream out = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(out);
            final String _url = url;
            final String _downloadLocalStorePath = downloadLocalStorePath;
            final String _md5 = md5;
            final boolean isDownload = endingB>0 && (System.currentTimeMillis() - readFile.lastModified() > 60*60*1000L);
            IOUtils.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (_md5!=null){
                            byte[] cacheBytes = resourceMemCache.get(_md5);
                            if (cacheBytes!=null){
//                                LLog.print(_url +" 缓存获取资源");
                                out.write(cacheBytes, 0, cacheBytes.length);
                                return;
                            }
                        }
//                        LLog.print(_url +" 本地文件获取资源");
                        AppUtils.getLocalFileToOutputStream(readFile,out);
                    } catch (Exception e) {
                        LLog.print("无法读取资源文件("+readFile+") 错误: " + e);
                    }finally {
                        try {
                            out.close();
                            if (isDownload) backDownload(_url,_downloadLocalStorePath);
                        } catch (IOException ignored) { }
                    }
                }
            });
            return new WebResourceResponse(mimeType,"UTF-8",inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void backDownload(final String downloadResourceUrl, final String downloadLocalStorePath) {
        if (downloadResourceUrl!=null && downloadLocalStorePath!=null){
            //加入下载列表
            HttpServerImp.addDownloadFileToQueue(new HttpServerImp.DownloadTask(downloadResourceUrl,downloadLocalStorePath,new HttpUtil.CallbackAbs(){
                @Override
                public void onResult(HttpUtil.Response response) {
                    File temp = response.getData();
                    if (temp!=null && temp.exists() && temp.length()>0){
                        byte[] bytes = getBytesByFile(temp);
                        //重命名
                        String newFileName = temp.getName().replace(".TEMP","");
                        boolean isSuccess = temp.renameTo(new File(temp.getParentFile(),newFileName));
                        if (isSuccess){
                            if (bytes != null) resourceMemCache.put(newFileName,bytes);
                        }
//                        LLog.print("下载("+downloadResourceUrl+") 缓存资源("+temp+") 重命名 (" + newFileName+ ") "+ (isSuccess?"成功":"失败"));
                    }
                }
            }));
        }
    }
}
