package com.edusoho.videoplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edusoho.videoplayer.media.MediaWrapper;
import com.edusoho.videoplayer.media.MediaWrapperList;
import com.edusoho.videoplayer.service.listener.PlayCallback;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.MediaPlayer.Event;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by suju on 16/12/18
 */

public class AudioPlayerService extends Service implements IPlayerServcie {

    private final String TAG = "AudioPlayerService";

    private int         mCurrentIndex;
    private MediaPlayer mMediaPlayer;
    private final IBinder          mBinder    = new LocalBinder();
    private       MediaWrapperList mMediaList = new MediaWrapperList();

    private ArrayList<PlayCallback> mCallbacks = new ArrayList<PlayCallback>();
    private HandlerThread mTimeUpdateThread;
    private Handler       mTimeUpdateHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        mTimeUpdateThread = new HandlerThread("updateTime");
        mTimeUpdateThread.start();
        mTimeUpdateHandler = new Handler(mTimeUpdateThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case Event.TimeChanged:
                        Event event = new Event(Event.TimeChanged);
                        processCallback(event);
                        mTimeUpdateHandler.sendEmptyMessageDelayed(Event.TimeChanged, SystemClock.currentThreadTimeMillis() + 300);
                        break;
                }
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @MainThread
    public void loadMedia(MediaWrapper media) {
        ArrayList<MediaWrapper> arrayList = new ArrayList();
        arrayList.add(media);
        loadMedia(arrayList, 0);
    }

    @MainThread
    public synchronized void removeCallback(PlayCallback cb) {
        mCallbacks.remove(cb);
    }

    @MainThread
    public void pause() {
        mTimeUpdateHandler.removeMessages(Event.TimeChanged);
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
        }
    }

    @MainThread
    public void play() {
        mMediaPlayer.start();
        processCallback(new Event(Event.Playing));
        mTimeUpdateHandler.sendEmptyMessage(Event.TimeChanged);
    }

    @MainThread
    public void seek(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        } else {
            return false;
        }
    }

    @MainThread
    public void loadMedia(List<MediaWrapper> mediaList, int position) {
        mMediaList.clear();
        MediaWrapperList currentMediaList = mMediaList;
        for (int i = 0; i < mediaList.size(); i++) {
            currentMediaList.add(mediaList.get(i));
        }

        if (mMediaList.size() == 0) {
            Log.w(TAG, "Warning: empty media list, nothing to play !");
            return;
        }
        if (mMediaList.size() > position && position >= 0) {
            mCurrentIndex = position;
        } else {
            Log.w(TAG, "Warning: positon " + position + " out of bounds");
            mCurrentIndex = 0;
        }

        playIndex(mCurrentIndex);
    }

    public boolean hasCurrentMedia() {
        return mCurrentIndex >= 0 && mCurrentIndex < mMediaList.size();
    }

    public void playIndex(int position) {
        MediaWrapper mw = mMediaList.getMedia(mCurrentIndex);
        if (mw == null) {
            return;
        }

        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(getBaseContext(), mw.getUri());
            mMediaPlayer.prepareAsync();
            initMediaListener();

        } catch (IOException ie) {
        }
    }

    @MainThread
    public synchronized void addCallback(PlayCallback cb) {
        if (!mCallbacks.contains(cb)) {
            mCallbacks.add(cb);
        }
    }

    private void initMediaListener() {
        mMediaPlayer.setOnPreparedListener(getOnPreparedListener());
        mMediaPlayer.setOnCompletionListener(getOnCompletionListener());
        mMediaPlayer.setOnInfoListener(null);
        mMediaPlayer.setOnErrorListener(null);
        mMediaPlayer.setOnBufferingUpdateListener(getBufferingUpdateListener());
    }

    protected MediaPlayer.OnCompletionListener getOnCompletionListener() {
        return new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                processCallback(new Event(Event.EndReached));
            }
        };
    }

    protected MediaPlayer.OnBufferingUpdateListener getBufferingUpdateListener() {
        return new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent >= 100) {
                    return;
                }
                Event event = new Event(Event.Buffering);
                processCallback(event);
            }
        };
    }

    private void processCallback(Event event) {
        for (PlayCallback callback : mCallbacks) {
            callback.onMediaPlayerEvent(event);
        }
    }

    protected MediaPlayer.OnPreparedListener getOnPreparedListener() {
        return new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mp != null) {
                    mp.start();
                    Event event = new Event(Event.Playing);
                    processCallback(event);
                    mTimeUpdateHandler.sendEmptyMessage(Event.TimeChanged);
                }
            }
        };
    }

    public long getTime() {
        return mMediaPlayer == null ? 0L : mMediaPlayer.getCurrentPosition();
    }

    public long getLength() {
        return mMediaPlayer == null ? 0L : mMediaPlayer.getDuration();
    }

    @Override
    public IVLCVout getVLCVout() {
        return null;
    }

    private class LocalBinder extends Binder implements PlayServiceBinder {
        @Override
        public IPlayerServcie getService() {
            return AudioPlayerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
