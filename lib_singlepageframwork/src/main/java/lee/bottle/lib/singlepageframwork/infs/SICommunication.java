package lee.bottle.lib.singlepageframwork.infs;

import lee.bottle.lib.singlepageframwork.base.SMessage;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public interface SICommunication {
    /**
     * fragment发送消息到activity 或者 一个可以对消息做出处理的实现类
     * 返回 true 则被拦截-处理
     * */
    boolean dispatch(SMessage message);
}
