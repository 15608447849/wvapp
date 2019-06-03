package lee.bottle.lib.singlepageframwork.imps;

import java.util.HashMap;

import lee.bottle.lib.singlepageframwork.base.SFGroup;
import lee.bottle.lib.singlepageframwork.base.SFManage;
import lee.bottle.lib.singlepageframwork.base.SFragment;
import lee.bottle.lib.singlepageframwork.infs.SFOPage;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public class SFOPageImps implements SFOPage {
    /**
     * 所有碎片及当前显示碎片等属性管理
     */
    public final HashMap<String, SFGroup> groupMap = new HashMap<>();

    private final SFManage manage = SFManage.getInstance();

    /**
     * 查询一个碎片
     *
     * @param pageHolderTag
     * @param gPageTag
     */
    @Override
    public SFragment query(String pageHolderTag, String gPageTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return null;
        return manage.queryFragmentByTag(pageHolder,pageHolder.getPage(gPageTag));
    }

    /**
     * 页面的跳转
     *
     * @param pageHolderTag
     * @param gPageTag
     */
    @Override
    public void skip(String pageHolderTag, String gPageTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return;
        //如果当前显示的页面和需要显示的页面一致, 不操作
        if (manage.checkTargetIsStackTop(pageHolder,pageHolder.getPage(gPageTag))) return;
        //隐藏当前页面
        manage.hindGroupFragment(pageHolder,pageHolder.getCurrentGroupPage());
        //显示一个页面
        manage.showGroupFragment(pageHolder,pageHolder.getPage(gPageTag));
    }

    /**
     * 页面的隐藏
     *
     * @param pageHolderTag
     * @param gPageTag
     */
    @Override
    public void hidden(String pageHolderTag, String gPageTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder == null) return;
        manage.hindGroupFragment(pageHolder,pageHolder.getPage(gPageTag));
    }

    /**
     * 页面的移除
     *
     * @param pageHolderTag
     * @param gPageTag
     */
    @Override
    public void remove(String pageHolderTag, String gPageTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return;
       manage.removeGroupFragment(pageHolder,pageHolder.getPage(gPageTag));
    }

    /**
     * 在当前组页面栈添加一个页面
     *
     * @param pageHolderTag
     * @param cPageTag
     */
    @Override
    public void addStack(String pageHolderTag, String cPageTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return;
        //添加页面在当前栈
       manage.showGroupFragmentByOnlyStackTop(pageHolder,pageHolder.getCurrentGroupPage(),pageHolder.getPage(cPageTag));
    }

    /**
     * 当前页面退出回退栈
     *
     * @param pageHolderTag
     */
    @Override
    public void back(String pageHolderTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return;
       manage.removeGroupFragmentByOnlyStackTop(pageHolder,pageHolder.getCurrentGroupPage());
    }

    /**
     * 全部页面清理
     *
     * @param pageHolderTag
     */
    @Override
    public void clearAll(String pageHolderTag) {
        SFGroup pageHolder = groupMap.get(pageHolderTag);
        if (pageHolder==null) return;
       manage.removeGroupFragmentByActiveAll(pageHolder);
    }
}
