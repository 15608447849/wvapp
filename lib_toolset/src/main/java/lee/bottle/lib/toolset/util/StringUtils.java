package lee.bottle.lib.toolset.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

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
        return sb.toString();
    }

}
