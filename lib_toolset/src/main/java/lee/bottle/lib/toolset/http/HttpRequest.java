package lee.bottle.lib.toolset.http;


import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lee.bottle.lib.toolset.util.GsonUtils;

/**
 * 配合ftc文件服务器使用
 * lzp
 */
public class HttpRequest extends HttpUtil.CallbackAbs  {

    private String text;
    private Exception exception;
    private HttpUtil.Callback callback;

    public HttpRequest bindParam(StringBuffer sb,Map<String,String > map){
        Iterator<Map.Entry<String,String>> it = map.entrySet().iterator();
        Map.Entry<String,String> entry ;

        while (it.hasNext()) {
            entry = it.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        sb.deleteCharAt(sb.length()-1);
        return accessUrl(sb.toString());
    }


    public static String mapToHttpBody(String url,String type,Map map){
        return HttpUtil.contentToHttpBody(url,type, GsonUtils.javaBeanToJson(map));
    }

//    public static String getMapToHttpBody(String url,Map map){
//        return HttpUtil.contentToHttpBody(url,"PATCH",GsonUtils.javaBeanToJson(map));
//    }

    public HttpRequest accessUrl(String url){
        new HttpUtil.Request(url,this)
                .setReadTimeout(30*1000)
                .setConnectTimeout(30*1000)
                .text()
                .execute();
        return this;
    }

    private List<String> pathList = new ArrayList<>();
    private List<String> nameList = new ArrayList<>();
    private List<String> imageSizeList = new ArrayList<>();
    private List<HttpUtil.FormItem> formItems = new ArrayList<>();

    /**
     * 上传文件
     */
    public HttpRequest addFile(File file, String remotePath, String remoteFileName){
        try {
            if (remotePath==null) remotePath = "/java/";
            if (remoteFileName==null) remoteFileName = file.getName();
            pathList.add(URLEncoder.encode(remotePath,"UTF-8"));
            nameList.add(URLEncoder.encode(remoteFileName,"UTF-8"));
            formItems.add(new HttpUtil.FormItem("file", file.getName(), file));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * 上传的文件设置裁剪大小
     */
    public HttpRequest addImageSize(String... sizes){
        imageSizeList.add(join(Arrays.asList(sizes),","));
        return this;
    }

    private boolean isLogo;

    public HttpRequest setLogo(boolean f){
        this.isLogo = f;
        return this;
    }

    private boolean isCompress;
    public HttpRequest setCompress(boolean f){
        this.isCompress= f;
        return this;
    }

    private long compressLimitSize;
    public HttpRequest setCompressLimitSieze(long size){
        this.compressLimitSize= size;
        return this;
    }

    /**
     * 上传流
     */
    public HttpRequest addStream(InputStream stream, String remotePath, String remoteFileName){
        if (remotePath==null) remotePath = "/java/";
        if (remoteFileName==null) throw new NullPointerException("需要上传的远程文件名不可以为空");
        pathList.add(remotePath);
        nameList.add(remoteFileName);
        formItems.add(new HttpUtil.FormItem("file", remoteFileName, stream));
        return this;
    }

    private static String join(List list, String separator) {
        if (list == null || list.size() == 0) return "";
        StringBuffer sb = new StringBuffer();
        for (Object obj : list){
            sb.append(obj.toString()).append(separator);
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private boolean isThumb = true;

    public HttpRequest setThumb(boolean thumb) {
        isThumb = thumb;
        return this;
    }

    /**
     * 执行表单文件上传
     */
    public HttpRequest fileUploadUrl(String url){
        if (formItems != null && formItems.size() > 0){
            HashMap<String,String> headParams = new HashMap<>();
            headParams.put("specify-path",join(pathList,";"));
            headParams.put("specify-filename",join(nameList,";"));
            if(imageSizeList.size() > 0) headParams.put("tailor-list",join(imageSizeList,";"));
            if (isThumb) headParams.put("image-min-exist","1");//图片最小比例缩略图

            if (isLogo) headParams.put("image-logo", "0");//水印
            if (isCompress) headParams.put("image-compress","0");//图片压缩
            if (compressLimitSize>0) headParams.put("image-compress-size",compressLimitSize+"");//图片压缩至少到多少阔值

            new HttpUtil.Request(url, HttpUtil.Request.POST, this)
                    .setFileFormSubmit()
                    .setParams(headParams)
                    .addFormItemList(formItems)
                    .upload()
                    .execute();
        }
        return this;
    }

    /**
     * 获取文件列表
     */
    public HttpRequest getTargetDirFileList(String url, String dirPath, boolean isSub){
        HashMap<String,String> headParams = new HashMap<>();
        headParams.put("specify-path",dirPath);
        headParams.put("ergodic-sub",isSub+"");
        new HttpUtil.Request(url, HttpUtil.Request.POST, this)
                .setParams(headParams)
                .setReadTimeout(1000).
                setConnectTimeout(1000)
                .text()
                .execute();
        return this;
    }

    //获取返回的文本信息
    public String getRespondContent(){
        return text;
    }

    //获取错误信息
    public Exception getException(){
        return exception;
    }

    //删除文件
    public HttpRequest deleteFile(String url, List<String> fileItem){
        try {
            if (fileItem != null && fileItem.size() > 0) {
                HashMap<String,String> headParams = new HashMap<>();
                headParams.put("delete-list", URLEncoder.encode(GsonUtils.javaBeanToJson(fileItem),"UTF-8"));
                new HttpUtil.Request(url, HttpUtil.Request.POST, this)
                        .setParams(headParams)
                        .setReadTimeout(1000).
                        setConnectTimeout(1000)
                        .text()
                        .execute();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return this;
    }


    public HttpRequest setCallback(HttpUtil.Callback callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onProgress(File file, long progress, long total) {
        if (callback!= null) callback.onProgress(file,progress,total);
    }

    @Override
    public void onResult(HttpUtil.Response response) {
        this.text = response.getMessage();
        if (callback!=null) callback.onResult(response);
    }

    @Override
    public void onError(Exception e) {
        exception = e;
        this.text = null;
        if (callback!= null) callback.onError(e);
    }

    private String responseContentType;

    public boolean download(String url,File file){
        if (file.exists() && file.isFile()) {
            if (!file.delete()) return false;
        }

        HttpUtil.Request r = new HttpUtil.Request(url,this);
        r.setDownloadFileLoc(file);
        r.download();
        r.execute();
        responseContentType = r.getResponseContentType();
        return file.exists() && file.length() > 0;
    }

    public String getResponseContentType() {
        return responseContentType;
    }
}
