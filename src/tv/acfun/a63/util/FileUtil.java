package tv.acfun.a63.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Locale;

import tv.acfun.a63.AcApp;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

public class FileUtil {
    public static Uri getLocalFileUri(File file){
        return Uri.fromFile(file);
    }
    public static long getFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                size = size + getFolderSize(files[i]);
            else
                size = size + files[i].length();
        }
        return size;
    }
    /**
     * @see {@link FileUtil#getFolderSize(File)}
     * @param folder
     * @return
     */
    public static String getFormatFolderSize(File folder){
        return formatFileSize(getFolderSize(folder));
    }
    
    /*** 格式化文件大小(xxx.xx B/KB/MB/GB) */
    public static String formatFileSize(long size) {
        if(size <=0) return "0B";
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (size < _1KB)
            fileSizeString = df.format((double) size) + "B";
        else if (size < _1MB)
            fileSizeString = df.format((double) size / _1KB) + "KB";
        else if (size < _1GB)
            fileSizeString = df.format((double) size / _1MB) + "MB";
        else
            fileSizeString = df.format((double) size / _1GB) + "GB";

        return fileSizeString;
    }

    /**
     * 显示SD卡剩余空间
     * 
     * @return SD卡不存在则返回null
     */
    public static String showFileAvailable() {
        long availableSize = getExternalAvailable();
        if (availableSize > 0)
            return formatFileSize(availableSize);
        return null;
    }

    /**
     * 获得SD卡剩余空间
     * 
     * @return SD卡未挂载则返回-1
     */
    public static long getExternalAvailable() {
        if (AcApp.isExternalStorageAvailable()) {
            StatFs sf = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();
            return availCount * blockSize;
        } else
            return -1;

    }
    /**
     *   "/" ~ "?"之间的".xxx"
     * @param url
     * @return
     */
    public static String getUrlExt(String url){
        
        if (!TextUtils.isEmpty(url)) {
//            int start = url.lastIndexOf('.');
            int start = url.lastIndexOf('/');
            int end = url.lastIndexOf('?');
            end = end <= start ? url.length() : end;
            String ext = "";
            if (start > 0 && start < url.length() - 1) {
                try{
                ext = url.substring(start, end).toLowerCase();
                
                return ext.substring(ext.lastIndexOf('.'));
                }catch (StringIndexOutOfBoundsException e) {
                   Log.e("Util", "when get url ext : "+url,e);
                }
            }
            
        }
        return "flv";
    }
    
    public static String guessVideoMimetype(String ext){
        String mimetype = null;
        if(".flv".equals(ext)){
            mimetype = "video/x-flv";
        }else if(".f4v".equals(ext)){
            mimetype = "video/x-f4v";
        }else if(".mp4".equals(ext)){
            mimetype = "video/mp4";
        }else mimetype = "video/*";
/*        else if(".hlv".equals(ext)){
            mimetype = "video/x-f4v"; // XXX: mimetype of hlv???
        }*/
        return mimetype;
    }
    public static final int _1KB = 1024;
    public static final int _1MB = _1KB * _1KB;
    public static final int _1GB = _1KB * _1MB;
    /**
     * @param type the http header, content-type
     * @return
     */
    public static String getMimeType(String type) {
        if (type == null) {
            return null;
        }

        type = type.trim().toLowerCase(Locale.US);

        final int semicolonIndex = type.indexOf(';');
        if (semicolonIndex != -1) {
            type = type.substring(0, semicolonIndex);
        }
        return type;
    }
    public static String getHashName(String url){
        return String.valueOf(url.hashCode()) + getUrlExt(url);
    }
    public static String getName(String url,boolean raw) {
        if (!TextUtils.isEmpty(url)) {
            if (raw) {
                int start = url.lastIndexOf('/');
                int end = url.lastIndexOf('?');
                end = end <= start ? url.length() : end;
                String name = "";
                if (start > 0 && start < url.length() - 1) {
                    try {
                        name = url.substring(start, end).toLowerCase();
                        return name;
                    } catch (StringIndexOutOfBoundsException e) {
                        Log.e("Util", "when get url name : " + url, e);
                    }
                }
            }
            return getHashName(url);
            
        }
        return "cache";
    }
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4*_1KB];
        int len = -1;
        while((len = in.read(buf))!=-1){
            out.write(buf,0,len);
        }
        buf = null;
    }
    
    public static File generateCacheFile(String type, String fileUri){
        int hashCode = fileUri.hashCode();
        String folderName = String.format("%x", hashCode & 0xf);
        String fileName = String.format("%x", hashCode >>> 4)+getUrlExt(fileUri);
        File cache =new File(AcApp.getExternalCacheDir(type+"/"+folderName),fileName);
        return cache;
        
    }
    public static File generateImageCacheFile(String imgUri){
        return generateCacheFile(AcApp.IMAGE, imgUri);
    }
    
    public static boolean deleteFiles(File file){
        if(file.isFile()) return file.delete();
        else{
            String[] progArray = new String[]{"rm","-r",file.getAbsolutePath()};
            try {
                Runtime.getRuntime().exec(progArray);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    public static final int MSG_DELETE_OK  = 200;
    
    public static final int MSG_DELETE_FAILED  = 300;
    
    public static void deleteFilesAsync(final File file, final Handler handler) {
        new Thread() {
            public void run() {
                
                boolean ok = deleteFiles(file);
                if(handler != null){
                    if(ok)
                        handler.sendEmptyMessage(MSG_DELETE_OK);
                    else
                        handler.sendEmptyMessage(MSG_DELETE_FAILED);
                }
            }
        }.start();
    }
}
