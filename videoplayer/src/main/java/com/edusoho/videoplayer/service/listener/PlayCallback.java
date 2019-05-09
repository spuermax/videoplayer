package com.edusoho.videoplayer.service.listener;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

/**
 * Created by suju on 16/12/18.
 */

public interface PlayCallback {
    void onMediaEvent(Media.Event event);
    void onMediaPlayerEvent(MediaPlayer.Event event);
}
