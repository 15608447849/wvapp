package lee.bottle.lib.toolset.web;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class JSUtils {
    // URL加载进度
    public interface WebProgressI{
        void updateProgress(String url , int current, boolean isForce);
    }
    //资源请求拦截
    public interface WebResourceRequestI{
        WebResourceResponse resourceIntercept(String url);
    }

    private static WebProgressI webProgressI = null;

    public synchronized static void setWebProgressI(WebProgressI callback){
        webProgressI = callback;
    }

    private  static WebResourceRequestI webResourceRequestI;
    public synchronized static void setWebResourceRequestI(WebResourceRequestI callback){
        webResourceRequestI = callback;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //更新当前URL的加载进度
    public synchronized static void progressHandler(String url, int progress,boolean isForce){
        if (webProgressI!=null) webProgressI.updateProgress(url,progress,isForce);
    }

    //文件选择结果标识
    private static int REQUEST_SELECT_FILE = 254;
    //文件选择
    private static ValueCallback<Uri[]> _filePathCallback;
    /** 文件选择 */
    static boolean onShowFileChooser(Object binder, WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        if (_filePathCallback != null) {
            _filePathCallback.onReceiveValue(null);
            _filePathCallback = null;
        }
        _filePathCallback = filePathCallback;
        try {
            if (binder==null){
              throw new IllegalAccessException("浏览器需要打开文件选择器,当前Activity不存在");
            }
            if (binder instanceof Activity){
                ((Activity)binder).startActivityForResult(fileChooserParams.createIntent(), REQUEST_SELECT_FILE);
            }
            if (binder instanceof Fragment){
                ((Fragment)binder).startActivityForResult(fileChooserParams.createIntent(), REQUEST_SELECT_FILE);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (_filePathCallback!=null){
                _filePathCallback.onReceiveValue(null);
                _filePathCallback = null;
            }
        }
        return false;
    }

    static void onActivityResultHandle(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_SELECT_FILE){
            if (_filePathCallback != null) {
                Uri[] uris = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                _filePathCallback.onReceiveValue(uris);
                _filePathCallback = null;
            }
        }
    }

    /** 对资源文件拦截 */
    static WebResourceResponse resourceIntercept(String url){
        if (webResourceRequestI!=null){
            return webResourceRequestI.resourceIntercept(url);
        }
        return null;
    }

}
