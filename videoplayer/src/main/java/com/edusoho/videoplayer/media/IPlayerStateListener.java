package com.edusoho.videoplayer.media;

/**
 * Created by suju on 17/2/21.
 */

public interface IPlayerStateListener {

    void onPrepare();

    void onFinish();

    void onPlaying();
}
