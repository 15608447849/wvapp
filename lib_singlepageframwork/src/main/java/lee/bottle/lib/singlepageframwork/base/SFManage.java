package lee.bottle.lib.singlepageframwork.base;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Iterator;
import java.util.List;

import lee.bottle.lib.singlepageframwork.imps.SFOImp;
import lee.bottle.lib.singlepageframwork.infs.SFOInterface;

/**
 * Created by Leeping on 2018/4/15.
 * email: 793065165@qq.com
 *
 * activity 实现 SpaActivity
 * 设置SpaAttr实现( 关于所有的Fragment的相关信息等 )
 */

public final class SFManage implements SFOInterface {

    private SFManage() {}

    private static class Holder{
        private static SFManage INSTANCE = new SFManage();
    }
    public static SFManage getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * fragment 操作具体实现
     */
    private SFOInterface spaFragmentOperation = new SFOImp();

    /**
     * 当前activity 是否显示
     *
     * @param isResume
     */
    @Override
    public void setActivityResume(boolean isResume) {
        spaFragmentOperation.setActivityResume(isResume);
    }

    @Override
    public SFragment queryFragmentByTag(SFGroup pageHolder, SFAttribute gAttribute) {
        if (gAttribute==null) return null;
        return  spaFragmentOperation.queryFragmentByTag(pageHolder,gAttribute);
    }

    /**
     * 判断当前栈顶是否是指定目标
     */
    @Override
    public boolean checkTargetIsStackTop(SFGroup pageHolder, SFAttribute gAttribute) {

        if (pageHolder.getCurrentGroupPage()!=null && pageHolder.getCurrentGroupPage().equals(gAttribute)) {
            return spaFragmentOperation.checkTargetIsStackTop(pageHolder, gAttribute);
        }
        return false;
    }

    /**
     * 1.创建添加显示组fragment
     * 2.如果存在,显示组或者栈顶fragment
     * 3.设置当前组碎片
     */
    @Override
    public boolean showGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        if (gAttribute == null) return false;
        boolean flag = spaFragmentOperation.showGroupFragment(pageHolder,gAttribute);
        pageHolder.setCurrentGroupPage(gAttribute);
        if (flag){
            SLog.print(this+ " , 显示组碎片 : "+gAttribute.getTagName());
        }
        return flag;
    }

    /**
     * 向组fragment添加下一个fragment并显示(添加进回退栈操作)
     */
    @Override
    public void showGroupFragmentByOnlyStackTop(SFGroup pageHolder, SFAttribute gAttribute, SFAttribute cAttribute) {
        if (gAttribute==null || cAttribute==null || gAttribute.equals(cAttribute)) return; //避免重复添加组fragment
        SLog.print(this+ " , 向组中添加栈顶元素 : 组碎片"+gAttribute.getTagName()+" - 等待添加的栈顶碎片"+ cAttribute.getTagName());
        spaFragmentOperation.showGroupFragmentByOnlyStackTop(pageHolder,gAttribute,cAttribute);
    }

    /**
     * 1.隐藏组或栈顶fragment
     */
    @Override
    public boolean hindGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        if (gAttribute==null) return false;
        boolean flag = spaFragmentOperation.hindGroupFragment(pageHolder,gAttribute);
        if (flag) SLog.print(this+ " , 隐藏组碎片 : "+gAttribute.getTagName());
        return flag;
    }

    /**
     * 1.移除组fragment,包括此组栈中所有fragment
     */
    @Override
    public void removeGroupFragment(SFGroup pageHolder, SFAttribute gAttribute) {
        if (gAttribute==null) return;
        SLog.print(this + " ,移除组碎片  "+gAttribute.getTagName());
        spaFragmentOperation.removeGroupFragment(pageHolder,gAttribute);
        if (pageHolder.getCurrentGroupPage()!=null && pageHolder.getCurrentGroupPage().equals(gAttribute)) {
            pageHolder.setCurrentGroupPage(null);
        }else{
            pageHolder.removeGroupPage(gAttribute);
        }
    }

    /**
     * 1.如果存在栈,移除指定组存在的一个栈顶fragment,显示上一个栈顶(回退操作)
     */
    @Override
    public void removeGroupFragmentByOnlyStackTop(SFGroup pageHolder, SFAttribute attribute) {
        if (attribute==null) return;
        SLog.print(this+ " ,移除组栈顶  "+attribute.getTagName());
        spaFragmentOperation.removeGroupFragmentByOnlyStackTop(pageHolder,attribute);
    }

    /**
     * 移除当前活动的所有fragment
     */
    @Override
    public void removeGroupFragmentByActiveAll(SFGroup pageHolder) {
        SLog.print(this+ " ,移除全部活动碎片");
        spaFragmentOperation.removeGroupFragmentByActiveAll(pageHolder);
        pageHolder.removeCurrentGroupAll();
    }

    /**还原fragment的状态*/
    public void recovery(SFGroup spaFragmentPageHolder, List<SFragment> recoveryFragments) {
        SLog.print(this+ " recovery() ,  还原状态 "+spaFragmentPageHolder.getTag());
        FragmentManager fm = spaFragmentPageHolder.getFm();
        SFragment temp;
        for (SFragment fragment : recoveryFragments){
            if (!SFAttribute.parsePageTag(fragment.getTag()).equals(spaFragmentPageHolder.getTag())){
                continue;
            }
            if (fragment.getNextTag()!=null && fragment.getNext()==null){
                temp = (SFragment) fm.findFragmentByTag(fragment.getNextTag());
                fragment.setNext(temp);
                temp.setPrev(fragment);
                SLog.print(this+ ",关联回退栈关系: "+fragment.getTag()+ " -> "+ temp.getTag());
            }

            if (fragment.getPrevTag()!=null && fragment.getPrev()==null){
                temp = (SFragment) fm.findFragmentByTag(fragment.getPrevTag());
                fragment.setPrev(temp);
                temp.setNext(fragment);
                SLog.print(this+ " ,关联回退栈关系: "+temp.getTag()+" -> "+fragment.getTag());
            }
        }
        FragmentTransaction ft = fm.beginTransaction();
        //隐藏所有
        for (Fragment f : recoveryFragments){
            ft.hide(f);
        }
        ft.commit();

        Iterator<SFragment> iterator = recoveryFragments.iterator();
        SFAttribute pageAttr;

        while (iterator.hasNext()){
            temp = iterator.next();
            if (!SFAttribute.parsePageTag(temp.getTag()).equals(spaFragmentPageHolder.getTag())){
                continue;
            }
            iterator.remove();

            SLog.print(this+ " : "+ temp +" - isHide = " + temp.isRecoverHide());

            if (temp.isRecoverHide()){

               /* if (temp.isVisible()){
                    SLog.print(this+ ", 隐藏碎片 "+temp.getTag()+" visible = "+temp.isVisible());
                    ft = fm.beginTransaction();
                    ft.hide(temp);
                    ft.commit();
                    while (temp.isVisible());
                }
*/
            }else{
               // SLog.print(this+ ", 显示碎片 "+temp.getTag() + " visible = "+ temp.isVisible() );
//                if (!temp.isVisible()){
                    ft = fm.beginTransaction();
                    ft.show(temp);
                    ft.commit();
//                    while (!temp.isVisible());
//                }

                //寻找组碎片
                temp = findGroupFragment(temp);
                pageAttr = spaFragmentPageHolder.getPage(SFAttribute.parseFragmentTag(temp.getTag()));
                spaFragmentPageHolder.setCurrentGroupPage(pageAttr);
                SLog.print(this+ ", 设置当前界面显示的组碎片 "+ pageAttr.getTagName());
            }
        }
    }

    /**查询组fragment*/
    private SFragment findGroupFragment(SFragment fragment){
        if (fragment.getPrev()!=null){
            return findGroupFragment(fragment.getPrev());
        }
        return fragment;
    }




}
