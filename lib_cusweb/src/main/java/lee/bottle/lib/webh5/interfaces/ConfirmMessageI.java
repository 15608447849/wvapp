package lee.bottle.lib.webh5.interfaces;

import android.webkit.JsResult;
import android.webkit.WebView;

public interface ConfirmMessageI {
    void onJsConfirm(final WebView view, String url, final String message, final JsResult result);
}
