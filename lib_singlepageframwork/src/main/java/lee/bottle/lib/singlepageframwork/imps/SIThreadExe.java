package lee.bottle.lib.singlepageframwork.imps;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public interface SIThreadExe {
    /**
     * 在UI线程执行
     */
    public void ui(Runnable r);

    /**
     * 在其他线程执行
     */
    public void io(Runnable r);
}
