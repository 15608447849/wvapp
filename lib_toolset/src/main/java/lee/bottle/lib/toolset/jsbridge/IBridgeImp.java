package lee.bottle.lib.toolset.jsbridge;

import android.content.Intent;

/**
 * Created by Leeping on 2019/6/3.
 * email: 793065165@qq.com
 */
public interface IBridgeImp{
    void setIJsBridge(IJsBridge bridge);
    Object invoke(final String methodName, final String data) throws Exception;
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
