package lee.bottle.lib.toolset.util;

import android.content.Context;

import java.io.File;

import lee.bottle.lib.toolset.log.LLog;
import top.zibin.luban.Luban;

/**
 * Created by Leeping on 2019/6/5.
 * email: 793065165@qq.com
 */
public class ImageUtils {
    /**
     * 图片压缩
     */
    public static File imageCompression(Context context, File image, int threshold){
        try{
           return Luban.with(context)
                    .load(image)
                    .ignoreBy(threshold)
                    .get(image.getCanonicalPath());
        }catch (Exception e){
            LLog.error(e);
        }
        return image;
    }
}
