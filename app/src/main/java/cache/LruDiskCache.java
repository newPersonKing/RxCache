package cache;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class LruDiskCache extends BaseCache {
    /*进行读写操作*/
    private IDiskConverter mDiskConverter;
    private DiskLruCache mDiskLruCache;


    public LruDiskCache(IDiskConverter diskConverter, File diskDir, int appVersion, long diskMaxSize) {

        try {
            mDiskLruCache = DiskLruCache.open(diskDir, appVersion, 1, diskMaxSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*判断是否存在key*/
    @Override
    protected boolean doContainsKey(String key) {
        if (mDiskLruCache == null) {
            return false;
        }
        try {
            return mDiskLruCache.get(key) != null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*判断是否过期*/
    @Override
    protected boolean isExpiry(String key, long existTime) {

        if (mDiskLruCache == null) {
            return false;
        }

        if (existTime > -1) {//-1表示永久性存储 不用进行过期校验
            //为什么这么写，请了解DiskLruCache，看它的源码
            File file = new File(mDiskLruCache.getDirectory(), key + "." + 0);
            if (isCacheDataFailure(file, existTime)) {//没有获取到缓存,或者缓存已经过期!
                return true;
            }
        }


        return false;
    }

    /*加载缓存*/
    @Override
    protected <T> T doLoad(Type type, String key) {
        if (mDiskLruCache==null){
            return null;
        }
        try {
            DiskLruCache.Editor edit= mDiskLruCache.edit(key);

            if (edit == null) {
                return null;
            }
            InputStream stream=edit.newInputStream(0);
            T value;
            if (stream!=null){
                value=mDiskConverter.load(stream,type);
                stream.close();
                edit.commit();
                return value;
            }
            edit.abort();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /*保存缓存*/
    @Override
    protected <T> boolean doSave(String key, T value) {
        if (mDiskLruCache==null){
            return false;
        }
        try {
            DiskLruCache.Editor edit = mDiskLruCache.edit(key);
            if (edit==null){
                return false;
            }

            OutputStream oos=edit.newOutputStream(0);
            if (oos!=null){
                boolean result=mDiskConverter.writer(oos,value);
                oos.close();
                return result;
            }
            edit.abort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*删除缓存*/
    @Override
    protected boolean doRemove(String key) {

        if (mDiskLruCache == null) {
            return false;
        }
        try {
            return mDiskLruCache.remove(key);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /*清空所有的缓存*/
    @Override
    protected boolean doClear() {
        boolean statu = false;
        try {
            mDiskLruCache.delete();
            statu = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return statu;
    }


    /**
     * 判断缓存是否已经失效
     * time 自己设置的文件存在的时间
     */
    private boolean isCacheDataFailure(File dataFile, long time) {
        if (!dataFile.exists()) {
            return false;
        }
        long existTime = System.currentTimeMillis() - dataFile.lastModified();
        return existTime > time*1000;
    }
}
