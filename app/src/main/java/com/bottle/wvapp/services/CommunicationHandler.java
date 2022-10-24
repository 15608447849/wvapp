package com.bottle.wvapp.services;

import android.content.Intent;

import lee.bottle.lib.toolset.os.ApplicationDevInfo;

import com.bottle.wvapp.activitys.NativeActivity;
import com.bottle.wvapp.app.WebApplication;
import com.bottle.wvapp.tool.NotifyUer;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;

import static com.bottle.wvapp.beans.BusinessData.getCurrentDevCompanyID;

public class CommunicationHandler {
    private final SoftReference<IMService> imServiceRef;

    protected CommunicationHandler(IMService imService) {
        this.imServiceRef = new SoftReference<>(imService);
    }

    /*  同步服务器上的用户信息 */
    private int updateLocalCacheReturnCompanyID(){
        int compid = getCurrentDevCompanyID(true, WebApplication.iceClient);
        LLog.print("更新本地用户信息 compid = "+ compid);
        if (compid == 0){
            // 断开连接
            imServiceRef.get().communicationClose();
        }
        return compid;
    }

    void handlerMessage(String message) {
        try {

            LLog.print("处理长连接消息: "+ message);
            String prop = message.substring(0,message.indexOf(":"));
            String msg = message.substring(message.indexOf(":")+1);

            Map<String,String> map = new LinkedHashMap<>();

            protocol_ref(prop,msg,map);
            protocol_logout(prop,msg,map);
            protocol_payResult(prop,msg,map);
            protocol_pushMessage(prop,msg,map);
            protocol_alertMessage(prop,msg,map);

            if (map.isEmpty()) return;
            imServiceRef.get().sendDataToNativeActivity(map);
        } catch (Exception e) {
            LLog.print("接收长连接消息异常: "+ message+" , " + e.getMessage());
        }
    }


    //更新本地企业信息
    private void protocol_ref(String prop, String msg, Map<String,String> intent) {
        if (prop.equals("ref")){
            LLog.print("服务器用户信息存在更新");
            updateLocalCacheReturnCompanyID();
        }
    }

    //其他设备登录后 广播 强制登录指令
    private void protocol_logout(String prop, String msg, Map<String,String> intent) {
        if (prop.startsWith("logout")){
            LLog.print("登出信息: "+ msg);

            // 刷新用户企业信息(与服务器同步)
            int compid = updateLocalCacheReturnCompanyID();
            String devID = msg.substring(0,msg.lastIndexOf("@"));
            String devType = msg.substring(msg.lastIndexOf("@")+1);
            String curDevID =  ApplicationDevInfo.getShareDEVID(imServiceRef.get().getApplication()) ;

            LLog.print("公司标识: " + compid + "\n目标设备: " + devID + "\n当前设备: " + curDevID);

            if (devType.equals(WebApplication.DEVTYPE)){
                if (devID.equals(curDevID)){
                    // 同一个设备
                    //intent.put("alertTipWindow","您的账号已从当前设备退出");// 弹窗提醒
                }else{
                    // 不同设备
                    if (prop.equals("logout-force")){
                        // 当前设备需要强制登出
                        // 通知activity 强制退出
                        intent.put("forceLogout","true");
                        //message = "您的账号正在其他设备登录";
                    }
                }
            }else{
                // 其他类型的终端
                //intent.put("alertTipWindow","您的账号已在其他终端("+devType+")退出");// 弹窗提醒
            }
        }
    }

    //支付结果
    private void protocol_payResult(String prop, String msg, Map<String,String> intent) {
//    String msg = "pay:" + jsonObject.toJSONString();
//            IceRemoteUtil.sendMessageToClient(compid, msg);
        if (prop.equals("pay")){
            LLog.print("支付结果: "+ msg);
            // 通知actyivity 支付结果
            intent.put("pushPaySuccessMessageToJs",msg);

        }
    }

    //推送消息
    private void protocol_pushMessage(String prop, String msg, Map<String,String> intent) {

        if (prop.equals("push") || prop.equals("custom")){
            LLog.print("推送消息: "+ msg);

            String content = msg;
            String likePath = null;

            if (prop.startsWith("custom")){
                // 自定义推送消息: 内容;链接
                String[] arr = content.split(";");
                if (arr.length >= 1){
                    content = arr[0];
                }
                if (arr.length >= 2){
                    likePath = arr[1];
                }
            }

            Map<String,String> map = new HashMap<>();
            map.put("content",content);
            map.put("likePath",likePath);
            // 发送推送消息
            intent.put("pushMessageToJs", GsonUtils.javaBeanToJson(map));

        }
    }

    // 强制弹框
    private void protocol_alertMessage(String prop,String msg,Map<String,String> intent){
        if (prop.equals("alert")){
            intent.put("alertTipWindow",msg);
        }
    }


}
