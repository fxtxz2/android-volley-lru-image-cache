package com.zyl.androidlruimagecache;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;
import com.jakewharton.disklrucache.DiskLruCache;
import com.zyl.filemd5utils.MD5Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 用于volley的图片缓存库:二级(内存和磁盘缓存)Lru图片缓存
 * @author zyl
 *
 */
public class LruImageCache extends LruCache<String, Bitmap> implements ImageCache {

	/**
	 * 10MB
	 */
	private static int DISK_MAX_SIZE = 10 * 1024 * 1024;
	/**
	 * 磁盘LRU缓存对象
	 */
	private static DiskLruCache diskLruCache;

	/**
	 * 应用当前上下文
	 */
	private Context context;

	public LruImageCache(int maxSize, String diskCacheFodler, int diskCacheSize, Context context) {
		super(maxSize);
		DISK_MAX_SIZE = diskCacheSize;
		try {
			diskLruCache = DiskLruCache.open(
					getDiskCacheDir(context, diskCacheFodler),
					getAppVersion(context), 1, DISK_MAX_SIZE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
	        return value.getByteCount();
	    }
		return value.getRowBytes() * value.getHeight();
	}
	
	@Override
	public Bitmap getBitmap(String url) {
		String key=generateKey(url);  
		// 从内存缓存中检查数据
        Bitmap bmp = get(url);  
        if (bmp == null) {  
            bmp = getBitmapFromDiskLruCache(key);  
            //从磁盘读出后，放入内存  
            if(bmp!=null)  
            {  
                put(url,bmp);  
            }  
        }  
        return bmp;  
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		String key=generateKey(url);  
		// 写入内存缓存对象
		put(url, bitmap);  
        putBitmapToDiskLruCache(key,bitmap);  
	}
	
	/**
	 * 从磁盘缓存中获取bitmap数据
	 * @return bitmap
	 */
	private Bitmap getBitmapFromDiskLruCache(String key) {  
		InputStream inputStream = null;
        try {  
            DiskLruCache.Snapshot snapshot=diskLruCache.get(key);  
            if(snapshot!=null)  
            {  
                inputStream = snapshot.getInputStream(0);  
                if (inputStream != null) {  
                    Bitmap bmp = BitmapFactory.decodeStream(inputStream);  
                    inputStream.close();  
                    return bmp;  
                }  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally{
        	if (inputStream != null) {
        		try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        } 
        return null;  
    }
	
	/**
	 * 写入本地硬盘缓存对象
	 */
	private void putBitmapToDiskLruCache(String key, Bitmap bitmap) {  
		OutputStream outputStream = null;
        try {  
            DiskLruCache.Editor editor = diskLruCache.edit(key);  
            if(editor!=null)  
            {  
                outputStream = editor.newOutputStream(0);  
                bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);  
                editor.commit();  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {
        	if (outputStream != null) {
        		try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        }
    }

	/** 
     * 因为DiskLruCache对key有限制，只能是[a-z0-9_-]{1,64},所以用md5生成key 
     */
    private String generateKey(String url) {  
        return MD5Utils.hashKeyForDisk(url);
    }  
    
	/**
	 * 该方法会判断当前sd卡是否存在，然后选择缓存地址
	 * 
	 */
	public static File getDiskCacheDir(Context context, String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			if (context.getExternalCacheDir() != null){
				cachePath = context.getExternalCacheDir().getPath();
			} else {
				cachePath = context.getCacheDir().getPath();
			}
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + uniqueName);
	} 
	
	/**
	 * 获得应用version号码
	 * 
	 */
	public static int getAppVersion(Context context) {
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return 1;
	}
}
