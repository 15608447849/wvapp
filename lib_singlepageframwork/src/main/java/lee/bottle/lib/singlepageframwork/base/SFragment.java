package lee.bottle.lib.singlepageframwork.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.Serializable;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class SFragment extends Fragment {
    //下层碎片
    private SFragment prev;
    private String prevTag;
    //上层碎片
    private SFragment next;
    private String nextTag;


    public SFragment getPrev() {
        return prev;
    }
    public void setPrev(SFragment prev) {
        this.prev = prev;
        if (this.prev == null){
            setPrevTag(null);
        } else{
            setPrevTag(this.prev.getTag());
        }
    }

    public SFragment getNext() {
        return next;
    }

    public void setNext(SFragment next) {
        this.next = next;
        if (this.next == null){
            setNextTag(null);
        } else{
            setNextTag(this.next.getTag());
        }
    }

    public void setPrevTag(String prevTag) {
        this.prevTag = prevTag;
    }

    public void setNextTag(String nextTag) {
        this.nextTag = nextTag;
    }

    public String getPrevTag() {
        return prevTag;
    }

    public String getNextTag() {
        return nextTag;
    }

    private boolean isKillSelf = false;

    public void killSelf() {
        killSelf(true);
    }

    public void killSelf(boolean flag) {
        this.isKillSelf = flag;
    }

    public boolean isKillSelf() {
        return isKillSelf;
    }

    //提供给所有子类使用
    protected SHandler mHandler;

    public SActivity getSActivity(){
        if (mHandler!=null) return mHandler.getActivitySoftReference();
        return null;
    }

    private boolean recoverHide;

    public boolean isRecoverHide() {
        return recoverHide;
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        SLog.print(this+" - onAttach");
        if (context instanceof SActivity){
            SActivity activity  = (SActivity)context;
            mHandler = activity.mHandler;
            activity.addSFragment(this);
        }else{
            throw new RuntimeException("activity does not extends 'com.leezp.lib.singlepageapplication.base.SpaBaseActivity'.");
        }
    }
    private static final String STATE_SAVE_HIDDEN = "STATE_SAVE_HIDDEN";
    private static final String STRUCT_SAVE_PREV = "STRUCT_SAVE_PREV";
    private static final String STRUCT_SAVE_NEXT = "STRUCT_SAVE_NEXT";
    private static final String DATA_SAVE_IN = "DATA_SAVE_IN";
    private static final String DATA_SAVE_OUT = "DATA_SAVE_OUT";
    private Object dataIn; //传进来
    private Object dataOut;  //传出去

    //获取传递进这个页面的数据
    public final Object getDataIn() {
        return dataIn;
    }
    //传出去这个页面的数据
    public final void setDataOut(Object dataOut){this.dataOut = dataOut;}
    /**向其他页面传递数据 */
    public final Object transmitData() {
        return dataOut;
    }
    /**接收来自其他页面的数据*/
    public final void receiveData(Object dataIn) {
        this.dataIn = dataIn;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) return;
        SLog.print(this+" - onCreate ,savedInstanceState");
        setPrevTag(savedInstanceState.getString(STRUCT_SAVE_PREV,null));
        setNextTag(savedInstanceState.getString(STRUCT_SAVE_NEXT,null));
        recoverHide = savedInstanceState.getBoolean(STATE_SAVE_HIDDEN);
        dataIn = savedInstanceState.getSerializable(DATA_SAVE_IN);
        if (dataIn==null) dataIn = savedInstanceState.getParcelable(DATA_SAVE_IN);
        dataOut = savedInstanceState.getSerializable(DATA_SAVE_OUT);
        if (dataOut==null) dataOut = savedInstanceState.getParcelable(DATA_SAVE_OUT);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_SAVE_HIDDEN, isHidden());
        outState.putString(STRUCT_SAVE_PREV,getPrevTag());
        outState.putString(STRUCT_SAVE_NEXT,getNextTag());
        if (dataIn!=null){
            if ( dataIn instanceof Serializable){
                outState.putSerializable(DATA_SAVE_IN, (Serializable) dataIn);
            }
            if (dataIn instanceof Parcelable){
                outState.putParcelable(DATA_SAVE_IN, (Parcelable) dataIn);
            }
        }
        if (dataOut!=null){
            if ( dataOut instanceof Serializable){
                outState.putSerializable(DATA_SAVE_OUT, (Serializable) dataIn);
            }
            if (dataOut instanceof Parcelable){
                outState.putParcelable(DATA_SAVE_OUT, (Parcelable) dataIn);
            }
        }

        super.onSaveInstanceState(outState);
        SLog.print(this+" - onSaveInstanceState");
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState==null) return;
        SLog.print(this+" - onViewStateRestored");
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SLog.print(this+", onViewCreated"
                        +" getView().getWindowToken() = "+ getView().getWindowToken()
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        SLog.print(this+" - onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        SLog.print(this +" - onResume\n"
                        +" isVisible-"+isVisible()
                        +" isAdded-" + isAdded()
                        +" isHidden-" + isHidden()
                        +" isResumed-" + isResumed()
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        SLog.print(this+" - onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        SLog.print(this+" - onStop");
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        SLog.print(this +" - onHiddenChanged - hidden:" + hidden);
        if (!hidden){
            if (isResumed())  onResume();
        }else{
            if (isResumed()) onPause();
        }
    }


    //viewpage 调用
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        SLog.print(this+" - setUserVisibleHint - " + isVisibleToUser);
        if (isVisibleToUser) {
            //相当于Fragment的onResume
            if (isResumed()) onResume();
        } else {
            //相当于Fragment的onPause
            if (isResumed()) onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        SLog.print(this+" - onDestroyView");
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        SLog.print(this+" - onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        SLog.print(this+" - onDetach");
        mHandler = null;
    }

    /**
     * 发送消息
     */
    public void senSMessage(SMessage message) {
        if (mHandler!=null){
            mHandler.dispatch(message);
        }
    }

    /**
     * 发送消息到activity
     */
    public void sendMessageToActivity(SMessage message){
        if (mHandler!=null){} mHandler.getActivitySoftReference().dispatch(message);
    }


    protected boolean onBackPressed() {
        return false;
    }
}
