package lee.bottle.lib.toolset.util;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;

public class IntentUtils {
    public interface StringCallback{
        void callback(String v);
    }
    public interface ListCallback{
        void callback(List<String> list);
    }
    /*获取并删除intent的消息*/
    public static void getStringExtra(Intent intent, String key, StringCallback callback){
        try {
            String v = intent.getStringExtra(key);
            if (v!=null) {
                intent.removeExtra(key);
                if (callback!=null) callback.callback(v);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*获取并删除intent的消息*/
    public static void getStringArrayListExtra(Intent intent, String key, ListCallback callback){
        try {
            ArrayList<String> list  = intent.getStringArrayListExtra(key);
            if (list!=null) {
                intent.removeExtra(key);
                if (callback!=null) callback.callback(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
