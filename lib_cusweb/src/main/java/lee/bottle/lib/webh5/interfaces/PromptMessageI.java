package lee.bottle.lib.webh5.interfaces;

import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebView;

public interface PromptMessageI {
    void onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result);
}
