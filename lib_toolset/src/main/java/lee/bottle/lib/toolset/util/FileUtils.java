package lee.bottle.lib.toolset.util;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lee.bottle.lib.toolset.log.LLog;

/**
 * Created by lzp on 2017/5/9.
 *
 */
public class FileUtils {
    public static final String PROGRESS_HOME_PATH = ".";
    public static final String SEPARATOR = "/";//File.separator;
    private static byte[] ENDFD = "\n".getBytes();

    /**
     * 替换文件分隔符,检测文件前缀或后缀
     */
    public static String replaceFileSeparatorAndCheck(String path,String prefix,String suffix){
        path = path.replace("\\",SEPARATOR);
        if (!StringUtils.isEmpty(prefix)){
            if (path.startsWith(prefix)){
                path = path.substring(1);
            }
        }
        if (!StringUtils.isEmpty(suffix)){
            if (path.endsWith(suffix)){
                path = path.substring(0,path.length()-1);
            }
        }
        return  path;
    }

    /**检查目录 存在返回true*/
    public static boolean checkDir(String dir){
        File dirs = new File(dir);
        if(!dirs.exists()){
//            ("mkdirs file : "+ dirs.getAbsolutePath());
            return dirs.mkdirs();
        }
        return true;
    }

    /**
     * 删除单个文件
     *
     * @param sPath
     *            被删除文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String sPath) {

            File file = new File(sPath);
        if ( file.exists() && file.isFile()) {
//            ("delete file : "+ file.getAbsolutePath());
            return file.delete();
        }
        return false;
    }

    public static void closeStream(FileChannel in, FileChannel out){
        try {
            if (in != null) in.close();

        } catch (IOException e) {
            LLog.error(e);
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            LLog.error(e);
        }

    }
    public static void closeStream(InputStream in, OutputStream out, RandomAccessFile raf,HttpURLConnection httpConnection){
        try {
            if (in != null) in.close();

        } catch (IOException e) {
            LLog.error(e);
        }
        try {
            if (httpConnection != null) httpConnection.disconnect();

        } catch (Exception e) {
            LLog.error(e);
        }
        try {
            if (out != null) out.close();

        } catch (IOException e) {
            LLog.error(e);
        }
        try {
            if (raf != null) raf.close();
        } catch (IOException e) {
            LLog.error(e);
        }

    }
    //从命名 不会删除源文件,需要自行判断决定是否删除
    public static boolean rename(File sourceFile,File targetFile){
        try {
            if (sourceFile.renameTo(targetFile)){
                return true;
            }else{
                if (copyFile(sourceFile, targetFile)){
                    return true;
                }
            }
        } catch (Exception e) {
            LLog.error(e);
        }
        return false;
    }

    public static boolean copyFile(File source,File target) throws IOException {

        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new RandomAccessFile(source,"rw").getChannel();
            out = new RandomAccessFile(target,"rw").getChannel();
            in.transferTo(0, in.size(), out);//连接两个通道，并且从in通道读取，然后写入out通道
            out.close();
            in.close();
//            ("启动文件复制: "+ source+ " - "+ target+" 成功.");

            return true;
        }  catch (IOException e) {
            LLog.error(e);
            return false;
        } finally {
            closeStream(in,out);
        }
    }

    /**
     * 保存对象到文件
     * @param obj
     */
    public static void writeObjectToFile(Object obj, String filePath)
    {
        File file =new File(filePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            ObjectOutputStream objOut=new ObjectOutputStream(out);

            objOut.writeObject(obj);
            objOut.flush();
            objOut.close();
//            ("序列化","成功");
        } catch (Exception e) {
            LLog.error(e);
        }
    }



    public static Object readObjectFromFile(String filePath)
    {
        Object temp=null;
        File file =new File(filePath);
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn=new ObjectInputStream(in);
            temp=objIn.readObject();
            objIn.close();
        } catch (Exception e) {
            LLog.error(e);
        }
        return temp;
    }


    public static boolean writeMapToFile(Map<String,String> maps, String path){
        try {
            Map<String,String> map = maps;
            File file = new File(path);
            if (!file.exists()) file.createNewFile();

            StringBuilder str = new StringBuilder();
            Iterator iter = map.entrySet().iterator();
            while(iter.hasNext()){
                Map.Entry entry = (Map.Entry)iter.next();
                str.append(entry.getKey()+"="+entry.getValue()).append("\n");
            }
            FileWriter fw = new FileWriter(path, false);
            fw.write(str.toString());
            fw.flush();
            fw.close();
            return true;
        } catch (IOException e) {
            LLog.error(e);
        }
        return false;
    }

    public static Map<String,String> readFileToMap(String path){
        try {
            File file = new File(path);
            if (!file.exists()) return null;
            StringBuilder str = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line=null;
            String key;
            String val;
            Map<String,String> map = new HashMap<>();
            while ( (line = reader.readLine()) != null){
                key = line.substring(0,line.indexOf("="));
                val = line.substring(line.indexOf("=")+1);
                map.put(key,val);
            }
            reader.close();
            return map.size()>0?map:null;
        } catch (IOException e) {
            LLog.error(e);
        }
        return null;
    }

    //指定位置存
    public static void writeByteToFilePoint(String path,byte[] content,int length,int point){
        RandomAccessFile raf = null;
        try {
            byte[] bytes =  new byte[length];
            System.arraycopy(content, 0, bytes, 0, content.length);
            bytes[content.length] = ENDFD[0];
            raf = new RandomAccessFile(path,"rw");
            raf.seek(point);
            raf.write(bytes,0,bytes.length); // 写进去的长度
            raf.close();
        } catch (Exception e) {
            LLog.error(e);
        }finally {
            try {
                if (raf!=null) raf.close();
            } catch (IOException e) {
                LLog.error(e);
            }
        }
    }
    /**
     * 获取结束位置
     */
    private static int getEndPoint(byte[] content) {
        int length = content.length;
        for (int i = 0; i < length; i++) {
            if(ENDFD[0] == content[i]) return i;
        }
        return length;
    }
    public static String readFilePointToByte(String path,int point,int length){
        RandomAccessFile raf = null;
        try {
            byte[] bytes = new byte[length];
            raf = new RandomAccessFile(path,"r");
            raf.seek(point);
            raf.read(bytes);
            raf.close();
            int endPoint = getEndPoint(bytes);
            byte[] source = new byte[endPoint];
            System.arraycopy(bytes, 0, source, 0, endPoint);
            return  new String(source);
        } catch (Exception e) {
            ;
        }finally {
            try {
                if (raf!=null) raf.close();
            } catch (IOException e) {
                LLog.error(e);
            }
        }
        return null;
    }

    public static String getFileText(String tmpFile) {
        return getFileText(tmpFile,true);
    }
    public static String getFileText(String tmpFile,boolean isDelete) {
        FileChannel inChannel = null;
        try {
            inChannel = new RandomAccessFile(tmpFile,"rw").getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) inChannel.size());
            buffer.clear();
            inChannel.read(buffer);
            buffer.flip();
           return new String(buffer.array(),"UTF-8");
        } catch (Exception e) {
            LLog.error(e);
        }finally {
            if (inChannel!=null){
                try {
                    inChannel.close();
                } catch (IOException e) {
                    LLog.error(e);
                }
            }
            if (isDelete) deleteFile(tmpFile);
        }
        return null;
    }

    public static boolean checkFile(String path) {
        File file = new File(path);
        if (file.exists()) return true;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            LLog.error(e);
        }
        return false;
    }

    public static boolean checkFileNotCreate(String path) {
        File file = new File(path);
        if (file.exists()&& file.isFile()) return true;
        return false;
    }

    public static boolean writeStringToFile(String content, String pathDir,String fileName,boolean isAppend) {

        if (!checkDir(pathDir)) return false;
        File file = new File(pathDir+SEPARATOR+fileName);
        if (!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                LLog.error(e);
                return false;
            }
        }
        FileChannel out = null;
        try {
            out = new FileOutputStream(file,isAppend).getChannel();
           out.write(Charset.forName("UTF-8").encode(content));
           return true;
        } catch (IOException e) {
        } finally {
            closeStream(null,out);
        }
        return false;
    }

    public static boolean checkFileLength(String path,long totalSize) {
        File file = new File(path);
        if (file.exists() && file.isFile()){
           return file.length() == totalSize;
        }else{
            return false;
        }
    }

    public static String getFilePath(File file) {
        if (file==null) return null;
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }




    public static String readFileText(String path,String charset){
        if (StringUtils.isEmpty(charset)) charset = "UTF-8";
        try(FileInputStream fis = new FileInputStream(path)) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[1024];
            int len;
            while ( (len = fis.read(bytes))>0 ){
                sb.append( new String (bytes,0,len,charset));
            }
            return sb.toString();
        } catch (Exception e) {
            LLog.error(e);
        }
        return null;
    }
    public static <T> T readFileJson2Object(String path,String charset,Class<T> classType){
        String json = readFileText(path,charset);

        if (json!=null){
            try {
                return new Gson().fromJson(json,classType);
            } catch (JsonSyntaxException e) {
                LLog.error(e);
            }
        }
        return null;
    }


    public static String byteLength2StringShow(long size) {
        //如果字节数少于1024，则直接以B为单位，否则先除于1024，后3位因太少无意义
        if (size < 1024) {
            return size + "B";
        } else {
            size = size / 1024;
        }
        //如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        //因为还没有到达要使用另一个单位的时候
        //接下去以此类推
        if (size < 1024) {
            return size + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            //因为如果以MB为单位的话，要保留最后1位小数，
            //因此，把此数乘以100之后再取余
            size = size * 100;
            return (size / 100) + "."
                    + (size % 100) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return (size / 100) + "."
                    + (size % 100) + "GB";
        }
    }

    public static void clearFiles(File dir, long invTime) {
        if (dir.exists()){
            File[] files = dir.listFiles();
            for (File file : files){
                if (file.isDirectory()){
                    clearFiles(file,invTime);
                }
                if (file.isFile()){
                    long lastTime = file.lastModified();
                    if (System.currentTimeMillis() - lastTime >  invTime){
                        file.delete();
                    }
                }
            }
        }
    }

    public static boolean checkDictPermission(File dict) {


        try {
            // 目录不存在尝试创建
            if (!dict.exists() && !dict.mkdir())  throw new IllegalArgumentException("没有目录创建权限");

            // 存在检测删除 , 不存在检测创建
            File checkFile = new File(dict,"check");
            if (checkFile.exists() && !checkFile.delete() || !checkFile.exists() && !checkFile.createNewFile())  throw new IllegalArgumentException("没有文件写入权限");

        } catch (Exception e) {
            LLog.print("目录 "+ dict+" 权限拒绝");
            return false;
        }
        return true;
    }

    public static void moveFileDict(File src, File desc){
        File[] files = src.listFiles();
        if (files == null) return;
//        LLog.print("移动目录 ("+src+") -> ("+ desc +") 开始");
        for (File f: files){
            if (f.isFile()){
                // 文件复制
                File temp = new File(desc,f.getName());
                if (temp.exists() && !temp.delete()) continue;

                boolean isMove = f.renameTo(temp);
                LLog.print("移动文件 ("+f+") -> ("+ temp +")  结果 = " + isMove);
            }
            if (f.isDirectory()){
                // 创建目录
                File temp = new File(desc,f.getName());
                if (!temp.exists() && !temp.mkdir()) continue;
                // 转移子目录
                moveFileDict(f,temp);
            }
        }
    }

    private static boolean copyFileUsingChannel(File source, File dest) {

        try (FileChannel sourceChannel = new FileInputStream(source).getChannel();
             FileChannel destChannel = new FileOutputStream(dest).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
            return true;
        }catch (Exception e){
            LLog.error(e);
        }
        return false;

    }

    public static void copyFileDict(File src, File desc){
        File[] files = src.listFiles();
        if (files == null) return;

        for (File f: files){
            if (f.isFile()){
                // 文件复制
                File temp = new File(desc,f.getName());
                if (temp.exists() && !temp.delete()) continue;

                boolean isMove = copyFileUsingChannel(f,temp);
                LLog.print("复制文件 ("+f+") -> ("+ temp +")  结果 = " + isMove);
            }
            if (f.isDirectory()){
                // 创建目录
                File temp = new File(desc,f.getName());
                if (!temp.exists() && !temp.mkdir()) continue;
                // 转移子目录
                copyFileDict(f,temp);
            }
        }
    }
}
