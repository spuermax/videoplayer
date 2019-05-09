package com.edusoho.videoplayer.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.helper.ControllerViewTouchHelper;
import com.edusoho.videoplayer.media.MediaWrapper;
import com.edusoho.videoplayer.service.AudioPlayerService;
import com.edusoho.videoplayer.service.Client;
import com.edusoho.videoplayer.service.Helper;
import com.edusoho.videoplayer.service.IPlayerServcie;
import com.edusoho.videoplayer.service.listener.PlayCallback;
import com.edusoho.videoplayer.view.VideoControllerView;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer.Event;

/**
 * Created by suju on 16/12/18.
 */

public class AudioPlayerFragment extends Fragment
        implements Client.Callback, PlayCallback, VideoControllerView.ControllerListener {

    public static final String PLAY_URI = "play_uri";
    public static final String TAG      = "AudioPlayerFragment";

    private   Helper              mHelper;
    protected AudioPlayerService  mService;
    protected VideoControllerView mVideoControllerView;
    private   ProgressBar         mProgressView;
    private   FrameLayout         mContainerView;
    private   String              mPlayUrl;

    private static final int START_PLAYBACK     = 4;
    private static final int PROGRESS_CHANGE    = 2;
    private static final int PLAY_STATUS_CHANGE = 3;
    private static final int PROGRESS_BUFFERING = 5;
    private static final int PROGRESS_COMPLETE  = 6;
    private static final int PLAYER_PREPARE     = 7;
    private static final int STOP_PLAYBACK      = 7;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = new Helper(getContext(), this, getServiceIntent());
        Bundle bundle = getArguments();
        if (bundle != null) {
            mPlayUrl = bundle.getString(PLAY_URI);
        }
        mHelper.onStart();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressView = view.findViewById(R.id.pb_player_progress);
        mContainerView = view.findViewById(R.id.fl_player_contaner);
        mVideoControllerView = view.findViewById(R.id.vc_player_controller);
        mVideoControllerView.setControllerListener(this);
        mVideoControllerView.showControllerBar(true);
    }

    protected void setContainerView(View containerView) {
        mContainerView.removeAllViews();
        mContainerView.addView(containerView);
    }

    protected void setContainerView(View containerView, ViewGroup.LayoutParams lp) {
        mContainerView.removeAllViews();
        if (lp == null) {
            lp = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
        mContainerView.addView(containerView, lp);
    }

    private Intent getServiceIntent() {
        return new Intent(getContext(), AudioPlayerService.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            mService.removeCallback(this);
        }
        mHelper.onStop();
    }

    public void destoryService() {
        if (mService != null) {
            mService.pause();
        }
        mHelper.onStop();
        getContext().stopService(getServiceIntent());
    }

    @Override
    public void onConnected(IPlayerServcie service) {
        mService = (AudioPlayerService) service;
        mHandler.sendEmptyMessage(START_PLAYBACK);
    }

    @Override
    public void onDisconnected() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.content_audio_layout, null);
        ViewGroup parent = (ViewGroup) contentView.getParent();
        if (parent != null) {
            parent.removeView(contentView);
        }
        return contentView;
    }

    @MainThread
    private void updateMediaProgress() {
        mVideoControllerView.updateMediaProgress((int) mService.getTime(), (int) mService.getLength());
    }

    @MainThread
    protected void updateMediaPlayStatus(boolean isPlay) {
        mVideoControllerView.updatePlayStatus(isPlay);
    }

    @MainThread
    private void updateProcessBarStatus(int visibility) {
        if (visibility == mProgressView.getVisibility()) {
            return;
        }
        mProgressView.setVisibility(visibility);
    }

    @Override
    public void onSeek(int position) {
        mService.seek(position);
    }

    @Override
    public void onChangeScreen(int orientation) {
    }

    @Override
    public void onPlayStatusChange(boolean isPlay) {
        if (mService != null) {
            if (isPlay) {
                if (mService.isPlaying()) {
                    return;
                }
                mService.play();
            } else {
                mService.pause();
            }
        }
    }

    @Override
    public void onChangeRate(float rate) {
    }

    @Override
    public void onChangePlaySource(String url) {
    }

    @Override
    public void onChangeOverlay(boolean isShow) {
    }

    @Override
    public void onMediaEvent(Media.Event event) {
    }

    @Override
    public void onMediaPlayerEvent(Event event) {
        switch (event.type) {
            case Event.Playing:
                mHandler.sendEmptyMessage(PROGRESS_COMPLETE);
                mHandler.sendEmptyMessage(PLAYER_PREPARE);
                mHandler.sendEmptyMessage(PLAY_STATUS_CHANGE);
                break;
            case Event.TimeChanged:
                mHandler.sendEmptyMessage(PROGRESS_COMPLETE);
                mHandler.sendEmptyMessage(PROGRESS_CHANGE);
                break;
            case Event.Buffering:
                mHandler.sendEmptyMessage(PROGRESS_BUFFERING);
                break;
            case Event.Stopped:
                Log.d("flag--", "onMediaPlayerEvent: Event.Stopped");
                break;
            case Event.EndReached:
                stopPlayback();
                break;
        }
    }

    private void preparePlayerController() {
        ControllerViewTouchHelper touchHelper = new ControllerViewTouchHelper(getActivity());
        touchHelper.updateVideoSize(getView().getWidth(), getView().getHeight());
        mVideoControllerView.setControllerViewTouchHelper(touchHelper);
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
                case PLAYER_PREPARE:
                    preparePlayerController();
                    break;
                case PROGRESS_BUFFERING:
                    updateProcessBarStatus(View.VISIBLE);
                    break;
                case PROGRESS_COMPLETE:
                    updateProcessBarStatus(View.INVISIBLE);
                    break;
                case PROGRESS_CHANGE:
                    updateMediaProgress();
                    break;
                case PLAY_STATUS_CHANGE:
                    updateMediaPlayStatus(mService.isPlaying());
                    break;
                case START_PLAYBACK:
                    startPlayback();
            }
            return true;
        }
    });

    protected void playAudio(String uri) {
        if (TextUtils.isEmpty(uri)) {
            Toast.makeText(getContext(), R.string.audio_no_url, Toast.LENGTH_SHORT).show();
            return;
        }
        mPlayUrl = uri;
        startPlayback();
    }

    protected void stopPlayback() {
        mService.seek(0);
        mVideoControllerView.updatePlayStatus(false);
    }

    @MainThread
    protected void startPlayback() {
        if (TextUtils.isEmpty(mPlayUrl) || mService == null) {
            return;
        }
        mService.addCallback(this);
        if (mService.hasCurrentMedia()) {
            mService.play();
            return;
        }
        mService.loadMedia(new MediaWrapper(Uri.parse(mPlayUrl)));
    }
}
