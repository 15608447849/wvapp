package lee.bottle.lib.webh5.interfaces;

import android.webkit.JsResult;
import android.webkit.WebView;

public interface AlertMessageI {
    void onJsAlert(final WebView view, String url, final String message, final JsResult result);

}
