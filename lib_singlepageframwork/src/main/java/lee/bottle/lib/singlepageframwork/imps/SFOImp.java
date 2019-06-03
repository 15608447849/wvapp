package lee.bottle.lib.singlepageframwork.imps;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;
import java.util.List;

import lee.bottle.lib.singlepageframwork.base.SFAttribute;
import lee.bottle.lib.singlepageframwork.base.SFGroup;
import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.singlepageframwork.infs.SFOInterface;

/**
 * Created by Leeping on 2018/4/16.
 * email: 793065165@qq.com
 */

public class SFOImp implements SFOInterface {

    private class Helper{
        /**
         * 检测是否存在栈(链表结构)上fragment
         */
        private SFragment checkFragmentNext(SFragment spaBaseFragment) {
            if (spaBaseFragment.getNext()!=null){
                return checkFragmentNext(spaBaseFragment.getNext());
            }
            return spaBaseFragment;
        }

        /**
         * 获取栈中所有fragment
         */
        private void getFragmentStackAll(SFragment spaBaseFragment,List<SFragment> list) {
            list.add(spaBaseFragment);
            if (spaBaseFragment.getNext()!=null){
                getFragmentStackAll(spaBaseFragment.getNext(),list);
            }
        }
        /**提交*/
        private void commit(FragmentTransaction ft){
            synchronized (this){
                try{
                    ft.commit();
                }catch (Exception e){
                    e.printStackTrace();
                    ft.commitAllowingStateLoss();
                }
            }
        }
    }

    private Helper h = new Helper();

    /**
     * 查询一个fragment对象
     */
    @Override
    public SFragment queryFragmentByTag(SFGroup pageHolder, SFAttribute gAttribute) {
        FragmentManager fm = pageHolder.getFm();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment!=null) return (SFragment) fragment;
        return null;
    }

    /**
     * 检测栈顶是否与目标相同
     */
    @Override
    public boolean checkTargetIsStackTop(SFGroup pageHolder, SFAttribute gAttribute) {
        FragmentManager fm = pageHolder.getFm();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment != null){
            return fragment.isVisible() ; //显示中或者状态已经超过onresume - true || (fragment.isResumed() && !fragment.isHidden())
        }
        return false;
    }

    /**
     * 1.创建/添加 组fragment
     * 2.如果存在,显示栈顶fragment
     */
    @Override
    public boolean showGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        boolean flag = false;
        int containerRid = pageHolder.getContainerRid();
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment == null){
            //不存在,创建,添加 -且为栈组fragment
            fragment = Fragment.instantiate(pageHolder.getContext(),gAttribute.getClassPath(),gAttribute.getArgs());
            ft.add(containerRid,fragment,gAttribute.getTagName());
            flag = true;
        }else{
            //存在,显示栈顶
            fragment = h.checkFragmentNext((SFragment)fragment);
            if (!fragment.isVisible()){
                //不在显示中-> 显示
                ft.show(fragment);
                flag = true;
            }
        }
        if (flag) h.commit(ft);
        return  flag;
    }

    /**
     * 向组fragment添加下一个fragment栈顶并显示
     * (添加碎片进'回退栈'操作)
     */
    @Override
    public void showGroupFragmentByOnlyStackTop(SFGroup pageHolder,
                                                SFAttribute gAttribute,
                                                SFAttribute cAttribute) {
        int containerRid = pageHolder.getContainerRid();
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment gFragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (gFragment !=null ){
            SFragment stackTop =
                    h.checkFragmentNext((SFragment)gFragment);

            SFragment newStackTop =
                    (SFragment)Fragment.instantiate(pageHolder.getContext(),cAttribute.getClassPath(),cAttribute.getArgs());

            Object data = stackTop.transmitData();//当前栈顶传送到下一个栈顶的消息
            if (data!=null) newStackTop.receiveData(data);//新栈顶接受传递的消息

            if (stackTop.isVisible()){

                if (stackTop.isKillSelf() && stackTop.getPrev() != null){ //排除组fragment
                    ft.remove(stackTop);//移除栈顶
                    stackTop = stackTop.getPrev();//指向它的前一层
                }else{
                    ft.hide(stackTop);//隐藏栈顶
                }
            }

            ft.add(containerRid,newStackTop,cAttribute.getTagName());
            //关联
            stackTop.setNext(newStackTop);
            newStackTop.setPrev(stackTop);

            h.commit(ft);
        }
    }

    /**
     * 1.隐藏组或栈顶fragment
     */
    @Override
    public boolean hindGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment !=null ){

            SFragment f = h.checkFragmentNext((SFragment)fragment);
            if (f.isVisible()){
                if (f.isKillSelf() && f.getNext() == null){
                    //结束组- 移除
                    ft.remove(f);
                }else{
                    //隐藏
                    ft.hide(fragment);
                }
                h.commit(ft);
                return true;
            }
        }
        return false;
    }



    /**
     * 移除组fragment,包括此组栈中所有fragment
     */
    @Override
    public void removeGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment!=null){
            List<SFragment> list = new ArrayList<>();
            h.getFragmentStackAll((SFragment)fragment,list);
            for (SFragment f:list){
                ft.remove(f);
            }
            h.commit(ft);
        }
    }

    /**
     * 1.如果存在栈,移除指定组存在的一个栈顶fragment
     * 显示上一个栈顶
     * (回退操作)
     */
    @Override
    public void removeGroupFragmentByOnlyStackTop(SFGroup pageHolder, SFAttribute gAttribute) {
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentByTag(gAttribute.getTagName());
        if (fragment!=null){
            SFragment gFragment = (SFragment) fragment;
            if (gFragment.getNext()!=null){
                SFragment stackTop =
                        h.checkFragmentNext(gFragment.getNext());
                SFragment prevFragment = stackTop.getPrev();

                //数据传递
                Object data = stackTop.transmitData();
                if (data!=null) prevFragment.receiveData(data);
                //解除栈元素关联
                prevFragment.setNext(null);
                stackTop.setPrev(null);
                ft.remove(stackTop);
                ft.show(prevFragment);
                h.commit(ft);
            }
        }
    }

    /**
     * 移除当前活动的所有fragment
     */
    @Override
    public void removeGroupFragmentByActiveAll(SFGroup pageHolder) {
        FragmentManager fm = pageHolder.getFm();
        FragmentTransaction ft = fm.beginTransaction();
        ArrayList<SFAttribute> activeGroupPages = pageHolder.getActiveGroupPages();
        List<SFragment> fragmentAll = new ArrayList<>();
        Fragment temp;
        for (SFAttribute gAttribute : activeGroupPages){
            removeGroupFragment(pageHolder,gAttribute);
            temp = fm.findFragmentByTag(gAttribute.getTagName());
            if (temp!=null)  h.getFragmentStackAll((SFragment)temp,fragmentAll);
        }
        for (SFragment f:fragmentAll){
            ft.remove(f);
        }
        h.commit(ft);
    }
}
