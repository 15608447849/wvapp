package lee.bottle.lib.toolset.web;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.ClientCertRequest;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import lee.bottle.lib.toolset.jsbridge.JSUtils;
import lee.bottle.lib.toolset.log.LLog;

import static lee.bottle.lib.toolset.http.HttpUtil.closeIo;

/**
 * Created by Leeping on 2019/7/7.
 * email: 793065165@qq.com
 */
public class SysWebViewClient extends WebViewClient {
    /**
     * 当加载的网页需要重定向的时候就会回调这个函数告知我们应用程序是否需要接管控制网页加载，如果应用程序接管，
     * 并且return true意味着主程序接管网页加载，如果返回false让webview自己处理
     * 注："post"请求方式不会调用这个回调函数
     */
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        LLog.print("shouldOverrideUrlLoading\n\t"+url);
        view.loadUrl(url);
        return true;
    }

    /**
     * 通知应用程序内核即将加载url制定的资源，应用程序可以返回本地的资源提供给内核，
     * 若本地处理返回数据，内核不从网络上获取数据
     */
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView webView, String url) {
        WebResourceResponse webResourceResponse = JSUtils.mediaUriIntercept(webView.getContext(),url,WebResourceResponse.class);
        return resourceLocalStore(webView.getContext(),url,webResourceResponse) != null ? webResourceResponse : super.shouldInterceptRequest(webView,url);
    }



    private WebResourceResponse resourceLocalStore(Context context, String url, WebResourceResponse webResourceResponse) {
//        LLog.print(Thread.currentThread() + " 资源拦截: " + url+" ,webResourceResponse = " + webResourceResponse);
//        if (url.startsWith("http") || url.startsWith("https")){
//            File file = context.getFilesDir()+"/webResource";
//            return webResourceResponseDownload(url);
//        }
        return webResourceResponse;
    }

    private WebResourceResponse webResourceResponseDownload(String url) {

        //从软缓存拿
        //软缓存找不到->硬盘缓存拿
        //还是没有,下载资源->设置软存->设置硬存

        WebResourceResponse webResourceResponse = null;
        HttpURLConnection con = null;
        OutputStream out = null;//本地文件输出流
        InputStream in = null; //服务器下载输入流
        try {
            if (url.contains("localhost")) return null;
            LLog.print("下载: " + url);
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setRequestMethod("GET");// GET POST
            con .setUseCaches(false);
            con.setDefaultUseCaches(false);
            con.setConnectTimeout(30000);
            con.setReadTimeout(5000);
            con.setRequestProperty("Charset", "UTF-8");
            con.setRequestProperty("Connection", "keep-alive");  //设置连接的状态
            con.setDoInput(true);
            con.setRequestProperty("Accept-Encoding", "identity");

            con.setRequestProperty("Content-Type", "application/octet-stream");//传输数据类型,流传输
            con.connect();//连接
            int code = con.getResponseCode();
            String message = con.getResponseMessage();
            String contentType = con.getContentType();
            if ( code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_PARTIAL) {
                in = con.getInputStream();
                webResourceResponse = new WebResourceResponse(contentType,"UTF-8",in);
            }
        }catch (Exception e){
          e.printStackTrace();
        }finally {
            closeIo(out,in);
            if (con!=null) con.disconnect();//断开连接
        }
        LLog.print("返回: " + webResourceResponse);
        return webResourceResponse;
    }


    /**
     * http加载错误
     */
    @Override
    public void onReceivedHttpError(WebView webView, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(webView, request, errorResponse);
    }

    /** 访问地址错误回调 */
    @Override
    public void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError) {
        super.onReceivedError(webView, webResourceRequest, webResourceError);
    }

    /** 如果浏览器需要重新发送POST请求，可以通过这个时机来处理。默认是不重新发送数据 */
    @Override
    public void onFormResubmission(WebView view, Message dontResend, Message resend) {
        super.onFormResubmission(view, dontResend, resend);
    }

    /**通知应用程序可以将当前的url存储在数据库中，意味着当前的访问url已经生效并被记录在内核当中。
     * 这个函数在网页加载过程中只会被调用一次。
     * 注意网页前进后退并不会回调这个函数*/
    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    /** 提供应用程序同步一个处理按键事件的机会，菜单快捷键需要被过滤掉。如果返回true，webview不处理该事件，如果返回false，
     * webview会一直处理这个事件，因此在view 链上没有一个父类可以响应到这个事件。默认行为是return false */
    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return super.shouldOverrideKeyEvent(view, event);
    }

    /** 在加载页面资源时会调用，每一个资源（比如图片）的加载都会调用一次*/
    public void onLoadResource(WebView view, String url) {
//        LLog.print("加载资源: " + url);
        super.onLoadResource(view, url);
    }

    /**
     * 当网页加载资源过程中发现SSL错误会调用此方法。我们应用程序必须做出响应，是取消请求handler.cancel(),还是继续请求handler.
     * proceed();内核的默认行为是handler.cancel()
     * */
    @Override
    public void onReceivedSslError(WebView webView, SslErrorHandler handler, SslError error) {
        handler.proceed();// 接受所有网站的证书
        super.onReceivedSslError(webView, handler, error);
    }

    /**
     * onReceivedHttpAuthRequest 通知应用程序WebView接收到了一个Http auth的请求，
     * 应用程序可以使用supplied 设置webview的响应请求。默认行为是cancel 本次请求
     */
    @Override
    public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
        super.onReceivedHttpAuthRequest(view, handler, host, realm);
    }

    /** 在页面加载开始时调用*/
    @Override
    public void onPageStarted(WebView webView, String url, Bitmap favicon) {
        webView.getSettings().setBlockNetworkImage(true);
        super.onPageStarted(webView, url, favicon);
    }

    /** 在页面加载结束时调用 */
    @Override
    public void onPageFinished(WebView webView, String url) {
        super.onPageFinished(webView, url);
        webView.getSettings().setBlockNetworkImage(false);
    }

    /** 通知应用程序webview 要被scale。应用程序可以处理改事件，比如调整适配屏幕 */
    @Override
    public void onScaleChanged(WebView view, float oldScale, float newScale) {
        super.onScaleChanged(view, oldScale, newScale);
    }
 /** HTTP的body标签加载前调用，仅在主frame调用 */
    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
    }
    /** 通知主机应用程序处理SSL客户端证书请求。如果需要，主机应用程序负责显示UI并提供密钥。有三种方式来响应：proceed(), cancel()或ignore()。WebVIEW将调用响应存储在内存中（对于应用程序的生命周期）如果proceed() 或 cancel()被调用并且个对同样的主机：端口不会再次调用onReceivedClientCertRequest()。如果调用ignore()，WebVIEW不会存储响应。要注意多层chromium网络栈可能会缓存相应，所以最好的行为就是忽略（ignore）。这个方法在UI线程上被调用。在回调期间，连接被挂起。*/
    @Override
    public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
        super.onReceivedClientCertRequest(view, request);
    }

    @Override
    public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
        return super.onRenderProcessGone(view, detail);
    }

    /** Google安全浏览API */
    @Override
    public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType, SafeBrowsingResponse callback) {
        super.onSafeBrowsingHit(view, request, threatType, callback);

    }
}
