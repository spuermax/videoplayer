package com.edusoho.videoplayer.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * Created by suju on 17/2/17.
 */

public abstract class AbstractVideoPlayerFragment extends Fragment {

    public static final String PLAY_URI = "play_uri";
    public static final String PLAY_POSITION = "play_position";

    protected PowerManager.WakeLock mWakeLock = null;
    private static final String POWER_LOCK = "VideoPlayerFragmentLock";

    private String mMediaUrl;
    private long mSeekPosition;

    private void initWakeLock() {
        PowerManager pm = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, POWER_LOCK);
        mWakeLock.setReferenceCounted(false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaUrl = getArguments().getString(PLAY_URI);
            mSeekPosition = getArguments().getInt(PLAY_POSITION);
        }
        initWakeLock();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != mWakeLock && (!mWakeLock.isHeld())) {
            mWakeLock.acquire();
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
    }

    protected void setMediaUrl(String mediaUrl) {
        this.mMediaUrl = mediaUrl;
    }

    protected void setSeekPosition(long seekPosition) {
        this.mSeekPosition = seekPosition;
    }

    protected String getMediaUrl() {
        return mMediaUrl;
    }

    protected long getSeekPosition() {
        return mSeekPosition;
    }

    public abstract void play();

    public abstract void pause();

    public abstract void playVideo(String videoUri);

    protected abstract void changeMediaSource(String url);
}
