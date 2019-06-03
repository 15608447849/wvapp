package lee.bottle.lib.singlepageframwork.base;

import android.util.Log;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class SLog {
    public static void print(Object... objects){
        StringBuffer sb = new StringBuffer();
        for (Object o : objects){
            sb.append(o).append(" , ");
        }
        sb = sb.delete(sb.length()-3,sb.length());
        Log.d("SPAF",sb.toString());
    }
    public static void error(Object... objects){
        StringBuffer sb = new StringBuffer();
        for (Object o : objects){
            sb.append(o).append(" , ");
        }
        sb = sb.delete(sb.length()-3,sb.length());
        Log.e("SPAF",sb.toString());
    }
}
