package lee.bottle.lib.singlepageframwork.base;

import android.util.Log;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class SLog {
    public static boolean debug = true;
    public static void print(Object... objects){
        if (!debug) return;
        StringBuffer sb = new StringBuffer();
        for (Object o : objects){
            sb.append(o).append(" , ");
        }
        sb = sb.delete(sb.length()-3,sb.length());
        Log.e("单页面框架",sb.toString());
    }
}
