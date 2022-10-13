package lee.bottle.lib.webh5;

import android.net.Uri;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import lee.bottle.lib.toolset.http.FileServerClient;
import lee.bottle.lib.toolset.http.HttpUtils;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.os.ApplicationAbs;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.util.StringUtils;
import lee.bottle.lib.webh5.interfaces.WebResourceRequestI;

import static lee.bottle.lib.toolset.util.AppUtils.getBytesByFile;

/**
 * Created by Leeping on 2020/10/29.
 * email: 793065165@qq.com
 * 多媒体资源存储
 */
public class WebResourceCache implements WebResourceRequestI {

    private boolean isDebug;

    private static final long CLEAR_TIME = 24 * 60 * 60 * 1000L; // 一天

    private static final int cacheSize = (int) (Runtime.getRuntime().maxMemory()/8);

    private final LruCache<String,byte[]> resourceMemCache
            = new LruCache<String,byte[]>(cacheSize){
        private long totalSize = 0;
        @Override
        protected int sizeOf(@NonNull String key, @NonNull byte[] value) {
            totalSize+=value.length;

            if (isDebug) LLog.print("资源内存缓存 "+ key+" = "+ value.length +
                    " , 已处理总大小: "+ FileUtils.byteLength2StringShow(totalSize) +
                    " , 最大内存大小: "+ FileUtils.byteLength2StringShow(cacheSize) +
                    " , 实际存储大小: "+ FileUtils.byteLength2StringShow(size()) );

            return value.length;
        }
    };

    // 私有构造
    private WebResourceCache() {

        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                loadDiskCache();
            }
        });
    }

    private void loadDiskCache() {
        try{
            File dir = ApplicationAbs.getApplicationDIR("资源缓存");
            if (dir == null) return;

            File[] files = dir.listFiles();
            if (files!=null){
                for (File file : files){
                    byte[] cacheByte = getBytesByFile(file);
                    if (cacheByte!=null){
                        resourceMemCache.put(file.getName(),cacheByte);
                    }
                }
            }

        }catch (Exception ignored){
        }
    }

    private static final class H{
        private static final WebResourceCache INSTANCE = new WebResourceCache();
    }
    public static WebResourceCache getInstance(){
        return H.INSTANCE;
    }


    @Override
    public WebResourceResponse resourceIntercept(String url) {
//        if(url.startsWith("http://121.37.6.129:9999/media/drug/1649399503000001159/0.jpg")){
//            LLog.print("请求加载资源URL: "+ url);
//        }
//        LLog.print("请求加载资源URL: "+ url);
        Uri uri = Uri.parse(url);

        String scheme = uri.getScheme();
        if (scheme == null) return null;
        int endingA = url.lastIndexOf(".");
        int endingB = url.lastIndexOf("?");

        if (endingA == -1 || endingB == -1) return null;
//        LLog.print(endingA+" "+ endingB+" >> " + uri);

        //后缀
        final String endingStr = url.substring(endingA+1 ,  endingB>0 && endingB>endingA?  endingB : url.length());
//        LLog.print("请求加载资源URL: "+ url + " 后缀: "+ endingStr);

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

            }

            if (endingStr.equals("mp3")){
                mimeType = "audio/mpeg";
            }

            if (endingStr.equals("js")){
                mimeType = "application/javascript";
            }

            if (endingStr.equals("css")){
                mimeType = "text/css";
            }

            if (mimeType == null){
//                if (isDebug) LLog.print("[缓存] 无法缓存URL: "+ url);
                return null;
            }
//            if (isDebug) LLog.print("[缓存] 允许缓存URL: "+ url);


            md5 = StringUtils.strMD5(url);

            if (isDebug) LLog.print("请求加载资源URL: "+ url +" MD5: "+ md5);

            final File dir = ApplicationAbs.getApplicationDIR("资源缓存");
            if(dir == null) return null; //无法创建缓存目录

            resourceFile = new File(dir,md5);
            downloadLocalStorePath = resourceFile.getPath()+".TEMP";
        }

        if (resourceFile == null || !resourceFile.exists() || resourceFile.length()==0) {
            backDownload(url,downloadLocalStorePath);// 进入后台下载
            return null;
        }

        try {
            if (isDebug) LLog.print("请求加载资源URL: "+ url +" LOCAL: "+ resourceFile);
            //异步执行,读取文件
            final PipedOutputStream out = new PipedOutputStream();
            PipedInputStream inputStream = new PipedInputStream(out);
            final File _resourceFile = resourceFile;
            final String _url = url;
            final String _downloadLocalStorePath = downloadLocalStorePath;
            final String _md5 = md5;
            final boolean isDownload = endingB>0 && (System.currentTimeMillis() - _resourceFile.lastModified() > CLEAR_TIME);
            IOUtils.run(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (_md5!=null){
                            byte[] cacheBytes = resourceMemCache.get(_md5);
//                            LLog.print("内存获取> "+ _resourceFile +" 文件大小:"+FileUtils.byteLength2StringShow(_resourceFile.length()) + " 缓存大小: "+ FileUtils.byteLength2StringShow(cacheBytes.length));
                            if (cacheBytes==null) {
                                //通过本地文件获取,同时存入内存缓存
                                cacheBytes = getBytesByFile(_resourceFile);
                                if (cacheBytes!=null){
                                    resourceMemCache .put(_md5,cacheBytes);
                                }
                            }

                            if (cacheBytes!=null){
//                                LLog.print(_url +" 缓存获取资源");
                                out.write(cacheBytes, 0, cacheBytes.length);
                            }
                        }

//                        LLog.print(_url +" 本地文件获取资源");
//                        AppUtils.getLocalFileToOutputStream(readFile,out);
                    } catch (Exception e) {
                        LLog.print("无法读取资源文件("+_resourceFile+") 错误: " + e);
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
            LLog.print("缓存资源读取错误 : " + e);
        }
        return null;
    }

    private void backDownload(final String downloadResourceUrl, final String downloadLocalStorePath) {
        if (downloadResourceUrl!=null && downloadLocalStorePath!=null){
            //加入下载列表
            FileServerClient.addDownloadFileToQueue(new FileServerClient.DownloadTask(downloadResourceUrl,downloadLocalStorePath,new HttpUtils.CallbackAbs(){
                @Override
                public void onResult(HttpUtils.Response response) {
                    File temp = response.getData();
                    if (temp!=null && temp.exists() && temp.length()>0){
                        byte[] bytes = getBytesByFile(temp);
                        //重命名
                        String newFileName = temp.getName().replace(".TEMP","");
                        boolean isSuccess = temp.renameTo(new File(temp.getParentFile(),newFileName));
                        if (isSuccess){
                            if (bytes != null) {
                                resourceMemCache.put(newFileName,bytes);
                            }
                        }
                       if (isDebug) LLog.print("下载("+downloadResourceUrl+") 缓存资源("+temp+") 重命名 (" + newFileName+ ") "+ (isSuccess?"成功":"失败"));
                    }
                }
            }));
        }
    }
}
