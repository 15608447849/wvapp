package lee.bottle.lib.singlepageframwork.base;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;

import lee.bottle.lib.singlepageframwork.imps.DataStorageImps;
import lee.bottle.lib.singlepageframwork.imps.InvokeMethodImp;
import lee.bottle.lib.singlepageframwork.imps.SIThreadExe;
import lee.bottle.lib.singlepageframwork.infs.SICommunication;
import lee.bottle.lib.singlepageframwork.infs.SIDataStorage;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class SHandler extends Handler implements SICommunication, SIDataStorage, SIThreadExe {
    //线程池
    private final static SThreadPool pool = new SThreadPool();

    //所有通讯实现
    private final List<SICommunication> communicationImps = new LinkedList<>();

    private final SIDataStorage dataStorage = new DataStorageImps();

    /**
     * 添加fragment->activity消息传递回调接口实现类
     */
    public void addOnSpaCommunication(@NonNull SICommunication communication){
        communicationImps.add(communication);
    }

    //activity软引用
    private final SoftReference<SActivity> activitySoftReference;

    SHandler(SActivity activity) {
        this.activitySoftReference = new SoftReference<>(activity);
        addOnSpaCommunication(new InvokeMethodImp(this));//默认的消息处理实现
    }

    public SActivity getActivitySoftReference() {
        return activitySoftReference.get();
    }


    protected boolean checkMainThread(){
        return Looper.myLooper() == getLooper();
    }
    /**
     * 在Ui线程执行
     * @param r
     */
    @Override
    public void ui(Runnable r){
        if (checkMainThread()){
            r.run();
        }else{
            post(r);
        }
    }

    /**
     * @param r
     * 在其他线程执行
     */
    @Override
    public void io(Runnable r){
        if (checkMainThread()){
            pool.post(r);
        }else{
            r.run();
        }
    }
    /**
     * fragment发送消息到activity 或者 一个可以对消息做出处理的实现类
     * 返回 true 则被拦截-处理
     */
    @Override
    public boolean dispatch(SMessage message) {
        boolean flag = false;
        for (SICommunication c : communicationImps){
            flag = c.dispatch(message);
            if (flag) break;
        }
        message.clear();//清理消息
        return flag;
    }

    @Override
    public void putData(String key, Object val) {
        dataStorage.putData(key,val);
    }

    @Override
    public <T> T getData(String key, T def) {
        return dataStorage.getData(key,def);
    }

    @Override
    public <T> T removeData(String key, T def) {
        return dataStorage.removeData(key,def);
    }
}
