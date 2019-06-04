package lee.bottle.lib.singlepageframwork.infs;

import lee.bottle.lib.singlepageframwork.base.SFAttribute;
import lee.bottle.lib.singlepageframwork.base.SFGroup;
import lee.bottle.lib.singlepageframwork.base.SFragment;

/**
 * Created by Leeping on 2018/4/15.
 * email: 793065165@qq.com
 */

public interface SFOInterface {
    /** 当前activity 是否显示 */
    void setActivityResume(boolean isResume);

    /** 查询一个fragment对象*/
    SFragment queryFragmentByTag(SFGroup pageHolder, SFAttribute gAttribute);
    /**检测栈顶是否与目标相同*/
    boolean checkTargetIsStackTop(SFGroup pageHolder, SFAttribute gAttribute);
    /**
     * 1.创建添加显示组fragment
     * 2.如果存在,显示组或者栈顶fragment
     */
    boolean showGroupFragment(SFGroup pageHolder, SFAttribute gAttribute);
    /**
     * 向组fragment添加下一个fragment并显示(添加进回退栈操作)
     */
    void showGroupFragmentByOnlyStackTop(SFGroup pageHolder, SFAttribute gAttribute, SFAttribute cAttribute);
    /**
     * 1.隐藏组或栈顶fragment
     */
    boolean hindGroupFragment(SFGroup pageHolder, SFAttribute gAttribute);
    /**
     * 1.移除组fragment,包括此组栈中所有fragment
     */
     void removeGroupFragment(SFGroup pageHolder, SFAttribute gAttribute);
    /**
     *
     * 1.如果存在栈,移除指定组存在的一个栈顶fragment,显示上一个栈顶(回退操作)
     */
    void removeGroupFragmentByOnlyStackTop(SFGroup pageHolder, SFAttribute attribute);
    /**
     * 移除当前活动的所有fragment
     * */
    void removeGroupFragmentByActiveAll(SFGroup pageHolder);

}

