package com.edusoho.videoplayer.service;

import android.os.IBinder;

/**
 * Created by suju on 16/12/18.
 */

public interface PlayServiceBinder extends IBinder {

    IPlayerServcie getService();
}
