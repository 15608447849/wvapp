package lee.bottle.lib.toolset.web;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lee.bottle.lib.toolset.log.LLog;

import static android.webkit.WebViewClient.ERROR_AUTHENTICATION;
import static android.webkit.WebViewClient.ERROR_BAD_URL;
import static android.webkit.WebViewClient.ERROR_CONNECT;
import static android.webkit.WebViewClient.ERROR_FAILED_SSL_HANDSHAKE;
import static android.webkit.WebViewClient.ERROR_FILE;
import static android.webkit.WebViewClient.ERROR_FILE_NOT_FOUND;
import static android.webkit.WebViewClient.ERROR_HOST_LOOKUP;
import static android.webkit.WebViewClient.ERROR_IO;
import static android.webkit.WebViewClient.ERROR_PROXY_AUTHENTICATION;
import static android.webkit.WebViewClient.ERROR_REDIRECT_LOOP;
import static android.webkit.WebViewClient.ERROR_TIMEOUT;
import static android.webkit.WebViewClient.ERROR_TOO_MANY_REQUESTS;
import static android.webkit.WebViewClient.ERROR_UNKNOWN;
import static android.webkit.WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME;
import static android.webkit.WebViewClient.ERROR_UNSUPPORTED_SCHEME;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class JSUtils {
    // URL加载进度
    public interface WebProgressI{
        void updateProgress(String url , int current, boolean isForce);
    }
    // 资源请求拦截
    public interface WebResourceRequestI{
        WebResourceResponse resourceIntercept(String url);
    }
    // 加载错误
    public interface LoadErrorI{
        void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError,int errorCount);
    }
    // 弹窗
    public interface AlertMessageI{
        void onJsAlert(final WebView view, String url, final String message, final JsResult result);
    }

    public static WebProgressI webProgressI = null;
    public  static WebResourceRequestI webResourceRequestI;
    public static LoadErrorI loadErrorI;
    public static AlertMessageI onAlertI;

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

    //网页加载 错误码转换文本
    public static String webViewErrorCodeConvertString(int errorCode){
       switch (errorCode){
           case ERROR_UNKNOWN:
               return "未知错误";

           case ERROR_HOST_LOOKUP:
               return "服务器或代理主机名查找失败";

           case ERROR_UNSUPPORTED_AUTH_SCHEME:
                return "不支持的身份验证方案";

           case ERROR_AUTHENTICATION:
               return "服务器上的用户身份验证失败";

           case ERROR_PROXY_AUTHENTICATION:
               return "代理上的用户身份验证失败";

           case ERROR_CONNECT:
               return "无法连接到服务器";

           case ERROR_IO:
               return "无法读取或写入服务器";

           case ERROR_TIMEOUT:
               return "连接超时";

           case ERROR_REDIRECT_LOOP:
               return "重定向太多";

           case ERROR_UNSUPPORTED_SCHEME:
               return "不支持的URI方案";

           case ERROR_FAILED_SSL_HANDSHAKE:
               return "无法执行SSL握手";

           case ERROR_BAD_URL:
               return "错误的URL";

           case ERROR_FILE:
               return "一般文件错误";

           case ERROR_FILE_NOT_FOUND:
               return "找不到文件";

           case ERROR_TOO_MANY_REQUESTS:
               return "此加载期间请求过多";

           default:
               return "未知错误";
       }

    }

}
