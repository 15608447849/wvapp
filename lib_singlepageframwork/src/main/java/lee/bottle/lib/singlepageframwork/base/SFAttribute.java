package lee.bottle.lib.singlepageframwork.base;

import android.os.Bundle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Leeping on 2018/4/15.
 * email: 793065165@qq.com
 * 用于反射构建Fragment
 */

public final class SFAttribute {
    /**
     * 主容器标识
     */
    private String pageTag = "";
    /**
     * fragment 类路径
     */
    private String classPath;
    /**
     * fragment 唯一标识
     */
    private String tagName;
    /**
     * 初始化时携带的参数
     */
    private Bundle args;


    public  SFAttribute(String classPath, String tagName) {
        this(classPath,tagName,null);
    }

    public <T extends SFragment> SFAttribute(Class<T> cls, String tagName) {
        this(cls.getName(),tagName,null);
    }

    public SFAttribute(String classPath, String tagName, HashMap<String,String> map) {
        if (classPath==null || classPath.length()==0 || tagName==null || tagName.length()==0 ) throw new IllegalArgumentException("无法绑定一个fragment属性");
        this.classPath = classPath;
        this.tagName = tagName;
        if (map!=null){
            this.args = new Bundle();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                this.args.putString(entry.getKey(), entry.getValue());
            }
        }
    }

    public SFAttribute(String classPath) {
        this(classPath,classPath);
    }

    public String getClassPath() {
        return classPath;
    }

    public String getTagName() {
        return pageTag+separator+tagName;
    }

    public static final String separator = "&";

    public static String parsePageTag(String spaFragmentTag){
        String[] sArr = spaFragmentTag.split(separator);
        if (sArr.length==2) return sArr[0];
        return  null;
    }
    public static String parseFragmentTag(String spaFragmentTag){
        String[] sArr = spaFragmentTag.split(separator);
        if (sArr.length==2) return sArr[1];
        return  null;
    }

    public Bundle getArgs() {
        return args;
    }

    @Override
    public int hashCode() {
        return getTagName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj instanceof SFAttribute){
            SFAttribute o = (SFAttribute)obj;
            return o.getTagName().equals(getTagName());
        }
        return false;
    }

    public String getPageTag() {
        return pageTag;
    }

    public void setPageTag(String pageTag) {
        this.pageTag = pageTag;
    }

    @Override
    public String toString() {
        return "{" +
                "pageTag='" + pageTag + '\'' + ", tagName='" + tagName + '\'' +
                '}';
    }

    public boolean isSame(String tag) {
        return getTagName().equals(pageTag+separator+tag);
    }
}
