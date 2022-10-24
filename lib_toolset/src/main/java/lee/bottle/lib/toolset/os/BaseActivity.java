package lee.bottle.lib.toolset.os;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2020/10/19.
 * email: 793065165@qq.com
 */
public class BaseActivity extends AppCompatActivity {

    public Handler mHandler = new Handler();

    @Override
    protected void onNewIntent(Intent intent) {
        LLog.print(this +" *********  onNewIntent");
        super.onNewIntent(intent);
        setIntent(intent);

    }

    // 解决系统改变字体大小的时候导致的界面布局混乱的问题
    // https://blog.csdn.net/lsmfeixiang/article/details/42213483
    // https://blog.csdn.net/z_zT_T/article/details/80372819
    @Override
    public Resources getResources() {
        LLog.print(this +" *********  getResources Build.VERSION.SDK_INT="+ Build.VERSION.SDK_INT);
        Resources res = super.getResources();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            Configuration config=new Configuration();
            config.setToDefaults();
            res.updateConfiguration(config,res.getDisplayMetrics() );
        }
        return res;
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        LLog.print(this +" *********  attachBaseContext Build.VERSION.SDK_INT="+ Build.VERSION.SDK_INT);
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1){
            final Resources res = newBase.getResources();
            final Configuration config = res.getConfiguration();
            config.setToDefaults();
            final Context newContext = newBase.createConfigurationContext(config);
            super.attachBaseContext(newContext);
        }else{
            super.attachBaseContext(newBase);
        }
    }

    /***********************************************************************************************************************/
    /* 是否退出 */
    protected boolean isExitActivity = true;
    /* 是否杀死应用 */
    protected boolean isExitApplication = true;
    /** 捕获返回键点击间隔时间 */
    protected long cur_back_time = -1;
    /** 重置返回键 */
    private final Runnable resetBack = new Runnable() {
        @Override
        public void run() {
            cur_back_time = -1; //重置
        }
    };

    /* 回退事件处理 */
    @Override
    public void onBackPressed() {
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
    /* 结束应用 */
    @Override
    public void finish() {
        if (isExitActivity){
            super.finish();
            if (isExitApplication) android.os.Process.killProcess(android.os.Process.myPid());
        }else{
            moveTaskToBack(true);
        }
    }




}
