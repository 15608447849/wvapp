package lee.bottle.lib.toolset.http;

import android.content.Context;
import android.net.Uri;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.FileUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.TimeUtils;

/**
 * Created by Leeping on 2019/6/5.
 * email: 793065165@qq.com
 * 提供给移动端的文件上传/下载
 * 上传需要配合 FileServer后台
 */
public final class FileServerClient {

    private static String fileServerUrl = "http://127.0.0.1:80";

    public static void init(Context context,String fileServerUrl){
        FileServerClient.fileServerUrl = fileServerUrl;
        // 启动文件上传
        startUploadThread();
    }

    public static String downFileURL(String path){
        return fileServerUrl+path;
    }

    private static String uploadURL(){
        return fileServerUrl +"/upload";
    }

    //上传任务子项
    public static final class UploadFileItem {
        public String uri;//上传的文件的本地路径
        public String remotePath;// 指定保存的路径
        public String fileName;// 指定保存的文件名
        public boolean uploadSuccessDelete = false;
    }

    //上传任务
    public static final class UploadTask {
        private String url;//文件服务上传URL
        private List<UploadFileItem> files; //上传文件列表
        //添加上传文件
        private void addUploadFile(UploadFileItem item) {
            if (files == null) files = new ArrayList<>();
            files.add(item);
        }
    }

    //下载任务
    public static final class DownloadTask{
        private String url;
        private String storePath;
        private HttpUtils.Callback callback;
        public DownloadTask(String url, String storePath, HttpUtils.Callback callback) {
            this.url = url;
            this.storePath = storePath;
            this.callback = callback;
        }
    }

    //任务
    private static final class Task{
        private DownloadTask downloadTask;
        private UploadTask uploadTask;

        private Task(UploadTask uploadTask) {
            this.uploadTask = uploadTask;
        }
        private Task(DownloadTask downloadTask) {
            this.downloadTask = downloadTask;
        }
    }

     //任务队列
     private static final LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();

    // 任务队列执行线程
     private static Thread taskQueueThread = new Thread(){
         @Override
         public void run() {
             while (true){
                 try {
                     Task task = taskQueue.take();
                     if (task == null) continue;
                     try {
                         if (task.uploadTask != null){
                             updateFile(task.uploadTask);
                         }
                         if (task.downloadTask != null){
                             downloadFile(task.downloadTask.url,task.downloadTask.storePath,task.downloadTask.callback);
                         }
                     } catch (Exception e) {
                         LLog.print("任务执行失败:\n\t"+ GsonUtils.javaBeanToJson(task));
                     }
                 } catch (Exception ignored) { }
             }
         }
     };

    //多个文件上传 加入队列
    public static void addUpdateFileToQueue(UploadFileItem... items){
        UploadTask uploadTask = new UploadTask();
        for (UploadFileItem it : items){
            uploadTask.addUploadFile(it);
        }
        //加入上传队列
        taskQueue.offer(new Task(uploadTask));
    }

    public static void addDownloadFileToQueue(DownloadTask... items){
        for (DownloadTask it : items){
            //加入下载队列
            taskQueue.offer(new Task(it));
        }
    }


    //文件上传
    public static String updateFile(UploadTask bean){
        HttpRequest httpRequest = new HttpRequest();
        List<File> deleteList = new ArrayList<>();
        for (UploadFileItem item : bean.files){
            try {
                Uri uri = Uri.parse(item.uri);
                String path = uri.getPath();
                if (path!=null){
                    File file = new File(path);
                    if (file.exists()){
                        httpRequest.addFile(file,item.remotePath ,item.fileName);
                        if (item.uploadSuccessDelete){
                            deleteList.add(file);
                        }
                    }
                }
            } catch (Exception e) {
                LLog.error(e);
            }
        }
        String uploadURL = bean.url==null? uploadURL(): bean.url;
        String result =  httpRequest.fileUploadUrl(uploadURL).getRespondContent();
        if (deleteList.size() >0 && result!=null){
            //删除文件
            ServletResult<Object> result1Bean = GsonUtils.jsonToJavaBean(result, new TypeToken<ServletResult<Object>>(){}.getType());
            if (result1Bean!=null && result1Bean.code == 200){
                for (File file: deleteList) {
                    boolean isDeleteSuccess = file.delete();
                    LLog.print("本地文件("+file+") 上传成功, 尝试删除: " + isDeleteSuccess);
                }
            }
        }
        return result;
    }

    //文件下载
    public static File downloadFile(String url, String storePath, HttpUtils.Callback callback){
        long time = System.currentTimeMillis();;
        File file = new File(storePath);
        boolean isDownload = new HttpRequest().setCallback(callback).download(url,file);
        if (isDownload){
            LLog.print("[下载] URL: "+ url + "\n\t存储位置: "+ file.getPath() +
                    "\n\t文件大小: "+ FileUtils.byteLength2StringShow(file.length()) + " , 耗时: " + TimeUtils.formatDuring(System.currentTimeMillis() - time ));
        }
        return isDownload ? file : null;
    }

    //获取文本
    public static String text(String url) throws Exception{
        HttpRequest httpRequest = new HttpRequest().accessUrl(url);
        if (httpRequest.getException()!=null) throw httpRequest.getException();
        return httpRequest.getRespondContent();
    }

    /* 启动上传线程 */
    private static void startUploadThread() {
        taskQueueThread.setDaemon(true);
        taskQueueThread.start();
    }

}
