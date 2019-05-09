package com.edusoho.videoplayer.media;

import android.net.Uri;
import android.view.View;

import com.edusoho.videoplayer.view.VideoControllerView;

import java.util.List;

/**
 * Created by suju on 17/2/21.
 */

public interface IVideoPlayer {

    void onStart();

    void onStop();

    void setSeekPosition(long seekPosition);

    void addPlayerStateListener(IPlayerStateListener listener);

    void addVideoController(VideoControllerView videoControllerView);

    void play();

    void pause();

    void setMediaSource(String url);

    void setSubtitlesUrls(List<Uri> subtitlesUris);

    void start();

    View getView();

    long getPosition();

    long getVideoLength();

    void setDigestKey(String digestKey);

    void addLogListener(ILogoutListener logoutListener);
}
