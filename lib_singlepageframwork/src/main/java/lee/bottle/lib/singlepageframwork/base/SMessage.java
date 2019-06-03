package lee.bottle.lib.singlepageframwork.base;

import androidx.annotation.NonNull;

import static lee.bottle.lib.singlepageframwork.base.SMessage.Type.INVOKE_METHOD;
import static lee.bottle.lib.singlepageframwork.base.SMessage.Type.TRANSFER;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * 用于在fragmen <-> activity 之间传递消息
 */
public class SMessage {

    //自增长的消息id序号
    private static long sequence = 0;

    private static SMessage current;//当前未使用的消息

    public static SMessage generate(){
        //从池中获取
       SMessage msg =  getMessageByPool();
        //创建消息
        return new SMessage();
    }

    //从池中获取一个消息
    private static SMessage getMessageByPool() {
        synchronized (SMessage.class){
            if (current==null) return null;
            SMessage msg = current;
            current = current.prev;
            msg.prev = null;
            return msg;
        }
    }

    private static void putMessageToPool(SMessage msg) {
        synchronized (SMessage.class){
            SMessage temp = current;
            current = msg;
            current.prev = temp;
        }
    }

    public boolean isInvoke() {
        return type == INVOKE_METHOD;
    }

    //消息类型枚举
    public enum Type {
        INVOKE_METHOD,//调用一个方法
        TRANSFER//传输消息
    }

    public interface Callback{
        void  action(int code,java.lang.Object data);
    }

    private SMessage(){
        id = sequence++;
        SLog.print("创建消息,id = "+ id);
    }



    private long id; //消息序号
    private SMessage prev;
    private Type type = TRANSFER;
    private int what;
    private String message;
    private java.lang.Object data;

    public java.lang.Object target;//反射调用方法所属对象
    public String methodName;//反射方法名
    public Class<?>[] argsTypes;//反射方法参数类型
    public java.lang.Object[] args;//反射方法参数
    public Callback callback;
    //清空消息
    public void clear(){
        if (message!=null) message = null;
        if (data!=null) data = null;
        if (target!=null) target = null;
        if (methodName!=null) methodName = null;
        if (argsTypes!=null) argsTypes = null;
        if (args!=null) args = null;
        putMessageToPool(this);
    }

    //反射调用目标的方法
    public SMessage invokeTargetMethod(
            @NonNull String methodName,
            Class<?>[] argsTypes,
            java.lang.Object[] args){
        this.methodName = methodName;
        this.argsTypes = (null == argsTypes? new Class[]{}: argsTypes);
        this.args = (null == args? new java.lang.Object[]{} : args);
        this.type = INVOKE_METHOD;
        return this;
    }
    public SMessage invokeTargetMethod(
            @NonNull String methodName){
        return invokeTargetMethod(methodName,null,null);
    }
    public SMessage invokeTargetMethod(
            @NonNull String methodName,
            @NonNull java.lang.Object... args){
        Class[] argsTypes = new Class[args.length];
        for (int i = 0 ; i < args.length ; i++){
            argsTypes[i] = args[i].getClass();
        }
        return invokeTargetMethod(methodName,argsTypes,args);
    }

}
