package com.bottom.wvapp.activitys

import android.annotation.SuppressLint
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient

import kotlinx.android.synthetic.main.activity_main.*
import android.webkit.WebResourceResponse
import android.webkit.WebResourceRequest
import android.webkit.WebView

import android.net.http.SslError
import android.webkit.SslErrorHandler
import com.bottom.wvapp.R


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

        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        web_view.addJavascriptInterface(
                JavaScriptInterface(web_view, BackServerImp()),
                JavaScriptInterface.NAME);
        web_view.webViewClient = _WebViewClient();
        web_view.settings.blockNetworkImage = false;//解决图片不显示
        web_view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;
        //加载本地js
//        JavaScriptInterface.webViewLoadLocalJs(web_view);
        web_view.loadUrl("file:///android_asset/index.html");

//        web_view.loadUrl("http://www.baidu.com");
    }
}
