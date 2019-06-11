package com.bottle.lib.crosswalk;

import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by Leeping on 2019/6/11.
 * email: 793065165@qq.com
 */
public class CKWebChromeClient extends XWalkUIClient{
    public CKWebChromeClient(XWalkView view) {
        super(view);
    }

    @Override
    public boolean onConsoleMessage(XWalkView view, String message, int lineNumber, String sourceId, ConsoleMessageType messageType) {
        LLog.print(
              "浏览器控制台输出 - ["+messageType.name()+"]\t"+message
        );
        return true;
    }
}
