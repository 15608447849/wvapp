package lee.bottle.lib.singlepageframwork.infs;

/**
 * Created by Leeping on 2019/5/17.
 * email: 793065165@qq.com
 */
public interface SIDataStorage {
    void putData(String key,Object val);
    <T> T getData(String key,T def);
    <T> T removeData(String key,T def);
}
