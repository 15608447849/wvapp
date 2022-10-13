package lee.bottle.lib.webh5.interfaces;

import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

public interface LoadErrorI {
    void onReceivedError(WebView webView, WebResourceRequest webResourceRequest, WebResourceError webResourceError);

}
