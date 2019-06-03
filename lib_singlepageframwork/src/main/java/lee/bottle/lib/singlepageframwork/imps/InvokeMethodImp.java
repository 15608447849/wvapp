package lee.bottle.lib.singlepageframwork.imps;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import lee.bottle.lib.singlepageframwork.anno.SICMethod;
import lee.bottle.lib.singlepageframwork.base.SMessage;
import lee.bottle.lib.singlepageframwork.infs.SICommunication;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class InvokeMethodImp implements SICommunication {

    private final SIThreadExe exe;
    public InvokeMethodImp(SIThreadExe exe) {
        this.exe = exe;
    }

    /**
     * fragment发送消息到activity 或者 一个可以对消息做出处理的实现类
     * 返回 true 则被拦截-处理
     */
    @Override
    public boolean dispatch(SMessage message) {
        if (message.isInvoke()){
            final SMessage.Callback callback = message.callback;
            final java.lang.Object holder  = message.target;
            final String methodName = message.methodName;
            final Class[] paramClasses = message.argsTypes ;
            final java.lang.Object[] args = message.args;
            try{
                execute(callback,holder,methodName,paramClasses,args);
            }catch (Exception e){
                e.printStackTrace();
                if (callback!=null) callback.action(-1,null);
            }
             return true;
        }
        return false;
    }

    private void execute(final SMessage.Callback callback, final Object holder, final String methodName, final Class[] paramClasses, final Object[] args) throws Exception{
        final Method method = holder.getClass().getDeclaredMethod(methodName, paramClasses);
        method.setAccessible(true);
        int fieldValue = method.getModifiers();// 获取字段的修饰符
        if (!Modifier.isPublic(fieldValue)) throw new IllegalAccessException("method is not public, permission denied."); //非公开 拒绝
        SICMethod annotation = method.getAnnotation(SICMethod.class);
        if (annotation == null) throw new IllegalAccessException("annotation 'SICMethod' not find.");
        if (!annotation.allow()) throw new IllegalAccessException("annotation 'SICMethod' permission denied.");
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    java.lang.Object result = method.invoke(holder, args);
                    if (callback!=null) callback.action(0,result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        SICMethod.SThread threadType = annotation.workThread();
        if (threadType == SICMethod.SThread.IO){
            exe.io(runnable);
        }else if (threadType == SICMethod.SThread.UI) {
            exe.ui(runnable);
        }
    }

}
