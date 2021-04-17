package lee.bottle.lib.toolset.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.LongSerializationPolicy;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */
public class GsonUtils {
    private static Gson newGson(){
        return new GsonBuilder()
                .setLongSerializationPolicy(LongSerializationPolicy.STRING)
                .create();
    }
    /**
     * json to javabean
     *new TypeToken<List<xxx>>(){}.getType()
     * @param json
     */
    public static <T> T jsonToJavaBean(String json,Type type) {
        try {
            if (json==null || json.length()==0) return null;
            return newGson().fromJson(json, type);//对于javabean直接给出class实例
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * javabean to json
     * @param object
     * @return
     */
    public static String javaBeanToJson(Object object){
        if (object instanceof String) return object.toString(); //如果是String类型
        return newGson().toJson(object);
    }
    /**
     * json to javabean
     *
     * @param json
     */
    public static <T> T jsonToJavaBean(String json,Class<T> cls) {
        try {
            if (json==null || json.length()==0) return null;
            return newGson().fromJson(json, cls);//对于javabean直接给出class实例
        } catch (JsonSyntaxException e) {
//            e.printStackTrace();
        }
        return null;
    }

    public static <T,D> HashMap<T,D> string2Map(String json){
        try {
            if (StringUtils.isEmpty(json)) return null;
            return jsonToJavaBean(json, new TypeToken<HashMap<T,D>>() {}.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> List<T> json2List(String json, Class<T> clazz){
        List<T> list = new ArrayList<>();
        try {
            Gson gson = newGson();
            JsonArray array = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement element : array) {
                list.add(gson.fromJson(element, clazz));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 判断是否是数组类型的json字符串
     */
    public static boolean checkJsonIsArray(String json){
        try {
            Object jsonObj = new JSONTokener(json).nextValue();
            if (jsonObj instanceof JSONArray) {
                return true;
            }
        } catch (JSONException ignored) { }
        return false;
    }

    public static int convertInt(Object val){
        return new BigDecimal(String.valueOf(val)).intValue();
    }

}
