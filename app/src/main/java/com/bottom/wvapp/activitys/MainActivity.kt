package com.bottom.wvapp.activitys

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.bottom.apps.PermissionApply
import com.bottom.wvapp.R
import com.bottom.wvapp.bridge.JavaScriptInterface
import com.bottom.wvapp.service.BackServerImp
import kotlinx.android.synthetic.main.activity_main.*

/**
 * 内核
 */
private class _WebChromeClient : WebChromeClient(){

}

/**
 * 浏览器客户端
 */
private class _WebViewClient : WebViewClient(){
    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        super.onReceivedHttpError(view, request, errorResponse)
        //loadingFailed();
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        //                注意,super句话一定要删除，或者注释掉，否则又走handler.cancel() 默认的不支持https的了。
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
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
    }

    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
    }

}


class MainActivity : AppCompatActivity() , PermissionApply.Callback{

    private val permissionArray = arrayOf(

            Manifest.permission.CAMERA, // 相机和闪光灯
//            Manifest.permission.ACCESS_FINE_LOCATION, //GPS
//            Manifest.permission.ACCESS_COARSE_LOCATION, //NET LOCATION
//            Manifest.permission.READ_PHONE_STATE, //获取手机信息
            Manifest.permission.WRITE_EXTERNAL_STORAGE // 写sd卡
    )

    private val permissionApply = PermissionApply(this, permissionArray, this);





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViewWeb()
    }

    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    private fun initViewWeb() {
        web_view.webChromeClient = _WebChromeClient();
        web_view.settings.javaScriptEnabled = true;   //开启JavaScript支持
        val server = BackServerImp();
        val jsInterface = JavaScriptInterface(web_view,server,server)
        server.settingBridge(jsInterface)
        // 添加一个对象, 让JS可以访问该对象的方法, 该对象中可以调用JS中的方法
        web_view.addJavascriptInterface(
                jsInterface,
                JavaScriptInterface.NAME);
        web_view.webViewClient = _WebViewClient();
        web_view.settings.blockNetworkImage = false;//解决图片不显示
        web_view.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW;

//        web_view.loadUrl("file:///android_asset/index.html");
        web_view.loadUrl("http://114.116.149.145:8800/");
    }

    override fun onResume() {
        super.onResume()

        //检查权限
        if (permissionApply.isPermissionsDenied){
            permissionApply.permissionCheck()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        permissionApply.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //权限允许回调
    override fun onPermissionsGranted() {
        permissionApply.isIgnoreBatteryOption
    }
    //忽略电源回调
    override fun onPowerIgnoreGranted() {

    }
}
