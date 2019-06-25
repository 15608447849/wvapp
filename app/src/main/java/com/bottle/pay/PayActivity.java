package com.bottle.pay;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alipay.sdk.app.PayTask;
import com.bottle.wvapp.R;

import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.threadpool.IOUtils;
import lee.bottle.lib.toolset.util.AppUtils;
import lee.bottle.lib.toolset.util.GsonUtils;

import static lee.bottle.lib.toolset.util.StringUtils.mapToString;

/**
 * Created by Leeping on 2019/6/24.
 * email: 793065165@qq.com
 */
public class PayActivity extends AppCompatActivity {

    private final String json = "{\"code\":1,\"data\":{\"app_id\":\"2019051764979899\",\"biz_content\":\"{\\\"body\\\":\\\"我是附加数据呵呵呵呵呵\\\",\\\"out_trade_no\\\":\\\"201905210000000057\\\",\\\"passback_params\\\":\\\"我是附加数据呵呵呵呵呵\\\",\\\"product_code\\\":\\\"QUICK_MSECURITY_PAY\\\",\\\"seller_id\\\":\\\"2088531074373364\\\",\\\"subject\\\":\\\"五粮液贵族L0\\\",\\\"total_amount\\\":\\\"0.01\\\"}\",\"charset\":\"utf-8\",\"format\":\"json\",\"method\":\"alipay.trade.app.pay\",\"notify_url\":\"http://192.168.1.145:8080/result/alipay\",\"sign\":\"MnkB51fsD8JWs6FnTLBoD8ni38zjLGp0Tbp1ceDIe+tRYVTw6YlCwdOzp7JqEfav4qI5o9TCtDGat2b5IxuiIg3WFF8W+tnsWLefJu7MexFllnCe7j9HF3ms4VHeNY8aQrf3AoDqo99QBdEE3JD3aJ/IyrMepedP343aNelNssyKS83u8UJWr0N3CrMAo0CHq89sWZeszCbJEGvw4zvyflfZdQQZaK1EvIx835Kr/X0gVJKE8vHEpR8HCogY3jQJA8DF5BN2MPRRnrD/bGDZCVf0J8evS5taRQw1oLeff23t5HXDUz6RWMfnqPqCnprRZxsETkhCvAvqD2Esjodr7w\\u003d\\u003d\",\"sign_type\":\"RSA2\",\"timestamp\":\"2019-06-24 13:34:21\",\"version\":\"1.0\"}}\n";



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
    }

    public void alipay(View view){
        AppUtils.toast(this,"支付宝支付");
        Map map = GsonUtils.jsonToJavaBean(json,Map.class);
        final Map reqMap = (Map) map.get("data");
        final String orderInfo = mapToString(reqMap);

//        PayTask payTask = new PayTask(this);
//        String version = payTask.getVersion();
//        LLog.print("当前支付宝版本号: "+ version);
        IOUtils.run(new Runnable() {
            @Override
            public void run() {
                LLog.print(orderInfo);
                PayTask alipay = new PayTask(PayActivity.this);
                Map <String,String> result = alipay.payV2(orderInfo,true);

                LLog.print("支付宝回调: "+ result);
            }
        });

    }

    public void wx(View view){
        AppUtils.toast(this,"微信支付");
    }


}
