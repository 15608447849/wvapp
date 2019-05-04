package com.bottom.wvapp.activitys;

import android.util.Log;


public class BackServerImp {

    public String[] test(String json){
        Log.d("后台调用"," ---- "+ json);
        return new String[]{"123","456"};
    }
}
