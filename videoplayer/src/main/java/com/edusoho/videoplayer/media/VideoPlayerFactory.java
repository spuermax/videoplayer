package com.edusoho.videoplayer.media;

import android.content.Context;

import com.edusoho.videoplayer.view.VlcVideoView;
import com.edusoho.videoplayer.util.VLCOptions;
import com.edusoho.videoplayer.view.EduExoPlayerView;

/**
 * Created by suju on 17/2/21.
 */
public class VideoPlayerFactory {

    public static final int VLC_PLAYER = 1;
    public static final int EXO_PLAYER = 2;

    private static VideoPlayerFactory ourInstance = new VideoPlayerFactory();

    public static VideoPlayerFactory getInstance() {
        return ourInstance;
    }

    private VideoPlayerFactory() {
    }

    public IVideoPlayer createPlayer(Context context, int mediaCoder) {
        return createPlayer(context, mediaCoder, EXO_PLAYER);
    }

    public IVideoPlayer createPlayer(Context context, int mediaCoder, int playType) {
        if (playType == VLC_PLAYER) {
            return new VlcVideoView(context);
        }
        EduExoPlayerView playerView = new EduExoPlayerView(context);
        playerView.setMediaCoder(mediaCoder);
        return playerView;
    }
}
