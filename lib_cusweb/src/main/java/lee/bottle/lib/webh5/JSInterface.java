package lee.bottle.lib.webh5;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.webh5.interfaces.DynamicJSInterfaceI;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JSInterface {

    // 动态实现的api接口
    private static final Map<String,DynamicJSInterfaceI> dynamicJSInterfaceObjectMap = new HashMap<>();

    public static <T extends DynamicJSInterfaceI>  void addDynamicInterface(T interfaceObject){
        // 查询所有当前实现的的方法
        // 方法名与对象进行关联
        try {
            Class<?> clszz = interfaceObject.getClass();
            Method[] declaredMethods = clszz.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                dynamicJSInterfaceObjectMap.put(declaredMethod.getName(),interfaceObject);
            }
        } catch (Exception e) {
           LLog.error(e);
        }
    }

    public static DynamicJSInterfaceI getDynamicInterface(String methodName) {
        return dynamicJSInterfaceObjectMap.get(methodName);
    }

    public static Object callDynamicJSInterfaceI(DynamicJSInterfaceI dynamicInterface,String method, String data) throws Exception {
        Object val;
        if(data == null){
            Method m = dynamicInterface.getClass().getDeclaredMethod(method);
            m.setAccessible(true);
            val =  m.invoke(dynamicInterface);
        }else{
            Method m = dynamicInterface.getClass().getDeclaredMethod(method,String.class);
            m.setAccessible(true);
            val = m.invoke(dynamicInterface,data);
        }
        return val;
    }

    public final SysWebView webView;

    // 构造函数
    public JSInterface(SysWebView webView) {
        this.webView = webView;
        addJavascriptInterface(new DefaultFunction(this));
    }

    public <T extends JSInterfaceFunction> void addJavascriptInterface(T imp) {
        // 对dom添加接口
        String domObjName = imp.getInterfaceName();
        webView.addJavascriptInterface(imp,domObjName);
        LLog.print(this + " 添加 JAVA-JS INTERFERE: " + domObjName+" => "+imp );
    }


    public static class DefaultFunction extends JSInterfaceFunction{

        public DefaultFunction(JSInterface jsInterface) {
            super(jsInterface);
        }

        @JavascriptInterface
        @Override
        public String getDevice() {
            Context context = jsInterface.webView.getContext();

            Map<String,Object> map = new HashMap<>();
            map.put("statusBarHeight", AppUtils.statusBarHeight(context));
            map.put("version", AppUtils.getVersionName(context));

            return GsonUtils.javaBeanToJson(map);
        }

        @JavascriptInterface
        @Override
        public void openWindow(String url, String data) {
            LLog.print("JS 请求打开链接 URL= "+url+" ,DATA= "+ data);
        }

        @JavascriptInterface
        @Override
        public void onInitializationComplete() {
            LLog.print("********* 页面初始化完成 *******");
        }
    }


}
