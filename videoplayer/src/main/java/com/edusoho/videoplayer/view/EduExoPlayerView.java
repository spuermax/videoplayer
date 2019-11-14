package com.edusoho.videoplayer.view;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.edusoho.videoplayer.BuildConfig;
import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.broadcast.MessageBroadcastReceiver;
import com.edusoho.videoplayer.error.HlsSourceIOException;
import com.edusoho.videoplayer.helper.ControllerViewTouchHelper;
import com.edusoho.videoplayer.media.EduExoPlayer;
import com.edusoho.videoplayer.media.ILogoutListener;
import com.edusoho.videoplayer.media.IPlayerStateListener;
import com.edusoho.videoplayer.media.IVideoPlayer;
import com.edusoho.videoplayer.media.encrypt.EncrpytHttpDataSourceFactory;
import com.edusoho.videoplayer.media.encrypt.EncryptDataSourceFactory;
import com.edusoho.videoplayer.util.ControllerOptions;
import com.edusoho.videoplayer.util.VLCOptions;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.util.List;

/**
 * Displays a video stream.
 */
@TargetApi(16)
public class EduExoPlayerView extends FrameLayout implements IVideoPlayer {

    static final String TAG = "EduExoPlayerView";

    private final View                   surfaceView;
    private final AspectRatioFrameLayout layout;
    private final ComponentListener      componentListener;

    private EduExoPlayer        player;
    private SubtitleView        subtitleView;
    private ProgressBar         mProgressView;
    private VideoControllerView mVideoControllerView;
    private Handler             mainHandler;
    private String              mMediaUrl;
    private List<Uri>           mSubtitlesUris;
    private String              mDigestKey;
    private boolean             isAttachedToWindow;
    private int                 mMediaCoder;
    private int                 mCurrentPlayState;
    private int                 mSurfaceState;

    private       long mSeekPosition = -1L;
    private final int  STATE_CREATE  = 1;
    private final int  STATE_DESTORY = 2;
    private final int  STATE_NOMAL   = 3;

    private ILogoutListener      mLogoutListener;
    private IPlayerStateListener mIPlayerStateListener;
    private MediaSource          mCurrentSubtitleMediaSource;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateMediaProgress();
        }
    };

    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    public EduExoPlayerView(Context context) {
        this(context, null);
    }

    public EduExoPlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EduExoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        boolean useTextureView = false;
        LayoutInflater.from(context).inflate(R.layout.view_exoplayer_layout, this);
        componentListener = new ComponentListener();
        layout = findViewById(R.id.arf_exo_content);

        View view = useTextureView ? new TextureView(context) : new SurfaceView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
        surfaceView = view;
        layout.addView(surfaceView, 0);

        subtitleView = findViewById(R.id.exo_subtitles);
        subtitleView.setUserDefaultStyle();
        subtitleView.setUserDefaultTextSize();

        onCreateView();
    }

    public void setMediaCoder(int mMediaCoder) {
        this.mMediaCoder = mMediaCoder;
    }

    private void onCreateView() {
        mProgressView = findViewById(R.id.pb_player_progress);
        mainHandler = new Handler();
    }

    private void startPlayback() {
        if (player == null) {
            player = initializePlayer();
            setPlayer(player);
        }

        MediaSource videoSource = buildMediaSource(Uri.parse(mMediaUrl));
        if (mSubtitlesUris != null && mSubtitlesUris.size() > 0) {
            Format subtitleFormat = Format.createTextSampleFormat(
                    null, // An identifier for the track. May be null.
                    MimeTypes.APPLICATION_SUBRIP, // The mime type. Must be set correctly.
                    C.SELECTION_FLAG_DEFAULT, // Selection flags for the track.
                    "");
            MediaSource[] subtitleSources = new MediaSource[2];
            subtitleSources[0] = videoSource;
            int i = 1;
//            for (String subtitleUrl : mSubtitlesUris) {
//                MediaSource subtitleSource = new SingleSampleMediaSource.Factory(getDataSourceFactory())
//                        .createMediaSource(Uri.parse(subtitleUrl), subtitleFormat, C.TIME_UNSET);
//                subtitleSources[i++] = subtitleSource;
//            }
            mCurrentSubtitleMediaSource = new SingleSampleMediaSource.Factory(getDataSourceFactory())
                    .createMediaSource(mSubtitlesUris.get(0), subtitleFormat, C.TIME_UNSET);
            subtitleSources[i] = mCurrentSubtitleMediaSource;
            MergingMediaSource mergingMediaSource = new MergingMediaSource(videoSource, subtitleSources[i]);
            player.prepare(mergingMediaSource);
        } else {
            player.prepare(videoSource);
        }
    }

    private MediaSource buildMediaSource(Uri uri) {
        DataSource.Factory mediaDataSourceFactory = buildDataSourceFactory(BANDWIDTH_METER);
        int type = Util.inferContentType(uri.getLastPathSegment());
        switch (type) {
            case C.TYPE_SS:
                return new SsMediaSource(uri, getDataSourceFactory(),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
            case C.TYPE_DASH:
                return new DashMediaSource(uri, getDataSourceFactory(),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, null);
            case C.TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, null);
            case C.TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, null);
            default: {
                if (mLogoutListener != null) {
                    mLogoutListener.onLog(TAG, "Unsupported type: " + type);
                }
                throw new IllegalStateException("Unsupported type: " + type);
            }
        }
    }

    private String getUserAgent() {
        return Util.getUserAgent(getContext(), "kuozhi-Android-exo-player-" + BuildConfig.VERSION_NAME);
    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new EncryptDataSourceFactory(getContext(), mDigestKey, bandwidthMeter,
                new EncrpytHttpDataSourceFactory(getUserAgent(), bandwidthMeter));
    }

    private DefaultDataSourceFactory getDataSourceFactory() {
        return new DefaultDataSourceFactory(getContext(), getUserAgent());
    }

    private EduExoPlayer initializePlayer() {
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        EduExoPlayer player = new EduExoPlayer(getContext(), trackSelector, new DefaultLoadControl(), null);
        player.setPlayWhenReady(true);
        return player;
    }

    protected VideoControllerView.ControllerListener getDefaultControllerListener() {
        return new VideoControllerView.ControllerListener() {
            @Override
            public void onSeek(int position) {
                if (player != null) {
                    player.seekTo(position);
                }
            }

            @Override
            public void onChangeScreen(int orientation) {
            }

            @Override
            public void onPlayStatusChange(boolean isPlay) {
                Log.d(TAG, "onPlayStatusChange:" + isPlay);
                if (isPlay) {
                    if (mCurrentPlayState == ExoPlayer.STATE_ENDED) {
                        startPlayback();
                        return;
                    }
                    play();
                } else {
                    pause();
                }
            }

            @Override
            public void onChangeRate(float rate) {
                if (player != null) {
                    player.setRate(rate);
                }
            }

            @Override
            public void onChangePlaySource(String url) {
                mSeekPosition = player.getCurrentPosition();
                player.stop();
                if (mCurrentSubtitleMediaSource == null) {
                    player.prepare(buildMediaSource(Uri.parse(url)));
                } else {
                    MergingMediaSource mergingMediaSource = new MergingMediaSource(buildMediaSource(Uri.parse(url)), mCurrentSubtitleMediaSource);
                    player.prepare(mergingMediaSource);
                }
            }

            @Override
            public void onChangeOverlay(boolean isShow) {
            }

            @Override
            public void onPosition(int position, int duration) {

            }
        };
    }

    public void setPlayer(EduExoPlayer player) {
        if (this.player != null) {
            this.player.removeListener(componentListener);
            this.player.removeTextOutput(componentListener);
            this.player.removeVideoListener(componentListener);
            if (surfaceView instanceof TextureView) {
                this.player.clearVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                this.player.clearVideoSurfaceView((SurfaceView) surfaceView);
            }
        }
        this.player = player;
        if (subtitleView != null) {
            subtitleView.setCues(null);
        }
        if (player != null) {
            if (surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
                ((SurfaceView) surfaceView).getHolder().addCallback(new SurfaceHolder.Callback() {
                    @Override
                    public void surfaceCreated(SurfaceHolder holder) {
                        mSurfaceState = STATE_CREATE;
                    }

                    @Override
                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    @Override
                    public void surfaceDestroyed(SurfaceHolder holder) {
                        mSurfaceState = STATE_DESTORY;
                    }
                });
            }
            player.addVideoListener(componentListener);
            player.addTextOutput(componentListener);
            player.addListener(componentListener);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVideoControllerView != null && ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (mVideoControllerView.getVisibility() == VISIBLE) {
                mVideoControllerView.setVisibility(INVISIBLE);
            } else {
                mVideoControllerView.setVisibility(VISIBLE);
            }
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (mVideoControllerView == null) {
            return false;
        }
        mVideoControllerView.setVisibility(VISIBLE);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mVideoControllerView != null ? mVideoControllerView.dispatchKeyEvent(event) : super.dispatchKeyEvent(event);
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    private final class ComponentListener implements SimpleExoPlayer.VideoListener,
            TextRenderer.Output, ExoPlayer.EventListener {

        // TextRenderer.Output implementation
        @Override
        public void onCues(List<Cue> cues) {
            if (subtitleView != null) {
                subtitleView.onCues(cues);
            }
        }

        @Override
        public void onRenderedFirstFrame() {
        }

        // SimpleExoPlayer.VideoListener implementation
        @Override
        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                       float pixelWidthHeightRatio) {
            ControllerViewTouchHelper touchHelper = new ControllerViewTouchHelper((Activity) getContext());
            touchHelper.updateVideoSize(width, height);
            mVideoControllerView.setControllerViewTouchHelper(touchHelper);
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        }

        // ExoPlayer.EventListener implementation
        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            Log.d(TAG, "playbackState:" + playbackState);
            mCurrentPlayState = playbackState;
            if (mIPlayerStateListener != null && playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mIPlayerStateListener.onFinish();
            }
            if (mVideoControllerView != null && playbackState == ExoPlayer.STATE_ENDED) {
                mVideoControllerView.setVisibility(VISIBLE);
            }
            if (mIPlayerStateListener != null && mVideoControllerView != null && playbackState == ExoPlayer.STATE_READY) {
                mIPlayerStateListener.onPlaying();
                validToSeek(mSeekPosition);
            }
            mVideoControllerView.updatePlayStatus(playWhenReady);
            mProgressView.setVisibility(playbackState == ExoPlayer.STATE_BUFFERING ? VISIBLE : INVISIBLE);
            if (playWhenReady) {
                updateMediaProgress();
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                pause();
                releasePlayer();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            Throwable ioException = e.getCause();
            if (ioException != null) {
                if (ioException instanceof HlsSourceIOException) {
                    DataSpec dataSpec = ((HlsSourceIOException) ioException).getDataSpec();
                    String message = dataSpec != null ? dataSpec.uri.getPath() : "";
                    getContext().sendBroadcast(MessageBroadcastReceiver.getIntent("VideoFileNotFound", message));
                } else {
                    StackTraceElement[] stackTraceElements = ioException.getStackTrace();
                    if (stackTraceElements != null) {
                        for (StackTraceElement element : stackTraceElements) {
                            if ("android.media.MediaCodec".equals(element.getClassName())) {
                                getContext().sendBroadcast(MessageBroadcastReceiver.getIntent("MediaCodecError", ""));
                                break;
                            }
                        }
                    }
                }

            }
            if (mLogoutListener != null) {
                mLogoutListener.onLog("onPlayerError", e);
            }
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
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
    }

    private synchronized void validToSeek(long seekPosition) {
        if (mSurfaceState != STATE_CREATE) {
            return;
        }

        if (mSeekPosition == -1) {
            return;
        }
        Log.d(TAG, "validToSeek: " + seekPosition);
        player.seekTo(seekPosition);
        mSurfaceState = STATE_NOMAL;
    }

    private void updateMediaProgress() {
        if (getVisibility() != VISIBLE || !isAttachedToWindow
                || player == null
                || !player.getPlayWhenReady()) {
            return;
        }
        mVideoControllerView.updateMediaProgress((int) player.getCurrentPosition(), (int) player.getDuration());
        removeCallbacks(updateProgressAction);
        // Schedule an update if necessary.
        int playbackState = player == null ? ExoPlayer.STATE_IDLE : player.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (player.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (player.getCurrentPosition() % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressAction, delayMs);
        }
    }

    //IVideoPlayer
    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        if (player != null && player.getPlaybackState() == ExoPlayer.STATE_READY) {
            player.setPlayWhenReady(true);
            mSeekPosition = mSeekPosition - 2000;
            return;
        }
        if (!TextUtils.isEmpty(mMediaUrl)) {
            startPlayback();
        }
    }

    @Override
    public void onStop() {
        releasePlayer();
    }

    @Override
    public void setSeekPosition(long seekPosition) {
        if (player == null) {
            return;
        }
        Log.d(TAG, "seekPosition:" + seekPosition);
        if (seekPosition > 0) {
            player.seekTo(seekPosition);
        }
    }

    @Override
    public void addPlayerStateListener(IPlayerStateListener listener) {
        this.mIPlayerStateListener = listener;
        if (!TextUtils.isEmpty(mMediaUrl)) {
            mIPlayerStateListener.onPrepare();
        }
    }

    @Override
    public void addVideoController(VideoControllerView videoControllerView) {
        mVideoControllerView = videoControllerView;
        ControllerOptions options = new ControllerOptions.Builder()
                .addOption(ControllerOptions.RATE, mMediaCoder == VLCOptions.SUPPORT_RATE)
                .addOption(ControllerOptions.SCREEN, true)
                .build();
        mVideoControllerView.setControllerOptions(options);
        mVideoControllerView.setControllerListener(getDefaultControllerListener());
    }

    @Override
    public void play() {
        if (player == null) {
            return;
        }
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (player == null) {
            return;
        }
        mSeekPosition = player.getCurrentPosition();
        player.setPlayWhenReady(false);
    }

    @Override
    public void setMediaSource(String url) {
        mMediaUrl = url;
    }

    @Override
    public void setSubtitlesUrls(List<Uri> subtitlesUris) {
        this.mSubtitlesUris = subtitlesUris;
    }

    @Override
    public void start() {
        startPlayback();
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public long getPosition() {
        return player == null ? 0 : player.getCurrentPosition();
    }

    @Override
    public long getVideoLength() {
        return player == null ? 0 : player.getDuration();
    }

    @Override
    public void setDigestKey(String digestKey) {
        this.mDigestKey = digestKey;
    }

    @Override
    public void addLogListener(ILogoutListener logoutListener) {
        this.mLogoutListener = logoutListener;
    }
}