package lee.bottle.lib.toolset.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import lee.bottle.lib.toolset.util.ObjectRefUtil;

import static lee.bottle.lib.toolset.util.AppUtils.getLocalFileByte;
import static lee.bottle.lib.toolset.util.ImageUtils.imageCompression;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class JSUtils {

    public interface WebProgressI{
        void updateProgress(int current);
    }

    private static WebProgressI webProgressI;

    public static void setWebProgressI(WebProgressI callback){
        webProgressI = callback;
    }

    /** 对媒体文件拦截 */
    public static <T> T mediaUriIntercept(Context context, String url,Class clazz){
        Uri uri = Uri.parse(url);
//        LLog.print("加载媒体资源URL: "+ url);
        String scheme = uri.getScheme();
        try {
            // 本地自定义协议
            if ("image".equalsIgnoreCase(scheme) || "audio".equalsIgnoreCase(scheme) || "video".equalsIgnoreCase(scheme)){
                String path = uri.getPath();
                File file = new File(path);
                if (!file.exists()) throw new FileNotFoundException(path);
                if ("image".equalsIgnoreCase(uri.getScheme())){
                    file = imageCompression(context,file,1000);//图片压缩
                }
                byte[] imageBuf = getLocalFileByte(file);
                String mimeType = uri.getScheme()+"/*";
                return (T) ObjectRefUtil.createObject(clazz,new Class[]{String.class,String.class, InputStream.class},mimeType, "UTF-8", new ByteArrayInputStream(imageBuf));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void progressHandler(int progress){
        if (webProgressI!=null){
            webProgressI.updateProgress(progress);
        }
    }

    private static int REQUEST_SELECT_FILE = 254;
    //文件选择
    private static ValueCallback<Uri[]> _filePathCallback;

    /** 文件选择 */
    public static boolean onShowFileChooser(Object binder, WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (_filePathCallback != null) {
            _filePathCallback.onReceiveValue(null);
            _filePathCallback = null;
        }
        _filePathCallback = filePathCallback;
        try {
            if (binder==null){
              throw new IllegalAccessException("浏览器需要打开文件选择器,当前Activity or Fragment不存在");
            }

            if (binder instanceof Activity){
                ((Activity)binder).startActivityForResult(fileChooserParams.createIntent(), REQUEST_SELECT_FILE);
            }
            if (binder instanceof Fragment){
                ((Fragment)binder).startActivityForResult(fileChooserParams.createIntent(), REQUEST_SELECT_FILE);
            }

            return true;
        } catch (Exception e) {
            if (_filePathCallback!=null){
                _filePathCallback.onReceiveValue(null);
                _filePathCallback = null;
            }
        }
        return false;
    }

    public static void onActivityResultHandle(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_SELECT_FILE){
            if (_filePathCallback != null) {
                Uri[] uris = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                _filePathCallback.onReceiveValue(uris);
                _filePathCallback = null;
            }
        }
    }







}
