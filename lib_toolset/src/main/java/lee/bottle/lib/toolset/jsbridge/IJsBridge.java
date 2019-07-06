package lee.bottle.lib.toolset.jsbridge;

/**
 * Created by Leeping on 2019/5/5.
 * email: 793065165@qq.com
 */
public interface IJsBridge {
    interface JSCallback{
        void callback(String data);
    }

    // 主动调用js方法
    void requestJs(final String method, final String data, JSCallback callback);

    void loadUrl(String content);

    void putData(String key,String val);

    String getData(String key);

    void delData(String key);
}
