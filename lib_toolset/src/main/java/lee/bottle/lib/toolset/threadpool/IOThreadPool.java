package lee.bottle.lib.toolset.threadpool;

import androidx.annotation.NonNull;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class IOThreadPool extends Thread  implements IThreadPool,RejectedExecutionHandler {
    private ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    private boolean isLoop = true;

    private static ThreadFactory factoryFactory = new ThreadFactory() {
        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("PIO#POOL-"+thread.getId());
            return thread;
        }
    };

    private ThreadPoolExecutor executor;

    public IOThreadPool() {
        executor = createIoExecutor(1000);
        setDaemon(true);
        setName("PIO#POOL#QUEUE-"+getId());
        start();
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
        queue.offer(r);
        synchronized (queue){
            queue.notifyAll();
        }
    }

    @Override
    public void run() {
        while (isLoop){
            try{
                //如果存在任务 , 一直执行 ,直到队列空, 进入等待执行
                Runnable runnable = queue.poll();
                if (runnable == null){
                    synchronized (queue){
                        try {
                            queue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                runnable.run();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void post(Runnable runnable){
        executor.execute(runnable);
    }

    @Override
    public void close(){
        isLoop = false;
        if (executor!=null) executor.shutdownNow();
    }

}
