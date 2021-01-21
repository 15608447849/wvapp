package lee.bottle.lib.toolset.util;

import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Leeping on 2018/6/27.
 * email: 793065165@qq.com
 */

public class TimeUtils {

    public static void getTargetTime(String timeStr, int[] arr){
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(timeStr);
            Calendar now = Calendar.getInstance();
            now.setTime(date);
            int year = now.get(Calendar.YEAR);
            int month = now.get(Calendar.MONTH) + 1;
            int day = now.get(Calendar.DATE);
            arr[0] = year;
            arr[1] = month;
            arr[2] = day;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public  static String formatUTCByCurrent() {
        return formatUTC(0,null);
    }
    /**
     * 格式化时间 时间戳转指定格式
     */
    public  static String formatUTCByCurrent(String strPattern) {
        return formatUTC(0,strPattern);
    }

    public static SimpleDateFormat getSimpleDateFormat(String s) {
        return new SimpleDateFormat(s, Locale.CHINA);
    }
    /**
     * 格式化时间 时间戳转指定格式
     */
    public  static String formatUTC(long l, String strPattern) {
        if (l==0) l = new Date().getTime();
        if (TextUtils.isEmpty(strPattern)) {
            strPattern = "yyyy-MM-dd HH:mm:ss";
        }
        return getSimpleDateFormat(strPattern).format(l);
    }

    /**
     * 指定格式日期 转 时间戳
     */
    public static void getTodayTime(int[] arr){
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DATE);
        arr[0] = year;
        arr[1] = month;
        arr[2] = day;
    }
    // 秒 转化 小时 ,分钟
    public static String formatDuring(long mss) {
        long days = mss / 86400000L;
        long hours = mss % 86400000L / 3600000L;
        long minutes = mss % 3600000L / 60000L;
        long seconds = mss % 60000L / 1000L;
        StringBuilder sb = new StringBuilder();
        if (days > 0L) {
            sb.append(days).append("天 ");
        }

        if (hours > 0L) {
            sb.append(hours).append("小时 ");
        }

        if (minutes > 0L) {
            sb.append(minutes).append("分钟 ");
        }

        if (seconds > 0L) {
            sb.append(seconds).append("秒 ");
        }

        if (sb.toString().length() == 0) {
            sb.append(mss).append("毫秒 ");
        }

        return sb.toString();
    }
    //比较时间
    public static boolean compTimeStr(String start, String end){

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            //获取Calendar实例
            Calendar startTime = Calendar.getInstance();
            Calendar endTime = Calendar.getInstance();
            //把字符串转成日期类型
            startTime.setTime(df.parse(start+" 00:00:00"));
            endTime.setTime(df.parse(end+" 23:59:59"));
            //利用Calendar的方法比较大小
            if (endTime.compareTo(startTime) > 0) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    //判断早中晚
    public static String getTimeBucketStr(){
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 8) {
            return "早上好";
        } else if (hour >= 8 && hour < 11) {
            return "上午好";
        } else if (hour >= 11 && hour < 13) {
            return"中午好";
        } else if (hour >= 13 && hour < 18) {
            return"下午好";
        } else {
            return"晚上好";
        }
    }

    //获取本月第一天的字符串
    public static String getMonthFirstDayStr() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH,1);//设置为1号,当前日期既为本月第一天
        return formatUTC(c.getTime().getTime(),"yyyy-MM-dd");
    }

    public static List<String> getYearNowToTarget(int target) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int tYear = year+target;
        List<String> list = new ArrayList<>();
        int minYear = Math.min(year, tYear);
        int maxYear = Math.max(year, tYear);
        for (;minYear<=maxYear ; minYear++){
            list.add(String.valueOf(minYear));
        }
        return list;
    }



}
