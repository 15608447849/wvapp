package lee.bottle.lib.toolset.threadpool;

import androidx.annotation.NonNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class IOThreadPool implements IThreadPool,RejectedExecutionHandler {
    private static ThreadFactory factoryFactory = new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("PIO#-"+thread.getId());
            return thread;
        }
    };

    private ThreadPoolExecutor executor;

    private IOThreadPool next;

    public IOThreadPool() {
        executor = createIoExecutor(500);
    }

    //核心线程数,最大线程数,非核心线程空闲时间,存活时间单位,线程池中的任务队列
    private ThreadPoolExecutor createIoExecutor(int capacity) {
        ArrayBlockingQueue<Runnable> arrayBlockingQueue = new ArrayBlockingQueue<>(capacity);
         return new ThreadPoolExecutor(
                 Runtime.getRuntime().availableProcessors(),200,30L,TimeUnit.SECONDS,
                 arrayBlockingQueue,
                 factoryFactory,
                 this
                 );
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (next == null) {
            next = new IOThreadPool();
        }
        next.post(r);
    }

    @Override
    public void post(Runnable runnable){
        executor.execute(runnable);
    }

    @Override
    public void close(){
        if (next!=null) next.close();
        if (executor!=null) executor.shutdownNow();
    }

}
