package com.edusoho.videoplayer.ui;

import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.edusoho.videoplayer.BuildConfig;
import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.broadcast.MessageBroadcastReceiver;
import com.edusoho.videoplayer.helper.LibUpdateHelper;
import com.edusoho.videoplayer.media.ILogoutListener;
import com.edusoho.videoplayer.media.IPlayerStateListener;
import com.edusoho.videoplayer.media.IVideoPlayer;
import com.edusoho.videoplayer.media.M3U8Stream;
import com.edusoho.videoplayer.media.VideoPlayerFactory;
import com.edusoho.videoplayer.media.listener.SimpleVideoControllerListener;
import com.edusoho.videoplayer.util.FileUtils;
import com.edusoho.videoplayer.util.M3U8Util;
import com.edusoho.videoplayer.util.VLCInstance;
import com.edusoho.videoplayer.util.VLCOptions;
import com.edusoho.videoplayer.view.VideoControllerView;
import com.edusoho.videoplayer.view.VideoPlayerHeaderView;

import org.videolan.libvlc.util.VLCUtil;

import java.util.List;

/**
 * Created by suju on 16/12/7.
 */

public class VideoPlayerFragment extends Fragment implements MessageBroadcastReceiver.Callback, IPlayerStateListener {

    private static final String TAG                    = "VideoPlayerFragment";
    private static final String VERSION                = BuildConfig.VERSION_NAME;
    //    private static final String SEEK_POSITION    = "seek_position";
    public static final  String PLAY_URI               = "play_uri";
    public static final  String SUBTITLE_URI           = "subtitle_uri";
    public static final  String SUBTITLE_DELAY_TIME    = "subtitle_delay_time";
    //    public static final  String PLAY_POSITION    = "play_position";
    public static final  String PLAY_MEDIA_CODER       = "play_media_coder";
    public static final  String PLAY_MEDIA_PLAYER_TYPE = "play_media_player_type";
    public static final  String PLAY_DIGEST_KET        = "play_digest_key";

    private ILogoutListener          mLogoutListener;
    private VideoControllerView      mVideoControllerView;
    private VideoPlayerHeaderView    mVideoPlayerHeaderView;
    private MessageBroadcastReceiver mMessageBroadcastReceiver;

    private String    mMediaUrl;
    private List<Uri> mSubtitleUris;
    private Uri       mPlayUri;
    //private long   mSeekPosition;
    private int       mMediaCoder;
    private int       mMediaPlayerType;
    private ViewGroup mPlayContainer;
    private View      mAudioCover;
    private boolean   hasRegistedMessageReceiver;
    private Object    messageReceiverLock = new Object();

    private IVideoPlayer                      mVideoPlayer;
    private LibUpdateHelper                   mLibUpdateHelper;
    private AsyncTask<String, String, String> mDownloadTask;

    protected            PowerManager.WakeLock mWakeLock  = null;
    private              boolean               isAudioOn;
    private static final String                POWER_LOCK = "VideoPlayerFragmentLock";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        if (getArguments() != null) {
            mMediaCoder = getArguments().getInt(PLAY_MEDIA_CODER, VLCOptions.NONE_RATE);
            mMediaPlayerType = getArguments().getInt(PLAY_MEDIA_PLAYER_TYPE, VideoPlayerFactory.EXO_PLAYER);
            mMediaUrl = getArguments().getString(PLAY_URI);
            mSubtitleUris = (List<Uri>) getArguments().getSerializable(SUBTITLE_URI);
        }

        if (!TextUtils.isEmpty(mMediaUrl)) {
            mPlayUri = Uri.parse(mMediaUrl);
        }

        initWakeLock();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.content_video_layout, null);
        ViewGroup parent = (ViewGroup) contentView.getParent();
        if (parent != null) {
            parent.removeView(contentView);
        }
        return contentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPlayContainer = view.findViewById(R.id.fl_player_contaner);
        if (getActivity().isFinishing() || isDetached()) {
            return;
        }
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mVideoControllerView = view.findViewById(R.id.vc_player_controller);
        createVideoPlayer(mMediaPlayerType, new VideoPlayerGetCallback() {
            @Override
            public void success() {
                if (mPlayUri != null) {
                    mVideoPlayer.setSubtitlesUrls(mSubtitleUris);
                    parseMediaWrapper(mPlayUri);
                }
            }
        });

        if (mLogoutListener != null) {
            mVideoPlayer.addLogListener(mLogoutListener);
        }
        mVideoControllerView.showControllerBar(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoPlayer != null && !isAudioOn) {
            mVideoPlayer.onStart();
        }
        if (null != mWakeLock && (!mWakeLock.isHeld())) {
            mWakeLock.acquire();
        }
        registMessageReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoPlayer != null) {
            savePosition(mVideoPlayer.getPosition());
            if (!isAudioOn) {
                mVideoPlayer.pause();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        unRegistMessageReceiver();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mLibUpdateHelper != null) {
            mLibUpdateHelper.stop();
        }
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
            Log.d(TAG, "cancel task");
        }
        if (mVideoPlayer != null) {
            mVideoPlayer.onStop();
        }
        Log.d(TAG, "onDestroyView");
    }

    private void checkLibExists(final VideoPlayerGetCallback callback) {
        if (mMediaPlayerType != VideoPlayerFactory.VLC_PLAYER || VLCInstance.testCompatibleCPU(getContext())) {
            if (callback != null) {
                callback.success();
            }
            return;
        }

        Log.d(TAG, "no match lib");
        mLibUpdateHelper = new LibUpdateHelper(getActivity());
        mLibUpdateHelper.update(VLCUtil.getMachineType(), new LibUpdateHelper.LibUpdateListener() {
            @Override
            public void onInstalled() {
                Log.d(TAG, "解码库更新完成");
                if (callback != null) {
                    callback.success();
                }
            }

            @Override
            public void onFail() {
                Toast.makeText(getContext().getApplicationContext(), R.string.video_not_support, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initWakeLock() {
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);
        mWakeLock.setReferenceCounted(false);
    }

    private void createVideoPlayer(int mediaPlayerType, VideoPlayerGetCallback videoPlayerGetCallback) {
        if (mVideoPlayer != null) {
            mPlayContainer.removeView(mVideoPlayer.getView());
            mVideoPlayer.onStop();
            mVideoPlayer = null;
        }
        mVideoPlayer = getVideoPlayer(mediaPlayerType);
        mVideoPlayer.setDigestKey(getArguments().getString(PLAY_DIGEST_KET, ""));
        mPlayContainer.addView(mVideoPlayer.getView());
        mVideoControllerView.clearControllerListener();
        addVideoControllerListener(getDefaultControllerListener());
        if (mAudioCover != null) {
            if (isAudioOn) {
                mAudioCover.setVisibility(View.VISIBLE);
                mVideoControllerView.setScreenChangeVisible(View.GONE);
            } else {
                mAudioCover.setVisibility(View.GONE);
                mVideoControllerView.setScreenChangeVisible(View.VISIBLE);
            }
            mPlayContainer.addView(mAudioCover);
        }
        mVideoControllerView.bringToFront();
        mVideoPlayer.addVideoController(mVideoControllerView);
        checkLibExists(videoPlayerGetCallback);
    }

    protected void setCached(boolean cached) {
        if (mVideoControllerView != null) {
            mVideoControllerView.setCached(cached);
        }
    }

    protected void setAudioOn(boolean isAudioOn) {
        this.isAudioOn = isAudioOn;
    }

    protected void setAudioCover(View view) {
        mAudioCover = view;
    }

    protected void registMessageReceiver() {
        synchronized (messageReceiverLock) {
            if (hasRegistedMessageReceiver) {
                return;
            }
        }

        mMessageBroadcastReceiver = new MessageBroadcastReceiver(this);
        getContext().registerReceiver(mMessageBroadcastReceiver, new IntentFilter(MessageBroadcastReceiver.ACTION));
        hasRegistedMessageReceiver = true;
    }

    protected void unRegistMessageReceiver() {
        synchronized (messageReceiverLock) {
            if (!hasRegistedMessageReceiver) {
                return;
            }
        }

        if (mMessageBroadcastReceiver != null) {
            getContext().unregisterReceiver(mMessageBroadcastReceiver);
            mMessageBroadcastReceiver = null;
            hasRegistedMessageReceiver = false;
        }
    }

    private IVideoPlayer getVideoPlayer(int mediaPlayerType) {
        if (mVideoPlayer != null) {
            return mVideoPlayer;
        }
        synchronized (this) {
            if (mVideoPlayer == null) {
                mVideoPlayer = VideoPlayerFactory.getInstance().createPlayer(getContext(), mMediaCoder, mediaPlayerType);
            }
        }

        return mVideoPlayer;
    }

    protected void setSeekPosition(long position) {
        mVideoPlayer.setSeekPosition(position);
    }

    protected void savePosition(long seekTime) {
        //mSeekPosition = seekTime;
//        mVideoPlayer.setSeekPosition(seekTime);
    }

    protected void setVideoSize(int w, int h) {
        ViewGroup.LayoutParams lp = getView().getLayoutParams();
        lp.height = h;
        lp.width = w;
        getView().setLayoutParams(lp);
    }

    public void addVideoControllerListener(VideoControllerView.ControllerListener listener) {
        mVideoControllerView.setControllerListener(listener);
    }

    protected void setVideoPlayerHeaderView(VideoPlayerHeaderView headerView) {
        this.mVideoPlayerHeaderView = headerView;
        ViewGroup rootView = (ViewGroup) getView();

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.gravity = Gravity.TOP;
        rootView.addView(headerView, lp);

        mVideoPlayerHeaderView.setHeaderEventListener(mHeaderEventListener);
    }

    protected VideoPlayerHeaderView.HeaderEventListener mHeaderEventListener = new VideoPlayerHeaderView.HeaderEventListener() {
        @Override
        public void onBack() {
            changeHeaderViewStatus(false);
            changeScreenLayout(Configuration.ORIENTATION_PORTRAIT);
        }
    };

    protected VideoControllerView.ControllerListener getDefaultControllerListener() {

        return new SimpleVideoControllerListener() {
            @Override
            public void onChangeScreen(int orientation) {
                super.onChangeScreen(orientation);
                changeHeaderViewStatus(orientation == Configuration.ORIENTATION_LANDSCAPE);
                changeScreenLayout(orientation);
            }

            @Override
            public void onChangeOverlay(boolean isShow) {
                super.onChangeOverlay(isShow);
                changeHeaderViewStatus(isShow);
            }

            @Override
            public void onChangePlaySource(String url) {
                super.onChangePlaySource(url);
            }
        };
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

    private void exit(int resultCode) {
        if (getActivity().isFinishing()) {
            return;
        }
        getActivity().finish();
    }

    public synchronized void playVideo(String videoUri) {
        mMediaUrl = videoUri;
        mPlayUri = Uri.parse(mMediaUrl);
        if (mVideoPlayer != null) {
            parseMediaWrapper(mPlayUri);
        }
    }

    public synchronized void playVideo(String videoUri, List<Uri> subtitles) {
        mMediaUrl = videoUri;
        mPlayUri = Uri.parse(mMediaUrl);
        if (mVideoPlayer != null) {
            mVideoPlayer.setSubtitlesUrls(subtitles);
            parseMediaWrapper(mPlayUri);
        }
    }

    private void startPlayback() {
        if (getActivity() == null || getContext() == null || getActivity().isFinishing()) {
            return;
        }

        mVideoPlayer.start();
    }

    private void parseMediaWrapper(final Uri uri) {
        if (TextUtils.isEmpty(uri.getPath())
                || "file".equals(uri.getScheme())
                || !uri.getPath().endsWith(".m3u8")) {
            playWithMedia(uri.toString());
            return;
        }

        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
        mDownloadTask = new AsyncTask<String, String, String>() {
            @Override
            protected String doInBackground(String... params) {
                if (isDetached() || isCancelled() || getContext() == null) {
                    return null;
                }
                return M3U8Util.downloadM3U8File(getContext(), params[0].toString());
            }

            @Override
            protected void onPostExecute(String url) {
                if (TextUtils.isEmpty(url)) {
                    return;
                }
                List<M3U8Stream> m3U8StreamList = M3U8Util.getM3U8StreamListFromPath(FileUtils.getParent(mPlayUri.toString()), url);
                if (m3U8StreamList == null || m3U8StreamList.isEmpty()) {
                    playWithMedia(uri.toString());
                    return;
                }
                mVideoControllerView.setM3U8StreamList(m3U8StreamList);
                String playUri = mVideoControllerView.getCurrentM3U8Stream() == null ? m3U8StreamList.get(0).getUrl() : mVideoControllerView.getCurrentM3U8Stream().getUrl();
                playWithMedia(playUri);
            }
        };
        mDownloadTask.execute(uri.toString());
    }

    private void playWithMedia(String mediaUrl) {
        mVideoPlayer.setMediaSource(mediaUrl);
        mVideoPlayer.addPlayerStateListener(this);
    }

    @Override
    public void onPrepare() {
        startPlayback();
        if (mVideoControllerView != null) {
            mVideoControllerView.setVisibility(View.VISIBLE);
            mVideoControllerView.setScreenChangeVisible(this.isAudioOn ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onFinish() {

    }

    @Override
    public void onPlaying() {

    }

    public void play() {
        if (mVideoPlayer != null) {
            mVideoPlayer.play();
        }
    }

    public void pause() {
        if (mVideoPlayer != null) {
            mVideoPlayer.pause();
        }
    }

    protected long getVideoLength() {
        return mVideoPlayer.getVideoLength();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mVideoControllerView.updateControllerConfiguration(newConfig.orientation);
    }

    public void stop() {
        if (mVideoPlayer != null) {
            mVideoPlayer.pause();
        }
    }

    @Override
    public void onReceive(String type, String mesasge) {
        if ("VideoFileNotFound".equals(type)) {
            Toast.makeText(getContext(), "文件未发现", Toast.LENGTH_SHORT).show();
        } else if ("MediaCodecError".equals(type)) {
            mMediaPlayerType = VideoPlayerFactory.VLC_PLAYER;
            createVideoPlayer(mMediaPlayerType, new VideoPlayerGetCallback() {
                @Override
                public void success() {
                    mVideoPlayer.addPlayerStateListener(VideoPlayerFragment.this);
                    mVideoPlayer.onStart();
                }
            });
        }
    }

    public void setHideStream(){
        mVideoControllerView.setShowStream(true);
    }

    public void setShowStream(){
        mVideoControllerView.setShowStream(false);
    }

    public void addLogoutListener(ILogoutListener logoutListener) {
        this.mLogoutListener = logoutListener;
        if (mVideoPlayer != null) {
            mVideoPlayer.addLogListener(logoutListener);
        }
    }

    public long getCurrentPosition() {
        return mVideoPlayer.getPosition();
    }

    interface VideoPlayerGetCallback {

        void success();
    }
}
