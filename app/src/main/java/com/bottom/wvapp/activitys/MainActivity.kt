package com.bottom.wvapp.activitys

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import kotlinx.android.synthetic.main.activity_main.*

import android.net.http.SslError
import android.webkit.*
import com.bottom.wvapp.R
import com.bottom.wvapp.activitys.JavaScriptInterface.webViewLoadLocalJs


private class _WebChromeClient : WebChromeClient(){

}

private class _WebViewClient : WebViewClient(){
    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        Log.d("_WebChromeClient", "onReceivedHttpError:$errorResponse")
        //loadingFailed();
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        //                注意：super句话一定要删除，或者注释掉，否则又走handler.cancel() 默认的不支持https的了。
        //                super.onReceivedSslError(view, handler, error);
        handler.proceed()// 接受所有网站的证书
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
//        return super.shouldOverrideUrlLoading(view, url)
        view?.loadUrl(url)
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("onPageStarted","开始加载$url")
//        view!!.evaluateJavascript(JavaScriptInterface.webViewLoadLocalJs(view), object :ValueCallback<String>{
//            override fun onReceiveValue(value: String?) {
//                Log.d("onPageStarted", "value=$value")
//            }
//        });
//        view!!.loadUrl("javascript:" + JavaScriptInterface.webViewLoadLocalJs(view));
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        Log.d("onPageFinished","完成加载$url")
        super.onPageFinished(view, url)
//        view!!.loadUrl("javascript:" + JavaScriptInterface.webViewLoadLocalJs(view));
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        Log.d("onLoadResource","----------------onLoadResource------------")
        super.onLoadResource(view, url)
//        view!!.evaluateJavascript(webViewLoadLocalJs(view), object :ValueCallback<String>{
//            override fun onReceiveValue(value: String?) {
//                Log.d("onPageStarted", "value=$value")
//            }
//        });

//        view!!.loadUrl("javascript:" + webViewLoadLocalJs(view));
    }

}


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViewWeb()
    }


    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun initViewWeb() {
        web_view.webChromeClient = _WebChromeClient();
        web_view.settings.javaScriptEnabled = true;   //开启JavaScript支持
        val jsInterface = JavaScriptInterface(web_view, BackServerImp())
        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        web_view.addJavascriptInterface(
                jsInterface,
                JavaScriptInterface.NAME);
        web_view.webViewClient = _WebViewClient();
        web_view.settings.blockNetworkImage = false;//解决图片不显示
        web_view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;

        web_view.loadUrl("file:///android_asset/index.html");
        JavaScriptInterface.test(jsInterface);
    }
}
