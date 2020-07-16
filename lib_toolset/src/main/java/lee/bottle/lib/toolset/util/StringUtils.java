package lee.bottle.lib.toolset.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;

public class StringUtils {


    /*字符串不为空*/
    public static boolean isEmpty(String str){
        return str == null || str.trim().length() == 0 ;
    }

    /*判断一组字符串都不为空*/
    public static boolean isEmpty(String... arr){
        for (String str : arr){
            if (isEmpty(str)) return true;
        }
        return false;
    }

    /* 文字转拼音大写字母 */
    public static String converterToFirstSpell(String chines) {
        String pinyinFirstKey = "";
        char[] nameChar = chines.toCharArray();
        HanyuPinyinOutputFormat defaultFormat = new HanyuPinyinOutputFormat();
        defaultFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        defaultFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        for (int i = 0; i < nameChar.length; i++) {
            String s = String.valueOf(nameChar[i]);
            if (s.matches("[\\u4e00-\\u9fa5]")) {
                try {
                    String[] mPinyinArray = PinyinHelper.toHanyuPinyinStringArray(nameChar[i], defaultFormat);
                    pinyinFirstKey += mPinyinArray[0].charAt(0);
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    e.printStackTrace();
                }
            } else {
                pinyinFirstKey += nameChar[i];
            }
        }
        return pinyinFirstKey.toUpperCase();
    }

    /**
     * 将奇数个转义字符变为偶数个
     * @param s
     * @return
     */
    public static String getDecodeJSONStr(String s){
        StringBuilder sb = new StringBuilder();
        char c;
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString()
                .replaceAll("%","%25")
                .replaceAll("\\+","%2B")
                .replaceAll("\\s+","%20")
                .replaceAll("/","%2F")
                .replaceAll("\\?","%3F")
                .replaceAll("#","%23")
                .replaceAll("&","%26")
                .replaceAll("=","%26")
                ;

    }

    /**
     　　* 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     　　*/
    public static String mapToString(Map map){
        try{
            StringBuffer sb = new StringBuffer();
            Iterator<Map.Entry> it = map.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry e = it.next();
                sb.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue()+"","UTF-8")).append("&");
            }
            sb = sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }catch (Exception e){
            e.printStackTrace();
        }
       return null;
    }

    /**
     * byte->16进制字符串
     * @param bytes
     * @return
     */
    public static String byteToHexString(byte[] bytes) {
        StringBuilder hexStr = new StringBuilder();
        int num;
        for (byte aByte : bytes) {
            num = aByte;
            if (num < 0) {
                num += 256;
            }
            if (num < 16) {
                hexStr.append("0");
            }
            hexStr.append(Integer.toHexString(num));
        }
        return hexStr.toString().toUpperCase();
    }

    /**
     * 获取一段字节数组的md5
     * @param buffer
     * @return
     */
    public static byte[] getBytesMd5(byte[] buffer) {
        byte[] result = null;
        try { result =  MessageDigest.getInstance("MD5").digest(buffer); } catch (Exception ignored) { }
        return result;
    }

    /* 获取字符串的MD5 */
    public static String strMD5(String str){
        return byteToHexString(getBytesMd5(str.getBytes()));
    }

}
