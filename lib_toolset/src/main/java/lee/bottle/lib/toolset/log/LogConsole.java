package lee.bottle.lib.toolset.log;

import android.util.Log;

/**
 * Created by Leeping on 2019/6/3.
 * email: 793065165@qq.com
 * 控制台输出实现
 */
class LogConsole implements ILogHandler{
    @Override
    public void handle(String tag,Build build, String content)  {
        if (!build.isWriteConsole) return;
        Log.println(build.level,tag,content);
    }
}
