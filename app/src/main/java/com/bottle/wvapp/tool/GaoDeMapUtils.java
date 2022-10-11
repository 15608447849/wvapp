package com.bottle.wvapp.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lee.bottle.lib.toolset.http.HttpRequest;
import lee.bottle.lib.toolset.log.LLog;
import lee.bottle.lib.toolset.util.GsonUtils;
import lee.bottle.lib.toolset.util.StringUtils;

/**
 * @Author: leeping
 * @Date: 2019/4/12 15:29
 */
public class GaoDeMapUtils {

    public static String apiKey = "c59217680590515b7c8369ff5e8fe124";

    public static class JsonBean{
        public String city;
    }

    /**
     * 获取外网的IP
     */
    private static String getNetIp() {
        URL infoUrl;
        InputStream inStream = null;
        String ipLine = "";
        HttpURLConnection httpConnection = null;
        try {
            infoUrl = new URL("http://pv.sohu.com/cityjson?ie=utf-8");
            URLConnection connection = infoUrl.openConnection();
            httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inStream = httpConnection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inStream, StandardCharsets.UTF_8));
                StringBuilder strber = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null){
                    strber.append(line).append("\n");
                }
                Pattern pattern = Pattern
                        .compile("((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))");
                Matcher matcher = pattern.matcher(strber.toString());
                if (matcher.find()) {
                    ipLine = matcher.group();
                }
            }
        } catch (Exception e) {
            LLog.error(e);
        }finally {
            try {
                if (inStream!=null) inStream.close();
            } catch (Exception ignored) { }
            try {
                if (httpConnection!=null)  httpConnection.disconnect();
            } catch (Exception ignored) { }

        }
        return ipLine;
    }
    /**
     * ip转地址信息
     */
    public static JsonBean ipConvertAddress(){
            StringBuffer sb = new StringBuffer( "https://restapi.amap.com/v3/ip?");
            HashMap<String,String> map = new HashMap<>();
            map.put("key",apiKey);
            map.put("ip",getNetIp());
            String result = new HttpRequest().bindParam(sb,map).getRespondContent();
            if(StringUtils.isEmpty(result)) return null;
            return GsonUtils.jsonToJavaBean(result,JsonBean.class);
    }

}
