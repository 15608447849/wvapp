package com.bottle.lib.crosswalk;

import org.xwalk.core.XWalkJavascriptResult;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.DialogUtil;

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

    @Override
    public boolean onJsConfirm(XWalkView view, String url, String message, final XWalkJavascriptResult result) {
        DialogUtil.dialogSimple2(view.getContext(), message, "确认", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.confirm();
            }
        }, "取消", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.cancel();
            }
        });
        return true;
    }

    @Override
    public boolean onJsAlert(XWalkView view, String url, String message,final XWalkJavascriptResult result) {
        DialogUtil.dialogSimple(view.getContext(), message, "确认", new DialogUtil.Action0() {
            @Override
            public void onAction0() {
                result.confirm();
            }
        });
        return true;
    }

    @Override
    public void onPageLoadStarted(XWalkView view, String url) {
        //LLog.print("开始加载:" + url);
        super.onPageLoadStarted(view, url);
    }
}
