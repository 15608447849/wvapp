package lee.bottle.lib.toolset.log;

/**
 * Created by Leeping on 2019/6/3.
 * email: 793065165@qq.com
 * 日志输出接口
 */
public interface ILogHandler {
    void handle(Build build, String content) throws Exception;
}
