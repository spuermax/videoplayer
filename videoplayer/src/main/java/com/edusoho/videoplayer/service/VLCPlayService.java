package com.edusoho.videoplayer.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.helper.AudioUtil;
import com.edusoho.videoplayer.media.MediaWrapper;
import com.edusoho.videoplayer.media.MediaWrapperList;
import com.edusoho.videoplayer.service.listener.PlayCallback;
import com.edusoho.videoplayer.util.Util;
import com.edusoho.videoplayer.util.VLCInstance;
import com.edusoho.videoplayer.util.VLCOptions;
import com.edusoho.videoplayer.util.WeakHandler;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by suju on 16/12/5.
 */

public class VLCPlayService extends Service implements IVLCVout.Callback, IPlayerServcie {

    private static final String TAG           = "VLCPlayService";
    private static final int    SHOW_PROGRESS = 0;
    private static final int    SHOW_TOAST    = 1;

    private SharedPreferences mSettings;
    private MediaPlayer       mMediaPlayer;

    private long            mTempPosition;
    private Stack<Integer> mPrevious;
    private int            mCurrentIndex;
    private int              mLastVolume = -1;
    private MediaWrapperList mMediaList  = new MediaWrapperList();

    private boolean mParsed          = false;
    private boolean mSeekable        = false;
    private boolean mPausable        = false;
    private boolean mIsAudioTrack    = false;
    private boolean mHasHdmiAudio    = false;
    private boolean mVideoBackground = false;

    private boolean mHasAudioFocus = false;
    private MediaSessionCompat mMediaSession;

    private PowerManager.WakeLock mWakeLock;
    private ArrayList<PlayCallback> mCallbacks = new ArrayList<PlayCallback>();

    int mPhoneEvents = PhoneStateListener.LISTEN_CALL_STATE;
    private PhoneStateListener mPhoneStateListener;

    private final IBinder mBinder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        AudioUtil.prepareCacheFolder(this);
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        mMediaPlayer = createMediaPlayer();
        mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(this));

        if (!VLCInstance.testCompatibleCPU(this)) {
            stopSelf();
            return;
        }

        mPrevious = new Stack();
        PowerManager pm = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);

        registerV21();
        if (readPhoneState()) {
            initPhoneListener();
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, mPhoneEvents);
        }
    }

    public IVLCVout getVLCVout() {
        return mMediaPlayer.getVLCVout();
    }

    public void setVideoTrackEnabled(boolean enabled) {
        if (!hasMedia() || !isPlaying())
            return;
        if (enabled)
            getCurrentMedia().addFlags(MediaWrapper.MEDIA_VIDEO);
        else
            getCurrentMedia().removeFlags(MediaWrapper.MEDIA_VIDEO);
        mMediaPlayer.setVideoTrackEnabled(enabled);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!hasCurrentMedia()) {
            stopSelf();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stopCacheServer();
        stop();

        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }

        if (mReceiverV21 != null) {
            unregisterReceiver(mReceiverV21);
        }

        mMediaPlayer.release();

        if (readPhoneState()) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    @MainThread
    private void seek(long position, long time) {
        try {
            if (time > 0)
                mMediaPlayer.setPosition((float) position / time);
            else
                mMediaPlayer.setTime(position);
        } catch (IllegalStateException e) {
            Log.d(TAG, "seek error");
        }
    }

    @MainThread
    public boolean isSeekable() {
        return mSeekable;
    }

    @MainThread
    public long getTime() {
        if (mMediaPlayer == null || mMediaPlayer.isReleased()) {
            return 0;
        }
        return mMediaPlayer.getTime();
    }

    @MainThread
    public long getLength() {
        if (mMediaPlayer == null || mMediaPlayer.isReleased()) {
            return 0;
        }
        return mMediaPlayer.getLength();
    }

    @MainThread
    public void playNext(int flags) {
        if (hasCurrentMedia()) {
            playIndex(mCurrentIndex, flags);
        }
    }

    public boolean hasNext() {
        int nextIndex = mCurrentIndex + 1;
        return nextIndex >= 0 && nextIndex < mMediaList.size();
    }

    @MainThread
    public void playIndex(int index) {
        playIndex(index, 0);
    }

    public void playIndex(int index, int flags) {
        if (mMediaList.size() == 0) {
            Log.w(TAG, "Warning: empty media list, nothing to play !");
            return;
        }
        if (index >= 0 && index < mMediaList.size()) {
            mCurrentIndex = index;
        } else {
            Log.w(TAG, "Warning: index " + index + " out of bounds");
            mCurrentIndex = 0;
        }

        MediaWrapper mw = mMediaList.getMedia(mCurrentIndex);
        if (mw == null) {
            Toast.makeText(getBaseContext(), R.string.audio_no_url, Toast.LENGTH_SHORT).show();
            return;
        }

        /* Pausable and seekable are true by default */
        mParsed = false;
        mPausable = mSeekable = true;
        mw.addFlags(MediaWrapper.MEDIA_VIDEO);
        Media media = new Media(VLCInstance.get(getApplicationContext()), mw.getUri());
        VLCOptions.setMediaOptions(media, this, flags | mw.getFlags());

        if (mw.getSlaves() != null) {
            for (Media.Slave slave : mw.getSlaves()) {
                media.addSlave(slave);
            }
        }

        media.setEventListener(mMediaListener);
        mMediaPlayer.setMedia(media);
        media.release();

        if (mw.getType() != MediaWrapper.TYPE_VIDEO || mw.hasFlag(MediaWrapper.MEDIA_FORCE_AUDIO)
                || isVideoPlaying()) {
            mMediaPlayer.setEqualizer(VLCOptions.getEqualizer(this));
            mMediaPlayer.setVideoTitleDisplay(MediaPlayer.Position.Disable, 0);
            changeAudioFocus(true);
            if ((flags & MediaWrapper.MEDIA_PAUSED) != 0) {
                mLastVolume = mMediaPlayer.getVolume();
                mMediaPlayer.setVolume(0);
            }
            mMediaPlayer.setEventListener(mMediaPlayerListener);
            mMediaPlayer.play();
        } else {
            Log.w(TAG, "play error");
        }
    }

    @MainThread
    public void playEnd() {
        Log.d(TAG, "playend");
        //saveTimeToSeek(0);
        seekByDelayed(0, 500);
    }

    public void seekByDelayed(final long seekTime, int delay) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("flag--", "seekByDelayed: " + seekTime);
                seek(seekTime, 0);
            }
        }, delay);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerV21() {
        final IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_HDMI_AUDIO_PLUG);
        registerReceiver(mReceiverV21, intentFilter);
    }

    private final BroadcastReceiver mReceiverV21 = AndroidUtil.isLolliPopOrLater() ? new BroadcastReceiver() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action == null)
                return;
            if (action.equalsIgnoreCase(AudioManager.ACTION_HDMI_AUDIO_PLUG)) {
                mHasHdmiAudio = intent.getIntExtra(AudioManager.EXTRA_AUDIO_PLUG_STATE, 0) == 1;
                if (mMediaPlayer != null && mIsAudioTrack)
                    mMediaPlayer.setAudioOutputDevice(mHasHdmiAudio ? "hdmi" : "stereo");
            }
        }
    } : null;

    private final MediaPlayer.EventListener mMediaPlayerListener = new MediaPlayer.EventListener() {

        @Override
        public void onEvent(MediaPlayer.Event event) {
            switch (event.type) {
                case MediaPlayer.Event.Playing:
                    Log.i(TAG, "MediaPlayer.Event.Playing");
                    if (mTempPosition != 0) {
                        seekByDelayed(mTempPosition, 200);
                        mTempPosition = 0;
                    }
                    changeAudioFocus(!getCurrentMedia().hasFlag(MediaWrapper.MEDIA_PAUSED));
                    if (!mWakeLock.isHeld())
                        mWakeLock.acquire();
                    mVideoBackground = false;
                    break;
                case MediaPlayer.Event.Paused:
                    Log.i(TAG, "MediaPlayer.Event.Paused");
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    break;
                case MediaPlayer.Event.EndReached:
                    Log.i(TAG, "MediaPlayer.Event.EndReached");
                    mCurrentIndex++;
                    seekByDelayed(0, 500);
                    break;
                case MediaPlayer.Event.Stopped:
                    Log.i(TAG, "MediaPlayer.Event.Stopped");
                    if (mWakeLock.isHeld())
                        mWakeLock.release();
                    changeAudioFocus(false);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    Log.w(TAG, "MediaPlayer.Event.EncounteredError");
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    break;
                case MediaPlayer.Event.PausableChanged:
                    mPausable = event.getPausable();
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    mSeekable = event.getSeekable();
                    break;
            }
            for (PlayCallback callback : mCallbacks) {
                callback.onMediaPlayerEvent(event);
            }
        }
    };

    private final Media.EventListener mMediaListener = new Media.EventListener() {
        @Override
        public void onEvent(Media.Event event) {
            boolean update = true;
            switch (event.type) {
                case Media.Event.MetaChanged:
                    Log.i(TAG, "Media.Event.MetaChanged: " + event.getMetaId());
                    break;
                case Media.Event.ParsedChanged:
                    Log.i(TAG, "Media.Event.ParsedChanged");
                    mParsed = true;
                    break;
                default:
                    update = false;
            }
            if (update) {
                for (PlayCallback callback : mCallbacks) {
                    callback.onMediaEvent(event);
                }
            }
        }
    };

    @MainThread
    public synchronized void addCallback(PlayCallback cb) {
        if (!mCallbacks.contains(cb)) {
            mCallbacks.add(cb);
            if (hasCurrentMedia())
                mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    }

    @MainThread
    public void saveTimeToSeek(long time) {
        mTempPosition = time;
    }

    @MainThread
    public MediaWrapper getCurrentMediaWrapper() {
        return getCurrentMedia();
    }

    @MainThread
    public boolean isPlaying() {
        if (mMediaPlayer == null) {
            return false;
        }
        return mMediaPlayer.isPlaying();
    }

    @MainThread
    public boolean isVideoPlaying() {
        return mMediaPlayer.getVLCVout().areViewsAttached();
    }

    @MainThread
    public void load(MediaWrapper media) {
        ArrayList<MediaWrapper> arrayList = new ArrayList<MediaWrapper>();
        arrayList.add(media);
        load(arrayList, 0);
    }

    @MainThread
    public void load(List<MediaWrapper> mediaList, int position) {
        Log.v(TAG, "Loading position " + ((Integer) position).toString() + " in " + mediaList.toString());

        if (hasCurrentMedia())
            savePosition();

        mMediaList.clear();
        MediaWrapperList currentMediaList = mMediaList;
        mPrevious.clear();
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

        playIndex(mCurrentIndex, 0);
    }

    @MainThread
    public void stop() {
        stopPlayback();
        stopSelf();
    }

    @MainThread
    public void stopPlayback() {
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession.release();
            mMediaSession = null;
        }

        if (mMediaPlayer == null)
            return;
        savePosition();
        Media media = mMediaPlayer.getMedia();
        if (media != null) {
            media.setEventListener(null);
            mMediaPlayer.setEventListener(null);
            mMediaPlayer.stop();
            mMediaPlayer.setMedia(null);
            media.release();
        }
        mCurrentIndex = -1;
        mPrevious.clear();
        mHandler.removeMessages(SHOW_PROGRESS);

        broadcastMetadata();
        changeAudioFocus(false);
    }

    @MainThread
    public boolean hasMedia() {
        return hasCurrentMedia();
    }

    @MainThread
    public synchronized void removeCallback(PlayCallback cb) {
        mCallbacks.remove(cb);
    }

    private final AudioManager.OnAudioFocusChangeListener mAudioFocusListener = createOnAudioFocusChangeListener();

    @TargetApi(Build.VERSION_CODES.FROYO)
    private AudioManager.OnAudioFocusChangeListener createOnAudioFocusChangeListener() {
        return new AudioManager.OnAudioFocusChangeListener() {
            private boolean mLossTransient = false;
            private int mLossTransientVolume = -1;
            private boolean wasPlaying = false;

            @Override
            public void onAudioFocusChange(int focusChange) {
                /*
                 * Pause playback during alerts and notifications
                 */
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.i(TAG, "AUDIOFOCUS_LOSS");
                        changeAudioFocus(false);
                        pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT");
                        mLossTransient = true;
                        wasPlaying = isPlaying();
                        if (wasPlaying)
                            pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.i(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK");
                        // Lower the volume
                        if (mMediaPlayer.isPlaying()) {
                            mLossTransientVolume = mMediaPlayer.getVolume();
                            mMediaPlayer.setVolume(36);
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.i(TAG, "AUDIOFOCUS_GAIN: " + mLossTransientVolume + ", " + mLossTransient);
                        // Resume playback
                        if (mLossTransientVolume != -1) {
                            mMediaPlayer.setVolume(mLossTransientVolume);
                            mLossTransientVolume = -1;
                        } else if (mLossTransient) {
                            if (wasPlaying)
                                mMediaPlayer.play();
                            mLossTransient = false;
                        }
                        break;
                }
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void changeAudioFocus(boolean acquire) {
        final AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am == null)
            return;

        if (acquire) {
            if (!mHasAudioFocus) {
                final int result = am.requestAudioFocus(mAudioFocusListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    am.setParameters("bgm_state=true");
                    mHasAudioFocus = true;
                }
            }
        } else {
            if (mHasAudioFocus) {
                final int result = am.abandonAudioFocus(mAudioFocusListener);
                am.setParameters("bgm_state=false");
                mHasAudioFocus = false;
            }
        }
    }

    private final Handler mHandler = new AudioServiceHandler(this);

    private static class AudioServiceHandler extends WeakHandler<VLCPlayService> {
        public AudioServiceHandler(VLCPlayService fragment) {
            super(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            VLCPlayService service = getOwner();
            if (service == null) return;

            switch (msg.what) {
                case SHOW_PROGRESS:
                    break;
                case SHOW_TOAST:
                    final Bundle bundle = msg.getData();
                    final String text = bundle.getString("text");
                    final int duration = bundle.getInt("duration");
                    Toast.makeText(service.getApplicationContext(), text, duration).show();
                    break;
            }
        }
    }

    @Nullable
    private MediaWrapper getCurrentMedia() {
        return mMediaList.getMedia(mCurrentIndex);
    }

    private synchronized void savePosition() {
        if (getCurrentMedia() == null)
            return;
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt("position_in_media_list", mCurrentIndex);
        editor.putLong("position_in_media", mMediaPlayer.getTime());
        Util.commitPreferences(editor);
    }

    private void broadcastMetadata() {
        MediaWrapper media = getCurrentMedia();
        if (media == null || media.getType() != MediaWrapper.TYPE_AUDIO)
            return;

        boolean playing = mMediaPlayer.isPlaying();

        Intent broadcast = new Intent("com.android.music.metachanged");
        broadcast.putExtra("track", media.getTitle());
        broadcast.putExtra("artist", media.getArtist());
        broadcast.putExtra("album", media.getAlbum());
        broadcast.putExtra("duration", media.getLength());
        broadcast.putExtra("playing", playing);

        sendBroadcast(broadcast);
    }

    @MainThread
    public void pause() {
        if (mPausable) {
            savePosition();
            mHandler.removeMessages(SHOW_PROGRESS);
            mMediaPlayer.pause();
            changeAudioFocus(false);
            broadcastMetadata();
        }
    }

    @MainThread
    public void setRate(float rate) {
        mMediaPlayer.setRate(rate);
    }

    @MainThread
    public void play() {
        if (hasCurrentMedia()) {
            changeAudioFocus(true);
            if (mLastVolume != -1) {
                mMediaPlayer.setVolume(mLastVolume);
                mLastVolume = -1;
            }
            mMediaPlayer.play();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
            broadcastMetadata();
        }
    }

    private boolean hasCurrentMedia() {
        return mCurrentIndex >= 0 && mCurrentIndex < mMediaList.size();
    }

    private static boolean readPhoneState() {
        return !AndroidUtil.isFroyoOrLater();
    }

    private void initPhoneListener() {
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (!mMediaPlayer.isPlaying() || !hasCurrentMedia())
                    return;
                if (state == TelephonyManager.CALL_STATE_RINGING || state == TelephonyManager.CALL_STATE_OFFHOOK)
                    pause();
                else if (state == TelephonyManager.CALL_STATE_IDLE)
                    play();
            }
        };
    }

    private LibVLC LibVLC() {
        return VLCInstance.get(getApplicationContext());
    }

    private MediaPlayer createMediaPlayer() {
        final MediaPlayer mp = new MediaPlayer(LibVLC());
        final String aout = VLCOptions.getAout(mSettings);
        if (mp.setAudioOutput(aout) && aout.equals("android_audiotrack")) {
            mIsAudioTrack = true;
            if (mHasHdmiAudio)
                mp.setAudioOutputDevice("hdmi");
        } else
            mIsAudioTrack = false;
        mp.getVLCVout().addCallback(this);

        return mp;
    }

    private class LocalBinder extends Binder implements PlayServiceBinder {
        @Override
        public IPlayerServcie getService() {
            return VLCPlayService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
        Log.d(TAG, "onSurfacesCreated:" + vlcVout);
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        Log.d(TAG, "onSurfacesDestroyed:" + vlcVout);
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {

    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {

    }
}
