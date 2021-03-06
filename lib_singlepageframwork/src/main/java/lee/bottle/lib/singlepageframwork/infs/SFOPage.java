package lee.bottle.lib.singlepageframwork.infs;

import lee.bottle.lib.singlepageframwork.base.SFragment;

/**
 * Created by Leeping on 2018/6/14.
 * email: 793065165@qq.com
 * 对碎片的操作
 */
public interface SFOPage {
    /**查询一个碎片*/
    SFragment query(String pageHolderTag, String gPageTag);
    /**页面的跳转*/
    void skip(String pageHolderTag, String gPageTag);
    /**页面的隐藏*/
    void hidden(String pageHolderTag, String gPageTag);
    /**页面的移除*/
    void remove(String pageHolderTag, String gPageTag);
    /**在当前组页面栈添加一个页面*/
    void addStack(String pageHolderTag, String cPageTag);
    /**当前页面退出回退栈*/
    void back(String pageHolderTag);
    /**全部页面清理*/
    void clearAll(String pageHolderTag);
}
