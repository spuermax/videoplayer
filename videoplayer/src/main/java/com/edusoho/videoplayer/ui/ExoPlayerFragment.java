package com.edusoho.videoplayer.ui;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ProgressBar;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.view.VideoControllerView;
import com.edusoho.videoplayer.view.VideoPlayerHeaderView;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

/**
 * Created by suju on 17/2/17.
 */

public class ExoPlayerFragment extends AbstractVideoPlayerFragment implements ExoPlayer.EventListener {

    private static final String                TAG             = "VideoPlayerFragment";
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    private VideoControllerView   mVideoControllerView;
    private VideoPlayerHeaderView mVideoPlayerHeaderView;
    private ProgressBar           mProgressView;

    private Handler             mainHandler;
    private SimpleExoPlayerView mExoPlayerView;
    private SimpleExoPlayer     player;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.exo_content_video_layout, null);
        ViewGroup parent = (ViewGroup) contentView.getParent();
        if (parent != null) {
            parent.removeView(contentView);
        }
        return contentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mExoPlayerView = (SimpleExoPlayerView) view.findViewById(R.id.player_view);
        //mExoPlayerView.setControllerVisibilityListener(this);
        mExoPlayerView.requestFocus();

        mVideoControllerView = (VideoControllerView) view.findViewById(R.id.vc_player_controller);
        mProgressView = (ProgressBar) view.findViewById(R.id.pb_player_progress);

        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mVideoControllerView.setControllerListener(mControllerListener);

        mainHandler = new Handler();
    }

    /**
     * 改变屏幕操作
     *
     * @param orientation
     */
    protected void changeScreenLayout(int orientation) {
        if (orientation == getResources().getConfiguration().orientation) {
            return;
        }
        View playView = getView();
        ViewParent viewParent = playView.getParent();
        if (viewParent == null) {
            return;
        }
        ViewGroup parent = (ViewGroup) viewParent;
        ViewGroup.LayoutParams lp = parent.getLayoutParams();
        lp.height = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                ViewGroup.LayoutParams.MATCH_PARENT : getContext().getResources().getDimensionPixelOffset(R.dimen.video_height);
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        parent.setLayoutParams(lp);

        int requestedOrientarion = orientation == Configuration.ORIENTATION_LANDSCAPE ?
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        getActivity().setRequestedOrientation(requestedOrientarion);
    }

    /*
        改变头部状态
     */
    protected void changeHeaderViewStatus(boolean isShow) {
        if (mVideoPlayerHeaderView == null) {
            return;
        }
        int visibility = isShow ? View.VISIBLE : View.INVISIBLE;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            visibility = View.INVISIBLE;
        }
        mVideoPlayerHeaderView.setVisibility(visibility);
    }

    protected VideoControllerView.ControllerListener mControllerListener = new VideoControllerView.ControllerListener() {
        @Override
        public void onSeek(int position) {
            player.seekTo(position);
        }

        @Override
        public void onChangeScreen(int orientation) {
            changeHeaderViewStatus(orientation == Configuration.ORIENTATION_LANDSCAPE);
            changeScreenLayout(orientation);
        }

        @Override
        public void onPlayStatusChange(boolean isPlay) {
            Log.d(TAG, "onPlayStatusChange:" + isPlay);
            if (isPlay) {
                play();
            } else {
                pause();
            }
        }

        @Override
        public void onChangeRate(float rate) {
            //not support rate
        }

        @Override
        public void onChangePlaySource(String url) {
            //savePosition(mService.getTime());
            changeMediaSource(url);
        }

        @Override
        public void onChangeOverlay(boolean isShow) {
            changeHeaderViewStatus(isShow);
        }
    };

    @Override
    public void play() {

    }

    @Override
    public void playVideo(String videoUri) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void onStart() {
        super.onStart();
        startPlayback();
    }

    private void startPlayback() {
        if (player == null) {
            player = initializePlayer();
        }

        mExoPlayerView.setPlayer(player);
        player.addListener(this);
        MediaSource videoSource = new HlsMediaSource(
                Uri.parse("http://default.andy.dev.qiqiuyun.cn:8071/video-player/examples/server/playlist.m3u8"),
                getDataSourceFactory(), mainHandler, null);
        player.prepare(videoSource);
    }

    private SimpleExoPlayer initializePlayer() {
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, new DefaultLoadControl());

        player.setPlayWhenReady(true);
        player.addListener(this);

        return player;
    }

    private DefaultDataSourceFactory getDataSourceFactory() {
        return new DefaultDataSourceFactory(getContext(), "android-player");
    }

    @Override
    protected void changeMediaSource(String url) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.d(TAG, "isLoading:" + isLoading);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "playWhenReady:" + playWhenReady);
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d(TAG, "error:" + error);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
}
