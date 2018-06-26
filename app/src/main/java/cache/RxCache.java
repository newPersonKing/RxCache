package cache;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

/* * RxCache rxCache = new RxCache.Builder(this)<br>*/
public class RxCache {

    private final Context context;
    private final CacheCore cacheCore;                                  //缓存的核心管理类
    private final String cacheKey;                                      //缓存的key
    private final long cacheTime;                                       //缓存的时间 单位:秒
    private final IDiskConverter diskConverter;                         //缓存的转换器
    private final File diskDir;                                         //缓存的磁盘目录，默认是缓存目录
    private final int appVersion;                                       //缓存的版本
    private final long diskMaxSize;                                     //缓存的磁盘大小


    public RxCache() {
        this(new Builder());
    }

    private RxCache(Builder builder) {
        this.context = builder.context;
        this.cacheKey = builder.cachekey;
        this.cacheTime = builder.cacheTime;
        this.diskDir = builder.diskDir;
        this.appVersion = builder.appVersion;
        this.diskMaxSize = builder.diskMaxSize;
        this.diskConverter = builder.diskConverter;
        cacheCore = new CacheCore(new LruDiskCache(diskConverter, diskDir, appVersion, diskMaxSize));
    }


    public static final class Builder{
        /*最小缓存*/
        private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
        /*最大缓存*/
        private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
        public static final long CACHE_NEVER_EXPIRE = -1;//永久不过期

        private int appVersion;
        private long diskMaxSize;
        /*缓存路径*/
        private File diskDir;

        private Context context;
        /*缓存key*/
        private String cachekey;
        /*缓存时间*/
        private long cacheTime;
        /*数据读写*/
        private IDiskConverter diskConverter;

        public Builder(){
            diskConverter=new SerializableDiskConverter();
            cacheTime=CACHE_NEVER_EXPIRE;
            appVersion=1;
        }

        public Builder(RxCache rxCache){

        }

        public Builder init(Context context){
            this.context=context;
            return this;
        }

        /**
         * 不设置，默认为1
         */
        public Builder appVersion(int appVersion) {
            this.appVersion = appVersion;
            return this;
        }

        /**
         * 默认为缓存路径
         *
         * @param directory
         * @return
         */
        public Builder diskDir(File directory) {
            this.diskDir = directory;
            return this;
        }

        public Builder diskConverter(IDiskConverter converter) {
            this.diskConverter = converter;
            return this;
        }

        /**
         * 不设置， 默为认50MB
         */
        public Builder diskMax(long maxSize) {
            this.diskMaxSize = maxSize;
            return this;
        }

        public Builder cachekey(String cachekey) {
            this.cachekey = cachekey;
            return this;
        }

        public Builder cacheTime(long cacheTime) {
            this.cacheTime = cacheTime;
            return this;
        }

        public RxCache build(){
            if (this.diskDir==null&&this.context!=null){
                this.diskDir=getDiskCacheDir(context,"data-cache");
            }

            if (!this.diskDir.exists()){
                this.diskDir.mkdirs();
            }

            if (this.diskConverter==null){
                diskConverter=new SerializableDiskConverter();
            }

            if (diskMaxSize<0){
                diskMaxSize=calculateDiskCacheSize(diskDir);
            }

            cacheTime=Math.max(CACHE_NEVER_EXPIRE,cacheTime);

            appVersion=Math.max(1,appVersion);

            return new RxCache(this);
        }

        /*计算可用的最大缓存*/
        private static long calculateDiskCacheSize(File dir) {
            long size = 0;
            try {
                StatFs statFs = new StatFs(dir.getAbsolutePath());
                long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
                size = available / 50;
            } catch (IllegalArgumentException ignored) {
            }
            return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
        }


        /**
         * 应用程序缓存原理：
         * 1.当SD卡存在或者SD卡不可被移除的时候，就调用getExternalCacheDir()方法来获取缓存路径，否则就调用getCacheDir()方法来获取缓存路径<br>
         * 2.前者是/sdcard/Android/data/<application package>/cache 这个路径<br>
         * 3.后者获取到的是 /data/data/<application package>/cache 这个路径<br>
         *
         * @param uniqueName 缓存目录
         */
        private File getDiskCacheDir(Context context, String uniqueName) {
            File cacheDir;
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                    || !Environment.isExternalStorageRemovable()) {
                cacheDir = context.getExternalCacheDir();
            } else {
                cacheDir = context.getCacheDir();
            }
            if (cacheDir == null) {// if cacheDir is null throws NullPointerException
                cacheDir = context.getCacheDir();
            }
            return new File(cacheDir.getPath() + File.separator + uniqueName);
        }

    }



}
