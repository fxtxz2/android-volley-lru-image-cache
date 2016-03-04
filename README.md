# android-volley-lru-image-cache
用于volley的图片缓存库:二级(内存和磁盘缓存)Lru图片缓存

# 在volley中的使用
```Java
private RequestQueue mRequestQueue;// com.android.volley.RequestQueue
private ImageLoader mImageLoader;// com.android.volley.toolbox.ImageLoader
private static Context mCtx;//当前上下文

public void init(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
        // 配置volley队列Image Lru Cache
        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache() {
            private final LruImageCache cache = new LruImageCache(MEM_CACHE_SIZE,"images",30*1024*1024, mCtx);

            @Override
            public Bitmap getBitmap(String url) {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap) {
                cache.put(url, bitmap);
            }
        });
    }
```

# Gradle引入
```Gradle
compile 'com.zyl.androidlruimagecache:androidvolleylruimagecache:0.0.1'// volley image lru cache
```
