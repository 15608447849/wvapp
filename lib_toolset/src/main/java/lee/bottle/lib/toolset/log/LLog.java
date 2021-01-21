package lee.bottle.lib.toolset.log;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Leeping on 2018/8/20.
 * email: 793065165@qq.com
 * 日志处理器
 */
public class LLog{

    public static boolean isDebug = false;

    //消息队列
    private static final ConcurrentLinkedQueue<Object[]> locQueue = new ConcurrentLinkedQueue<>();

    //内容处理者
    private static final ArrayList<ILogHandler> handlerList = new ArrayList<>();

    //文件输出
    private static LogFile fileHandler = new LogFile();

    private static volatile boolean flag = true;

    private static final Runnable RUNNABLE = new Runnable() {
        @Override
        public void run() {
            clear();
            execute();
        }
    };

    private static final Thread THREAD = new Thread(RUNNABLE);

    static {
        THREAD.setName("t-logger");
        THREAD.setDaemon(true);
        THREAD.start();
        addLogHandler(new LogConsole());
        addLogHandler(new LogFile());
    }

    private static Build build = new Build();

    public static final Build getBuild(){
        return build;
    }

    /**加入日志处理器*/
    public static boolean addLogHandler(ILogHandler handler){
        return handlerList.add(handler);
    }

    /**关闭日志处理器*/
    public static final void stopLogThread(){
        LLog.flag = false;
    }

    //等待
    private static void waitThread(long time) {
        try {
        synchronized (LLog.class){
                LLog.class.wait(time);
        }
        } catch (InterruptedException ignored) {}
    }

    //通知
    private static void notifyThread() {
        synchronized (LLog.class){
            LLog.class.notifyAll();
        }
    }

    //格式化写入
    public static void format(String format, Object... args){
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Thread thread = Thread.currentThread();
        //加入队列
        locQueue.offer(
                new Object[]{build.tag,thread,stackTraceElements,
                        new Object[]{String.format(Locale.getDefault(),format,args)}}
        );
        notifyThread();
    }

    //打印
    public static void print(Object... objects){
        printTag("<<LEE LOG>>",objects);
    }
    //打印
    public static void printTag(String tag, Object... objects){
        if (tag == null) tag = build.tag;
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        Thread thread = Thread.currentThread();
        //加入队列
        locQueue.offer(
                    new Object[]{tag,thread,stackTraceElements, objects}
                );
        notifyThread();
    }

    //清理日志
    public static void clear(){
        fileHandler.clear(build);
    }

    //执行
    private static void execute() {
        while (flag){
            Object[] objects = locQueue.poll();
            if (objects==null){
                waitThread(build.threadTime);
                continue;
            }
            try {
                execute(objects);
            } catch (Exception e) {
                Log.e("LLOG 日志输出器","无法写入日志文件, error = " + e);
            }
        }
    }

    @NonNull
    static <T> T checkNotNull(@Nullable final T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return obj;
    }

    private static void execute(Object[] objects){
        checkNotNull(objects);
        if (objects.length!=4) throw new IllegalArgumentException("objects length = " + objects.length);
        String tag = objects[0].toString();
        Thread thread = (Thread) objects[1];
        StackTraceElement[] trace = (StackTraceElement[]) objects[2];
        Object[] messages = (Object[]) objects[3];

        StringBuffer stringBuffer = new StringBuffer();

        //获取线程信息
        threadInfo(thread,stringBuffer);
        //获取代码调用栈信息
        stackInfo(trace,stringBuffer);
        //获取日志信息
        logInfo(messages ,stringBuffer);
        //日志内容
        final String content = stringBuffer.toString();
        //输出日志
        for (ILogHandler h : handlerList){
            try {
                h.handle(tag,build,content);
            } catch (Exception e) {

                if (isDebug){
                    e.printStackTrace();
                }
            }
        }
        stringBuffer.setLength(0);//清理
    }

    private static void threadInfo(Thread thread,StringBuffer stringBuffer) {
        if (!build.isWriteThreadInfo) return;
        ThreadGroup threadGroup = thread.getThreadGroup();
        while(threadGroup.getParent() != null){
            threadGroup = threadGroup.getParent();
        }
        int totalThread = threadGroup.activeCount();
        stringBuffer.append("线程ID=").append(thread.getId()).append(",");
        stringBuffer.append("线程名=").append(thread.getName()).append(",");
        stringBuffer.append("优先级=").append(thread.getPriority()).append(",");
        stringBuffer.append("守护线程=").append(thread.isDaemon()).append(",");
        stringBuffer.append("线程组=").append(thread.getThreadGroup().getName()).append(",");
//        stringBuffer.append("活跃数="+Thread.activeCount()+"\n");
        stringBuffer.append("总活跃数=").append(totalThread).append("\n");
    }

    private static void stackInfo(@NonNull StackTraceElement[] trace,StringBuffer stringBuffer) {
        int startIndex = getStackStartIndex(trace);
        StackTraceElement tmp;
        for (int i = startIndex; i< startIndex + build.methodLineCount; i++){
            tmp = trace[i];
            stringBuffer.append(tmp.getClassName() + "." + tmp.getMethodName()  + "(" + tmp.getFileName() + ":" + tmp.getLineNumber() + ")");
            stringBuffer.append("\n");
        }
    }

    private static int getStackStartIndex(StackTraceElement[] trace) {
        StackTraceElement e;
        for (int i = 0; i < trace.length; i++) {
            e = trace[i];
            if (e.getClassName().equals(LLog.class.getName())) {
                return ++i;
            }
        }
        return 0;
    }

    private static void logInfo(Object[] objects,StringBuffer stringBuffer){
        String temp;
        for (int i = 0;i<objects.length;i++){
            temp = String.valueOf(objects[i]) ;
            temp = (objects[i] == null || temp.equals("null")) ? "" : temp;
            stringBuffer.append(temp).append("\t");
        }
        stringBuffer.deleteCharAt(stringBuffer.length() - 1);
    }
}
