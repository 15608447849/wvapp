package lee.bottle.lib.toolset.os;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Created by Leeping on 2020/10/19.
 * email: 793065165@qq.com
 */
public class BaseActivity extends AppCompatActivity {

    public Handler mHandler = new Handler();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

    }

    // 解决系统改变字体大小的时候导致的界面布局混乱的问题
    // https://blog.csdn.net/lsmfeixiang/article/details/42213483
    // https://blog.csdn.net/z_zT_T/article/details/80372819
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            Configuration config=new Configuration();
            config.setToDefaults();
            res.updateConfiguration(config,res.getDisplayMetrics() );
        }
        return res;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.N){
            final Resources res = newBase.getResources();
            final Configuration config = res.getConfiguration();
            config.setToDefaults();
            final Context newContext = newBase.createConfigurationContext(config);
            super.attachBaseContext(newContext);
        }else{
            super.attachBaseContext(newBase);
        }
    }

}
