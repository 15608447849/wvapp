package lee.bottle.lib.singlepageframwork.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import lee.bottle.lib.singlepageframwork.imps.SFOPageImps;
import lee.bottle.lib.singlepageframwork.infs.SFOPage;
import lee.bottle.lib.singlepageframwork.infs.SICommunication;
import lee.bottle.lib.singlepageframwork.use.RegisterCentre;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 * fragment的创建属性与标识
 */
@SuppressWarnings("ALL")
public class SActivity extends AppCompatActivity implements SICommunication {

    protected SHandler mHandler = new SHandler(this);
    /** 是否已经调用onCreate*/
    private boolean isCreate = false;
    private boolean isSysRecovery = false;
    public boolean isSysRecovery() {
        return isSysRecovery;
    }

    //因为系统原因 activity被杀死却保留碎片的状态而存活的fragment - 存在界面的重叠问题
    private List<SFragment> recoveryFragments = new ArrayList<>();

    /** 被fragment调用 ,如果 activity没有创建完成却能加入到队列 说明是以前残存的碎片*/
    public final void addSFragment(SFragment fragment) {
        if (!isCreate){
            recoveryFragments.add(fragment);
        }
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState==null) return;
        SLog.print(this+" , onRestoreInstanceState , savedInstanceState");
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SLog.print(this+" , onSaveInstanceState");
    }

    /**
     * 所有碎片及当前显示碎片等属性管理
     */
    private final SFOPageImps m = new SFOPageImps();

    /** 创建容器持有者 */
    public  void createPageHolder(String tag, int containerRid){
        SLog.print(this+" , 创建碎片容器: "+tag+" <-> " +containerRid);
        final HashSet<SFAttribute> hashSet = RegisterCentre.getFragmentPage(tag);
        final SFGroup group =
                new SFGroup(this,tag,getSupportFragmentManager(),containerRid,hashSet);
        m.groupMap.put(tag,group);
    }

    public SFGroup getSFGroup(String tag) {
        return m.groupMap.get(tag);
    }

    public SFManage getSFManage() {
        return SFManage.getInstance();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCreate = true;
        SLog.print(this+" , onCreate()");
        if (savedInstanceState!=null) isSysRecovery = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        SLog.print(this+" , onNewIntent()");
    }

    //第一次显示界面
    private boolean isResumed = true;

    @Override
    protected void onResume() {
        super.onResume();
        if (isResumed){
            mHandler.io(new Runnable() {
                @Override
                public void run() {
                    SLog.print(this+" , onResume "+ getWindow().getDecorView().getWindowToken());
                    while (getWindow().getDecorView().getWindowToken()==null);
                    tryRecovery();
                    mHandler.ui(new Runnable() {
                        @Override
                        public void run() {
                            onInitResume();
                            if (isResumed) throw new IllegalStateException("It's already initialized");
                        }
                    });
                }
            });
        }
    }

    protected void onInitResume(){
        isResumed = false;
    }
    private boolean isRegister = false;
    /**
     * 反射,自动创建fragment容器视图持有者
     * */
    private void autoCreatePageHolder(){
        try{
            RegisterCentre.autoCreatePageHolder(this);
            isRegister = true;
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void tryRecovery() {
        //系统恢复状态, 查询栈中是否存在需要恢复的fragment
        if (m.groupMap.size()==0 || !isSysRecovery) return;
        SLog.print("尝试还原activity状态");
        Iterator<SFGroup> iterator = m.groupMap.values().iterator();
        while (iterator.hasNext()){
            getSFManage().recovery(iterator.next(),recoveryFragments);
        }
    }

    //是否允许回退按钮生效
    private boolean isAccessPressBackKey = true;

    //设置是否允许回退
    public void setAccessPressBackKey(boolean accessPressBackKey) {
        isAccessPressBackKey = accessPressBackKey;
    }
    //拦截系统回退键的点击
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            // 不允许回退键
            if (!isAccessPressBackKey){
                SLog.print("当前不允许回退操作");
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private final Runnable resetBack = new Runnable() {
        @Override
        public void run() {
            SLog.print("还原bcckIndex");
            cur_back_time = -1; //重置
        }
    };
    private long cur_back_time = -1;
    // 捕获返回键的方法2
    @Override
    public void onBackPressed() {
        if (interceptBackPressed()) return;
        if (!isAccessPressBackKey) return;
        SLog.print(this+" , onBackPressed() "+ cur_back_time);
        if (cur_back_time == -1){
            Toast.makeText(this,"再次点击将退出应用",Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(resetBack,2000);
            cur_back_time = System.currentTimeMillis();
        }else{
            if (System.currentTimeMillis() - cur_back_time < 100) {
                cur_back_time = System.currentTimeMillis();
                return;
            }
            mHandler.removeCallbacks(resetBack);
            super.onBackPressed();
        }
    }

    private boolean interceptBackPressed() {
        boolean flag = false;
        List<Fragment> list = getSupportFragmentManager().getFragments();
        for (Fragment fragment : list){
            if (fragment instanceof  SFragment){
                flag = ((SFragment)fragment).onBackPressed();
            }
            if (flag) break;
        }
        return flag;
    }

    public SFOPage getSFOPage() {
        if (!isRegister){
            autoCreatePageHolder();
        }
        return m;
    }

    /**
     * fragment发送消息到activity 或者 一个可以对消息做出处理的实现类
     * 返回 true 则被拦截-处理
     *
     * @param message
     */
    @Override
    public boolean dispatch(SMessage message) {
        return false;
    }
}
