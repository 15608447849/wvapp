package lee.bottle.lib.singlepageframwork.base;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by Leeping on 2018/4/15.
 * email: 793065165@qq.com
 * 把一个布局容器 与 一组fragment 连接
 */

public final class SFGroup implements Closeable{

    private final String tag;
    //当前存在的所有fragment
    private final ArrayList<SFAttribute> activeGroupPages = new ArrayList<>();
    //上下文
    private Context context = null;
    //fragmentManager
    private FragmentManager fm = null;
    //容器布局id
    private int containerRid = -1;
    //关联的所有可用fragment的属性及编号
    private final HashSet<SFAttribute> pages;

    public SFGroup(Context context, String tag, FragmentManager fm, int containerRid, HashSet<SFAttribute> pages ) {
        this.tag = tag;
        this.pages = pages;
        this.fm = fm;
        this.containerRid = containerRid;
        this.context = context;
    }
    public ArrayList<SFAttribute> getActiveGroupPages() {
        return activeGroupPages;
    }

    //正在当前栈顶显示的 fragment属性
    private SFAttribute currentGroupPage = null;

    public SFAttribute getCurrentGroupPage() {
        return currentGroupPage;
    }

    //设置当前页面 ,如果为Null 则表示当前无显示
    public void setCurrentGroupPage(SFAttribute page) {
        if (page == null) {
            if (currentGroupPage !=null){
                activeGroupPages.remove(currentGroupPage);
            }
        }else{
            if (page!=currentGroupPage)
                activeGroupPages.add(page);
        }
        currentGroupPage = page;
    }

    public void removeGroupPage(SFAttribute page){
        activeGroupPages.remove(page);
    }

    //移除全部页面
    public void removeCurrentGroupAll(){
        Iterator<SFAttribute> iterator = getActiveGroupPages().iterator();
        SFAttribute entry;
        while (iterator.hasNext()){
            entry = iterator.next();
            iterator.remove();
//            SpaPrt.print(this+" , 移除页面: "+entry);
        }
        this.currentGroupPage = null;
    }
    //获取fragment管理器
    public FragmentManager getFm() {
        return fm;
    }

    //根据一个指定的标识获取一个fragment属性
    public SFAttribute getPage(String tag) {

        Iterator<SFAttribute> it = pages.iterator();
        SFAttribute entry;
        while (it.hasNext()){
            entry = it.next();
            if (entry.isSame(tag)) return entry;
        }
        return null;
    }

    //获取容器层id
    public int getContainerRid() {
        return containerRid;
    }

    //获取上下文
    public Context getContext() {
        return context;
    }

    public String getTag() {
        return tag;
    }

    @Override
    public void close() throws IOException {
        context = null;
        fm = null;
        containerRid = -1;
    }
}
