package com.edusoho.videoplayer.cache;

import android.content.Context;

/**
 * Created by suju on 16/12/29.
 */
public class CacheFactory {

    private CacheServer mCacheServer;

    private static CacheFactory ourInstance = new CacheFactory();

    public static CacheFactory getInstance() {
        return ourInstance;
    }

    private CacheFactory() {
    }

    public void startServer(Context context) {
        mCacheServer = new CacheServer(context);
        mCacheServer.start();
    }

    public void destory() {
        if (mCacheServer != null) {
            mCacheServer.close();
        }
    }
}
