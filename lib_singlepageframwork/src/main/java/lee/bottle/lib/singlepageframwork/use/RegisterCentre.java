package lee.bottle.lib.singlepageframwork.use;

import android.view.View;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import lee.bottle.lib.singlepageframwork.anno.SLayoutId;
import lee.bottle.lib.singlepageframwork.anno.SViewHolder;
import lee.bottle.lib.singlepageframwork.base.SActivity;
import lee.bottle.lib.singlepageframwork.base.SFAttribute;
import lee.bottle.lib.singlepageframwork.base.SFragment;

/**
 * Created by Leeping on 2018/6/13.
 * email: 793065165@qq.com
 * fragment 注册中心
 */

public class RegisterCentre {

    public static class Bean{

        public Bean(String fragment,String name, String container) {
            this.container = container;
            this.fragment = fragment;
            this.name = name;
        }

        public String container;//容器
        String fragment;//fragment路径
        public String name;//fragment名字
        HashMap<String,String> map;//携带的参数

        public Bean addParam(String k,String v){
            if (map == null) map = new HashMap<>();
            map.put(k,v);
            return this;
        }

        @Override
        public String toString() {
            return "{" +
                    "container='" + container + '\'' +
                    ", fragment='" + fragment + '\'' +
                    ", name='" + name + '\'' +
                    ", map=" + map +
                    '}';
        }
    }

    static Map<String,HashSet<SFAttribute>> map = new LinkedHashMap<>();

    /** 注册单个fragment到指定容器*/
    public static void register(String containerTag, SFAttribute fragmentAttr){
        fragmentAttr.setPageTag(containerTag);//设置主容器标识
        HashSet<SFAttribute> hashSet = map.get(containerTag);
        if (hashSet == null){
            hashSet = new HashSet<>();
            map.put(containerTag,hashSet);
        }
        hashSet.add(fragmentAttr);

    }
    /** 注册多个fragment到指定容器*/
    public static void register(String containerTag, SFAttribute... fragmentAttrs){
       for (SFAttribute attr : fragmentAttrs){
           register(containerTag,attr);
       }
    }

    public static <T extends SFragment> void register(Bean... beans){
        for (Bean b : beans){
            register(b.container,b.fragment,b.name,b.map);
        }
    }

    public static <T extends SFragment> void register(String containerTag, String clsPath, String tagName,HashMap<String,String> map){
        register(containerTag,new SFAttribute(clsPath,tagName,map));
    }

    public static <T extends SFragment> void register(String containerTag, Class<T> cls, String tagName){
        register(containerTag,new SFAttribute(cls,tagName));
    }

    public static HashSet<SFAttribute> getFragmentPage(String tag){
        return map.get(tag);
    }
    /**根据注解自动创建容器
     *   * 1. 获取此对象所有属性
     * 2. 遍历此对象是否存在注解 @SLayoutId(name)
     * 3. 遍历是否存在注解 @SViewHolder() ,存在-> 遍历此对象是否存在 @SLayoutId
     * */
    public static void autoCreatePageHolder(SActivity activity) throws Exception{
        //获取实体属性列表
        Class cls = activity.getClass();
        Field[] fieldArr = cls.getDeclaredFields();
        Field field;
        SViewHolder svh;
        SLayoutId scph;
        int size = fieldArr.length;
        for (int i = 0; i < size; i++) {
            field  = fieldArr[i];
            field.setAccessible(true); //设置些属性是可以访问的
            svh = field.getAnnotation(SViewHolder.class);
            scph = field.getAnnotation(SLayoutId.class);
            if (svh!=null){
                foreachViewHolder(activity,field.get(activity));
            }
            if (scph!=null) execCreatePageHolder(activity,activity,scph.value(),field);
        }
    }

    private static void foreachViewHolder(SActivity activity, Object holder) throws Exception{
        Class cls = holder.getClass();
        Field[] fieldArr = cls.getDeclaredFields();
        int size = fieldArr.length;
        Field field;
        SLayoutId scph;
        for (int i=0 ; i<size;i++){
            field  = fieldArr[i];
            field.setAccessible(true); //设置些属性是可以访问的
            scph = field.getAnnotation(SLayoutId.class);
            if (scph!=null) execCreatePageHolder(activity,holder,scph.value(),field);
        }
    }

    private static void execCreatePageHolder(SActivity activity, Object FieldHolder,String pageTag, Field field) throws Exception{
        if (null == pageTag || "".equals(pageTag)) return;
        if (!View.class.isAssignableFrom(field.getType())) return;
        View v = (View) field.get(FieldHolder);
        activity.createPageHolder(pageTag,v.getId());
    }

}
