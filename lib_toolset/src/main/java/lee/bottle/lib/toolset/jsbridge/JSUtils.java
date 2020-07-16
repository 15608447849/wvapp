package lee.bottle.lib.toolset.jsbridge;

import android.content.Context;
import android.net.Uri;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

import lee.bottle.lib.toolset.util.ObjectRefUtil;

import static lee.bottle.lib.toolset.util.AppUtils.getLocalFileByte;
import static lee.bottle.lib.toolset.util.ImageUtils.imageCompression;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class JSUtils {

    public interface WebProgressI{
        void updateProgress(int current);
    }

    private static WebProgressI webProgressI;

    public static void setWebProgressI(WebProgressI callback){
        webProgressI = callback;
    }

    /** 对媒体文件拦截 */
    public static <T> T mediaUriIntercept(Context context, String url,Class clazz){
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        try {
            if ("image".equalsIgnoreCase(scheme)
                    || "audio".equalsIgnoreCase(scheme)
                    || "video".equalsIgnoreCase(scheme)){

                String path = uri.getPath();
                File file = new File(path);

                if (!file.exists()) throw new FileNotFoundException(path);
                if ("image".equalsIgnoreCase(uri.getScheme())){
                    file = imageCompression(context,file,1000);//图片压缩
                }

                byte[] imageBuf = getLocalFileByte(file);
                String mimeType = uri.getScheme()+"/*";
                return (T)ObjectRefUtil.createObject(clazz,new Class[]{String.class,String.class, InputStream.class},mimeType, "UTF-8", new ByteArrayInputStream(imageBuf));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    public static void progressHandler(Context context,int progress){
        if (webProgressI!=null){
            webProgressI.updateProgress(progress);
        }
    }








}
