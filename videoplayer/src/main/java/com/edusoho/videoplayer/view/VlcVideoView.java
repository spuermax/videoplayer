package com.edusoho.videoplayer.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.helper.ControllerViewTouchHelper;
import com.edusoho.videoplayer.media.ILogoutListener;
import com.edusoho.videoplayer.media.IPlayerStateListener;
import com.edusoho.videoplayer.media.IVideoPlayer;
import com.edusoho.videoplayer.media.M3U8Stream;
import com.edusoho.videoplayer.media.MediaWrapper;
import com.edusoho.videoplayer.service.Client;
import com.edusoho.videoplayer.service.Helper;
import com.edusoho.videoplayer.service.IPlayerServcie;
import com.edusoho.videoplayer.service.VLCPlayService;
import com.edusoho.videoplayer.service.listener.PlayCallback;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.List;

/**
 * Created by suju on 16/12/7.
 */

public class VlcVideoView extends FrameLayout implements
        Client.Callback, PlayCallback, IVLCVout.Callback, IVideoPlayer {

    private static final String TAG = "VideoPlayerFragment";

    private static final int START_PLAYBACK     = 4;
    private static final int PROGRESS_CHANGE    = 2;
    private static final int PLAY_STATUS_CHANGE = 3;
    private static final int PROGRESS_BUFFERING = 5;
    private static final int PROGRESS_COMPLETE  = 6;

    private VideoControllerView   mVideoControllerView;
    private VideoPlayerHeaderView mVideoPlayerHeaderView;
    private ProgressBar           mProgressView;
    private SurfaceView           mSurfaceView = null;
    private VLCPlayService        mService;

    private String  mMediaUrl;
    private Uri     mPlayUri;
    /**
     * 切到桌面返回播放保存seek点
     */
    private long    mSeekPosition;
    private boolean mIsPlaying = false;
    boolean mWasPaused = false;
    private boolean mPlaybackStarted  = false;
    private boolean mSurfacesAttached = false;

    private Helper               mHelper;
    private ILogoutListener      mLogoutListener;
    private IPlayerStateListener mIPlayerStateListener;

    protected            PowerManager.WakeLock mWakeLock  = null;
    private static final String                POWER_LOCK = "VideoPlayerFragmentLock";

    public VlcVideoView(Context context) {
        super(context);
        onCreateView();
    }

    public VlcVideoView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        onCreateView();
    }

    @TargetApi(21)
    public VlcVideoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        onCreateView();
    }

    private void onCreateView() {
        mHelper = new Helper(getContext(), this, getServiceIntent());
        LayoutInflater.from(getContext()).inflate(R.layout.view_vlc_video_layout, this);
        setSurfaceView((SurfaceView) findViewById(R.id.sv_player_surface));
        setProgressView((ProgressBar) findViewById(R.id.pb_player_progress));
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
    }

    public void setProgressView(ProgressBar progressView) {
        this.mProgressView = progressView;
    }

    protected Intent getServiceIntent() {
        return new Intent(getContext(), VLCPlayService.class);
    }

    protected VideoControllerView.ControllerListener getDefaultControllerListener() {
        return new VideoControllerView.ControllerListener() {
            @Override
            public void onSeek(int position) {
                mService.seekByDelayed(position, 0);
            }

            @Override
            public void onChangeScreen(int orientation) {
                changeHeaderViewStatus(orientation == Configuration.ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onPlayStatusChange(boolean isPlay) {
                Log.d(TAG, "onPlayStatusChange:" + isPlay);
                if (isPlay) {
                    if (!mService.hasMedia()) {
                        requestMediaUri();
                        return;
                    }
                    play();
                } else {
                    pause();
                }
            }

            @Override
            public void onChangeRate(float rate) {
                mService.setRate(rate);
            }

            @Override
            public void onChangePlaySource(String url) {
                mSeekPosition = mService.getTime();
                changeMediaSource(url);
            }

            @Override
            public void onChangeOverlay(boolean isShow) {
                changeHeaderViewStatus(isShow);
            }

            @Override
            public void onPosition(int position, int duration) {

            }
        };
    }

    protected void requestMediaUri() {
        playVideo(mMediaUrl);
    }

    protected void changeHeaderViewStatus(boolean isShow) {
        if (mVideoPlayerHeaderView == null) {
            return;
        }
        int visibility = isShow ? View.VISIBLE : View.INVISIBLE;
        Log.d(TAG, "changeHeaderViewStatus:" + getResources().getConfiguration().orientation);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            visibility = View.INVISIBLE;
        }
        mVideoPlayerHeaderView.setVisibility(visibility);
    }

    public synchronized void playVideo(String videoUri) {
        if (TextUtils.isEmpty(videoUri)) {
            if (mLogoutListener != null) {
                mLogoutListener.onLog("vlcvideo", "media not found:" + videoUri);
            }
            Toast.makeText(getContext(), "视频播放地址无效,请退出重试", Toast.LENGTH_SHORT).show();
            return;
        }
        mMediaUrl = videoUri;
        mPlayUri = Uri.parse(mMediaUrl);
        mWasPaused = false;
        if (mPlaybackStarted) {
            playWithMedia(new MediaWrapper(mPlayUri));
            return;
        }
        start();
    }

    private void stopPlayback() {
        if (!mPlaybackStarted)
            return;

        mWasPaused = !mService.isPlaying();
        mPlaybackStarted = false;
        mService.setVideoTrackEnabled(false);
        mService.removeCallback(this);

        IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.removeCallback(this);
        if (mSurfacesAttached)
            vlcVout.detachViews();

        //savePosition(mService.getTime());
        Log.d("flag--", "stopPlayback: ");
        if (isFocused()) {
            mService.stop();
            Log.d("flag--", "stopPlayback: stop()");
        } else {
            mService.pause();
            Log.d("flag--", "stopPlayback: pause()");
            mVideoControllerView.updatePlayStatus(false);
        }
    }

    private void changeMediaSource(String url) {
        if (mService == null) {
            return;
        }
        mIsPlaying = false;

        KeyguardManager km = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            mWasPaused = true;
        }

        mService.addCallback(this);
        if (mService.isPlaying()) {
            mService.stop();
        }
        playWithMedia(new MediaWrapper(Uri.parse(url)));
    }

    private void loadMedia() {
        if (mService == null)
            return;
        mIsPlaying = false;

        final KeyguardManager km = (KeyguardManager) getContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode()) {
            mWasPaused = true;
        }

        if (mPlayUri != null) {
            if (mService.hasMedia() && mVideoControllerView != null) {
                checkStopMedia();
            }

            mService.addCallback(this);
            /* prepare playback */
            boolean hasMedia = mService.hasMedia();
            if (!hasMedia) {
                playWithMedia(new MediaWrapper(mPlayUri));
            } else {
                mService.playIndex(0);
            }
            mService.setRate(1.0f);
        } else {
            if (mLogoutListener != null) {
                mLogoutListener.onLog("vlcvideo", "media no found:" + mMediaUrl);
            }
            Toast.makeText(getContext(), "播放地址无效!请退出重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkStopMedia() {
        M3U8Stream m3U8Stream = mVideoControllerView.getCurrentM3U8Stream();
        String currentMediaUrl = mService.getCurrentMediaWrapper().getUri().toString();
        if (m3U8Stream == null && !mPlayUri.equals(currentMediaUrl)) {
            mService.stop();
            return;
        }
        if (m3U8Stream != null && !m3U8Stream.getUrl().equals(currentMediaUrl)) {
            mService.stop();
        }
    }

    private void playWithMedia(List<MediaWrapper> mediaList) {
        for (MediaWrapper mediaWrapper : mediaList) {
            setMediaFlag(mediaWrapper);
        }
        mService.load(mediaList, 0);
    }

    private void setMediaFlag(MediaWrapper media) {
        Log.d(TAG, "mWasPaused:" + mWasPaused);
        if (mWasPaused) {
            media.addFlags(MediaWrapper.MEDIA_PAUSED);
        }

        //media.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
        media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
        media.addFlags(MediaWrapper.MEDIA_VIDEO);
    }

    private void playWithMedia(MediaWrapper media) {
        setMediaFlag(media);

        Log.d(TAG, "mSeekPosition:" + mSeekPosition);
        if (mSeekPosition > 0 && !mService.isPlaying()) {
            mService.saveTimeToSeek(mSeekPosition);
        }
        // Handle playback
        mService.load(media);
    }

    /**
     * Handle resize of the surface and the overlay
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mService == null)
                return true;

            switch (msg.what) {
                case PROGRESS_BUFFERING:
                    if ((float) msg.obj >= 100.0) {
                        updateProcessBarStatus(View.INVISIBLE);
                        return true;
                    }
                    updateProcessBarStatus(View.VISIBLE);
                    mVideoControllerView.updateMediaBufferState((float) msg.obj);
                    break;
                case PROGRESS_COMPLETE:
                    updateProcessBarStatus(View.INVISIBLE);
                    break;
                case PLAY_STATUS_CHANGE:
                    updateMediaPlayStatus();
                    break;
                case START_PLAYBACK:
                    start();
                    break;
                case PROGRESS_CHANGE:
                    updateMediaProgress();
                    break;
            }
            return true;
        }
    });

    private void updateMediaProgress() {
        mVideoControllerView.updateMediaProgress((int) mService.getTime(), (int) mService.getLength());
    }

    private void updateMediaPlayStatus() {
        Log.d(TAG, "mService.isPlaying:" + mService.isPlaying());
        mVideoControllerView.updatePlayStatus(mService.isPlaying());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mVideoControllerView.updateControllerConfiguration(newConfig.orientation);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected");
    }

    @Override
    public void onConnected(IPlayerServcie service) {
        Log.d(TAG, "onConnected");
        mService = (VLCPlayService) service;
        if (mIPlayerStateListener != null) {
            mIPlayerStateListener.onPrepare();
        }
    }

    @Override
    public void onMediaEvent(Media.Event event) {
    }

    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Playing:
                mHandler.sendEmptyMessage(PROGRESS_COMPLETE);
                mHandler.sendEmptyMessage(PLAY_STATUS_CHANGE);
                if (mIPlayerStateListener != null) {
                    mIPlayerStateListener.onPlaying();
                }
                break;
            case MediaPlayer.Event.PositionChanged:
                mHandler.sendEmptyMessage(PROGRESS_COMPLETE);
                mHandler.sendEmptyMessage(PROGRESS_CHANGE);
                break;
            case MediaPlayer.Event.Buffering:
                Message message = mHandler.obtainMessage(PROGRESS_BUFFERING);
                message.obj = event.getBuffering();
                message.sendToTarget();
                break;
            case MediaPlayer.Event.EndReached:
                if (mService.hasNext()) {
                    mService.playNext(0);
                    return;
                }

                Log.d(TAG, "EndReached");
                if (mLogoutListener != null) {
                    mLogoutListener.onLog("vlcvideo", "EndReached:" + mMediaUrl);
                }
                mPlayUri = null;
                mService.pause();
                mService.seekByDelayed(0, 0);
                setSeekPosition(0);
                mVideoControllerView.updateMediaProgress(0, (int) mService.getLength());
                mVideoControllerView.updatePlayStatus(false);
                if (mIPlayerStateListener != null) {
                    mIPlayerStateListener.onFinish();
                }
                break;
        }
    }

    protected void updateProcessBarStatus(int visibility) {
        if (visibility == mProgressView.getVisibility()) {
            return;
        }
        mProgressView.setVisibility(visibility);
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        ControllerViewTouchHelper touchHelper = new ControllerViewTouchHelper((Activity) getContext());
        touchHelper.updateVideoSize(width, height);
        mVideoControllerView.setControllerViewTouchHelper(touchHelper);
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        Log.d(TAG, "onSurfacesDestroyed");
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        Log.d(TAG, "onHardwareAccelerationError");
        if (mLogoutListener != null) {
            mLogoutListener.onLog("vlcvideo", "onHardwareAccelerationError:" + mMediaUrl);
        }
    }

    //IVideoPlayer
    @Override
    public void onStart() {
        if (mHelper != null) {
            mHelper.onStart();
        }
        if (null != mWakeLock && (!mWakeLock.isHeld())) {
            mWakeLock.acquire();
        }
    }

    @Override
    public void onStop() {
        stopPlayback();
        if (mService != null)
            mService.removeCallback(this);
        if (mHelper != null) {
            mHelper.onStop();
        }
    }

    @Override
    public void addPlayerStateListener(IPlayerStateListener listener) {
        this.mIPlayerStateListener = listener;
        if (mService != null && mPlayUri != null) {
            listener.onPrepare();
        }
    }

    @Override
    public void play() {
        if (mPlayUri == null || TextUtils.isEmpty(mPlayUri.getPath())) {
            return;
        }
        if (mService != null && !mService.isPlaying()) {
            mService.play();
            mVideoControllerView.updatePlayStatus(true);
        }
    }

    @Override
    public void pause() {
        if (mService != null && mService.isPlaying()) {
            mService.pause();
            mVideoControllerView.updatePlayStatus(false);
        }
    }

    @Override
    public void setMediaSource(String url) {
        mMediaUrl = url;
        mPlayUri = Uri.parse(url);
    }

    @Override
    public void setSubtitlesUrls(List<Uri> subtitlesUris) {

    }

    @Override
    public void start() {
        /* start playback only when audio service and both surfaces are ready */
        if (mPlaybackStarted || mService == null || mPlayUri == null)
            return;

        mPlaybackStarted = true;
        IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.setVideoView(mSurfaceView);
        mSurfacesAttached = true;
        vlcVout.addCallback(this);
        vlcVout.attachViews();
        mService.setVideoTrackEnabled(true);
        loadMedia();
    }

    @Override
    public void addVideoController(VideoControllerView videoControllerView) {
        mVideoControllerView = videoControllerView;
        mVideoControllerView.setControllerListener(getDefaultControllerListener());
    }

    @Override
    public void setSeekPosition(long seekPosition) {
        //this.mSeekPosition = seekPosition;
        mService.seekByDelayed((int) seekPosition, 500);
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public long getPosition() {
        return mService != null ? mService.getTime() : 0;
    }

    @Override
    public long getVideoLength() {
        return mService != null ? mService.getLength() : 0;
    }

    @Override
    public void setDigestKey(String digestKey) {
    }

    @Override
    public void addLogListener(ILogoutListener logoutListener) {
        this.mLogoutListener = logoutListener;
    }
}
