package lee.bottle.lib.toolset.threadpool;

public class IOUtils {
    private static IOThreadPool pool = new IOThreadPool();
    public static void run(Runnable runnable){
        pool.post(runnable);
    }
}