/*****************************************************************************
 * VideoPlayerActivity.java
 * ****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.test;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.libvlc.util.HWDecoderUtil;
import org.videolan.vlc.media.MediaWrapper;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.FileUtils;
import org.videolan.vlc.util.Permissions;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.Util;
import org.videolan.vlc.util.VLCInstance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import suju.org.videodemo.R;

public class VideoPlayerActivity extends AppCompatActivity implements
        IVLCVout.Callback, PlaybackService.Client.Callback, PlaybackService.Callback {

    public final static String TAG = "VLC/VideoPlayerActivity";

    // Internal intent identifier to distinguish between internal launch and
    // external intent.
    public final static String PLAY_FROM_VIDEOGRID = "";
    public final static String PLAY_FROM_SERVICE = "";
    public final static String EXIT_PLAYER = "";

    public final static String PLAY_EXTRA_ITEM_LOCATION = "item_location";
    public final static String PLAY_EXTRA_SUBTITLES_LOCATION = "subtitles_location";
    public final static String PLAY_EXTRA_ITEM_TITLE = "title";
    public final static String PLAY_EXTRA_FROM_START = "from_start";
    public final static String PLAY_EXTRA_OPENED_POSITION = "opened_position";
    public final static String PLAY_DISABLE_HARDWARE = "disable_hardware";

    public final static String ACTION_RESULT = "";
    public final static String EXTRA_POSITION = "extra_position";
    public final static String EXTRA_DURATION = "extra_duration";
    public final static int RESULT_CONNECTION_FAILED = RESULT_FIRST_USER + 1;
    public final static int RESULT_PLAYBACK_ERROR = RESULT_FIRST_USER + 2;
    public final static int RESULT_HARDWARE_ACCELERATION_ERROR = RESULT_FIRST_USER + 3;
    public final static int RESULT_VIDEO_TRACK_LOST = RESULT_FIRST_USER + 4;

    private PlaybackServiceActivity.Helper mHelper = new PlaybackServiceActivity.Helper(this, this);;
    private PlaybackService mService;
    private SurfaceView mSurfaceView = null;
    private SurfaceView mSubtitlesSurfaceView = null;
    private View mRootView;
    private FrameLayout mSurfaceFrame;
    private MediaRouter mMediaRouter;
    private MediaRouter.SimpleCallback mMediaRouterCallback;
    private int mPresentationDisplayId = -1;
    private Uri mUri;
    private boolean mAskResume = true;
    private GestureDetectorCompat mDetector = null;

    private ImageView mPlaylistToggle;
    private ImageView mPlaylistNext;
    private ImageView mPlaylistPrevious;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    private SharedPreferences mSettings;
    private int mTouchControls = 0;

    /** Overlay */
    private ActionBar mActionBar;
    private ViewGroup mActionBarView;
    private View mOverlayProgress;
    private View mOverlayBackground;
    private View mOverlayButtons;
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = -1;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int FADE_OUT_INFO = 3;
    private static final int START_PLAYBACK = 4;
    private static final int AUDIO_SERVICE_CONNECTION_FAILED = 5;
    private static final int RESET_BACK_LOCK = 6;
    private static final int CHECK_VIDEO_TRACKS = 7;
    private static final int LOADING_ANIMATION = 8;
    private static final int HW_ERROR = 1000; // TODO REMOVE

    private static final int LOADING_ANIMATION_DELAY = 1000;

    private boolean mDragging;
    private boolean mShowing;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mSysTime;
    private TextView mBattery;
    private TextView mTime;
    private TextView mLength;
    private TextView mInfo;
    private View mOverlayInfo;
    private View mVerticalBar;
    private View mVerticalBarProgress;
    private boolean mIsLoading;
    private boolean mIsPlaying = false;
    private ImageView mLoading;
    private ImageView mTipsBackground;
    private ImageView mPlayPause;
    private ImageView mTracks;
    private ImageView mNavMenu;
    private ImageView mRewind;
    private ImageView mForward;
    private ImageView mAdvOptions;
    private ImageView mPlaybackSettingPlus;
    private ImageView mPlaybackSettingMinus;
    private View mObjectFocused;
    private boolean mEnableBrightnessGesture;
    private boolean mEnableCloneMode;
    private boolean mDisplayRemainingTime = false;
    private int mScreenOrientation;
    private int mScreenOrientationLock;
    private ImageView mLock;
    private ImageView mSize;

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        return super.onGenericMotionEvent(event);
    }

    private boolean mIsLocked = false;
    /* -1 is a valid track (Disable) */
    private int mLastAudioTrack = -2;
    private int mLastSpuTrack = -2;
    private int mOverlayTimeout = 0;
    private boolean mLockBackButton = false;
    boolean mWasPaused = false;

    /**
     * For uninterrupted switching between audio and video mode
     */
    private boolean mSwitchingView;
    private boolean mSwitchToPopup;
    private boolean mHardwareAccelerationError;
    private boolean mHasSubItems = false;

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    //Volume
    private AudioManager mAudioManager;
    private int mAudioMax;
    private boolean mMute = false;
    private int mVolSave;
    private float mVol;

    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction = TOUCH_NONE;
    private int mSurfaceYDisplayRange;
    private float mInitTouchY, mTouchY = -1f, mTouchX = -1f;

    //stick event
    private static final int JOYSTICK_INPUT_DELAY = 300;
    private long mLastMove;

    // Brightness
    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;

    // Tracks & Subtitles
    private MediaPlayer.TrackDescription[] mAudioTracksList;
    private MediaPlayer.TrackDescription[] mSubtitleTracksList;
    /**
     * Used to store a selected subtitle; see onActivityResult.
     * It is possible to have multiple custom subs in one session
     * (just like desktop VLC allows you as well.)
     */
    private final ArrayList<String> mSubtitleSelectedFiles = new ArrayList<>();

    /**
     * Flag to indicate whether the media should be paused once loaded
     * (e.g. lock screen, or to restore the pause state)
     */
    private boolean mPlaybackStarted = false;
    private boolean mSurfacesAttached = false;

    // Tips
    private View mOverlayTips;
    private static final String PREF_TIPS_SHOWN = "video_player_tips_shown";

    // Navigation handling (DVD, Blu-Ray...)
    private int mMenuIdx = -1;
    private boolean mIsNavMenu = false;

    /* for getTime and seek */
    private long mForcedTime = -1;
    private long mLastTime = -1;

    private OnLayoutChangeListener mOnLayoutChangeListener;
    private AlertDialog mAlertDialog;

    private LibVLC LibVLC() {
        return VLCInstance.get();
    }

    private Intent getServiceIntent() {
        return new Intent(getBaseContext(), PlaybackService.class);
    }

    @Override
    public void update() {

    }

    @Override
    public void updateProgress() {

    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!VLCInstance.testCompatibleCPU(this)) {
            exit(RESULT_CANCELED);
            return;
        }

        if (AndroidUtil.isJellyBeanMR1OrLater()) {
            // Get the media router service (Miracast)
            mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
            mMediaRouterCallback = new MediaRouter.SimpleCallback() {
                @Override
                public void onRoutePresentationDisplayChanged(
                        MediaRouter router, MediaRouter.RouteInfo info) {
                    Log.d(TAG, "onRoutePresentationDisplayChanged: info=" + info);
                    final Display presentationDisplay = info.getPresentationDisplay();
                    final int newDisplayId = presentationDisplay != null ? presentationDisplay.getDisplayId() : -1;
                    if (newDisplayId != mPresentationDisplayId)
                        removePresentation();
                }
            };
            Log.d(TAG, "MediaRouter information : " + mMediaRouter.toString());
        }

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        /* Services and miscellaneous */
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mEnableCloneMode = mSettings.getBoolean("enable_clone_mode", false);
        createPresentation();
        setContentView(R.layout.player);

        /** initialize Views an their Events */
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setBackgroundDrawable(null);
        mActionBar.setDisplayShowCustomEnabled(true);

        mRootView = findViewById(R.id.player_root);
        mActionBarView = (ViewGroup) mActionBar.getCustomView();

        //mVerticalBarProgress = findViewById(R.id.verticalbar_progress);

        mScreenOrientation = Integer.valueOf(
                mSettings.getString("screen_orientation_value", "99" /*SCREEN ORIENTATION SENSOR*/));

        mSurfaceView = (SurfaceView) findViewById(R.id.player_surface);
        mSubtitlesSurfaceView = (SurfaceView) findViewById(R.id.subtitles_surface);

        if (HWDecoderUtil.HAS_SUBTITLES_SURFACE) {
            mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
            mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        } else
            mSubtitlesSurfaceView.setVisibility(View.GONE);

        mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);

        dimStatusBar(true);
        mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);

        mSwitchingView = false;
        mHardwareAccelerationError = false;

        mAskResume = mSettings.getBoolean("dialog_confirm_resume", false);
        // Clear the resume time, since it is only used for resumes in external
        // videos.

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);


        resetHudLayout();
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (mIsLocked && mScreenOrientation == 99)
            setRequestedOrientation(mScreenOrientationLock);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Permissions.PERMISSION_STORAGE_TAG: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadMedia();
                } else {
                    Permissions.showStoragePermissionDialog(this, true);
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (mPlaybackStarted) {
            Uri uri = intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION) ?
                    (Uri) intent.getExtras().getParcelable(PLAY_EXTRA_ITEM_LOCATION) : intent.getData();
            if (uri == null || uri.equals(mUri))
                return;
            if (TextUtils.equals("file", uri.getScheme()) && uri.getPath().startsWith("/sdcard")) {
                Uri convertedUri = FileUtils.convertLocalUri(uri);
                if (convertedUri == null || convertedUri.equals(mUri))
                    return;
                else
                    uri = convertedUri;
            }
            mUri = uri;
            mTitle.setText(mService.getCurrentMediaWrapper().getTitle());

            showTitle();
            initUI();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        hideOverlay(true);

        /* Stop the earliest possible to avoid vout error */
        if (isFinishing())
            stopPlayback();
    }

    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
        stopPlayback();
        exitOK();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetHudLayout();
    }

    public void resetHudLayout() {
    }


    @Override
    protected void onStart() {
        super.onStart();
        mHelper.onStart();
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = mSettings.getFloat("brightness_value", -1f);
            if (brightness != -1f)
                setWindowBrightness(brightness);
        }
        IntentFilter filter = new IntentFilter(PLAY_FROM_SERVICE);
        filter.addAction(EXIT_PLAYER);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mServiceReceiver, filter);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);

        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();

        stopPlayback();


        restoreBrightness();
        if (mService != null)
            mService.removeCallback(this);
        mHelper.onStop();
    }

    @TargetApi(Build.VERSION_CODES.FROYO)
    private void restoreBrightness() {
        if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness * 255f);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
        // Save brightness if user wants to
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = getWindow().getAttributes().screenBrightness;
            if (brightness != -1f) {
                Editor editor = mSettings.edit();
                editor.putFloat("brightness_value", brightness);
                Util.commitPreferences(editor);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)

        mAudioManager = null;
    }

    /**
     * Add or remove MediaRouter callbacks. This is provided for version targeting.
     *
     * @param add true to add, false to remove
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void mediaRouterAddCallback(boolean add) {
        if (!AndroidUtil.isJellyBeanMR1OrLater() || mMediaRouter == null) return;

        if (add)
            mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        else
            mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void startPlayback() {
        /* start playback only when audio service and both surfaces are ready */
        if (mPlaybackStarted || mService == null)
            return;

        mPlaybackStarted = true;

        final IVLCVout vlcVout = mService.getVLCVout();

        vlcVout.setVideoView(mSurfaceView);
        mSurfacesAttached = true;
        vlcVout.addCallback(this);
        vlcVout.attachViews();
        mService.setVideoTrackEnabled(true);

        initUI();

        loadMedia();
        mService.setRate(1.0f);

    }

    private void initUI() {

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void stopPlayback() {
        if (!mPlaybackStarted)
            return;

        mWasPaused = !mService.isPlaying();

        mPlaybackStarted = false;

        mService.setVideoTrackEnabled(false);
        mService.removeCallback(this);

        mHandler.removeCallbacksAndMessages(null);
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.removeCallback(this);
        if (mSurfacesAttached)
            vlcVout.detachViews();

        if (mSwitchingView && mService != null) {
            Log.d(TAG, "mLocation = \"" + mUri + "\"");
            return;
        }

        cleanUI();

        long time = getTime();
        long length = mService.getLength();
        //remove saved position if in the last 5 seconds
        if (length - time < 5000)
            time = 0;
        else
            time -= 2000; // go back 2 seconds, to compensate loading time
        if (isFinishing())
            mService.stop();
        else
            mService.pause();

        Editor editor = mSettings.edit();
        // Save position
        if (mService.isSeekable()) {

        }
        // Save selected subtitles
        String subtitleList_serialized = null;
        if (mSubtitleSelectedFiles.size() > 0) {
            Log.d(TAG, "Saving selected subtitle files");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(mSubtitleSelectedFiles);
                subtitleList_serialized = bos.toString();
            } catch (IOException e) {
            }
        }

    }

    private void cleanUI() {

        if (mRootView != null)
            mRootView.setKeepScreenOn(false);

        if (mDetector != null) {
            mDetector.setOnDoubleTapListener(null);
            mDetector = null;
        }

        /* Stop listening for changes to media routes. */
        if (mMediaRouter != null)
            mediaRouterAddCallback(false);

        if (mSurfaceFrame != null && AndroidUtil.isHoneycombOrLater() && mOnLayoutChangeListener != null)
            mSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);

        if (AndroidUtil.isICSOrLater())
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (data == null) return;

        if (data.getData() == null)
            Log.d(TAG, "Subtitle selection dialog was cancelled");
        else {
        }
    }

    public static void start(Context context, Uri uri) {
        start(context, uri, null, false, -1);
    }

    public static void start(Context context, Uri uri, boolean fromStart) {
        start(context, uri, null, fromStart, -1);
    }

    public static void start(Context context, Uri uri, String title) {
        start(context, uri, title, false, -1);
    }

    public static void startOpened(Context context, Uri uri, int openedPosition) {
        start(context, uri, null, false, openedPosition);
    }

    private static void start(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = getIntent(context, uri, title, fromStart, openedPosition);

        context.startActivity(intent);
    }

    @NonNull
    public static Intent getIntent(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        return getIntent(PLAY_FROM_VIDEOGRID, context, uri, title, fromStart, openedPosition);
    }

    @NonNull
    public static Intent getIntent(String action, Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.setAction(action);
        intent.putExtra(PLAY_EXTRA_ITEM_LOCATION, uri);
        intent.putExtra(PLAY_EXTRA_ITEM_TITLE, title);
        intent.putExtra(PLAY_EXTRA_FROM_START, fromStart);

        if (openedPosition != -1 || !(context instanceof Activity)) {
            if (openedPosition != -1)
                intent.putExtra(PLAY_EXTRA_OPENED_POSITION, openedPosition);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
                if (mBattery == null)
                    return;
                int batteryLevel = intent.getIntExtra("level", 0);
                if (batteryLevel >= 50)
                    mBattery.setTextColor(Color.GREEN);
                else if (batteryLevel >= 30)
                    mBattery.setTextColor(Color.YELLOW);
                else
                    mBattery.setTextColor(Color.RED);
                mBattery.setText(String.format("%d%%", batteryLevel));
            }
        }
    };

    private void exit(int resultCode) {
        if (isFinishing())
            return;
        Intent resultIntent = new Intent(ACTION_RESULT);
        if (mUri != null && mService != null) {
            resultIntent.setData(mUri);
            resultIntent.putExtra(EXTRA_POSITION, mService.getTime());
            resultIntent.putExtra(EXTRA_DURATION, mService.getLength());
        }
        setResult(resultCode, resultIntent);
        finish();
    }

    private void exitOK() {
        exit(RESULT_OK);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mIsLoading)
            return false;
        showOverlay();
        return true;
    }

    @TargetApi(12) //only active for Android 3.1+
    public boolean dispatchGenericMotionEvent(MotionEvent event) {

        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    private boolean navigateDvdMenu(int keyCode) {
          return false;
    }


    public void showDelayControls() {
        initPlaybackSettingInfo();
    }

    private void initPlaybackSettingInfo() {

    }


    /**
     * Show text in the info view and vertical progress bar for "duration" milliseconds
     * @param text
     * @param duration
     * @param barNewValue new volume/brightness value (range: 0 - 15)
     */
    private void showInfoWithVerticalBar(String text, int duration, int barNewValue) {
        showInfo(text, duration);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mVerticalBarProgress.getLayoutParams();
        layoutParams.weight = barNewValue;
        mVerticalBarProgress.setLayoutParams(layoutParams);
        mVerticalBar.setVisibility(View.VISIBLE);
    }

    /**
     * Show text in the info view for "duration" milliseconds
     * @param text
     * @param duration
     */
    private void showInfo(String text, int duration) {

    }

    /**
     * hide the info view with "delay" milliseconds delay
     * @param delay
     */
    private void hideInfo(int delay) {
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay);
    }

    /**
     * hide the info view
     */
    private void hideInfo() {
        hideInfo(0);
    }

    private void fadeOutInfo() {

    }

    @Override
    public void onMediaEvent(Media.Event event) {
        switch (event.type) {
            case Media.Event.ParsedChanged:
                break;
            case Media.Event.MetaChanged:
                break;
            case Media.Event.SubItemTreeAdded:
                mHasSubItems = true;
                break;
        }
    }

    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        Log.i(TAG, "Event:" + event.type);
        switch (event.type) {
            case MediaPlayer.Event.Opening:
                mHasSubItems = false;
                break;
            case MediaPlayer.Event.Playing:
                onPlaying();
                break;
            case MediaPlayer.Event.Paused:
                updateOverlayPausePlay();
                break;
            case MediaPlayer.Event.Stopped:
                exitOK();
                break;
            case MediaPlayer.Event.EndReached:
                /* Don't end the activity if the media has subitems since the next child will be
                 * loaded by the PlaybackService */
                if (!mHasSubItems)
                    endReached();
                break;
            case MediaPlayer.Event.EncounteredError:
                encounteredError();
                break;
            case MediaPlayer.Event.TimeChanged:
                Log.i(TAG, "TimeChanged:" + event.getTimeChanged() + "-:" + mService.getLength());
                break;
            case MediaPlayer.Event.Vout:
                if (mMenuIdx == -1)
                    handleVout(event.getVoutCount());
                break;
            case MediaPlayer.Event.ESAdded:
            case MediaPlayer.Event.ESDeleted:
                if (mMenuIdx == -1 && event.getEsChangedType() == Media.Track.Type.Video) {
                    mHandler.removeMessages(CHECK_VIDEO_TRACKS);
                    mHandler.sendEmptyMessageDelayed(CHECK_VIDEO_TRACKS, 1000);
                }
                invalidateESTracks(event.getEsChangedType());
                break;
            case MediaPlayer.Event.SeekableChanged:
                break;
            case MediaPlayer.Event.PausableChanged:
                break;
            case MediaPlayer.Event.Buffering:
                if (!mIsPlaying)
                    break;
                if (event.getBuffering() == 100f)
                    stopLoading();
                else if (!mHandler.hasMessages(LOADING_ANIMATION) && !mIsLoading
                        && mTouchAction != TOUCH_SEEK && !mDragging)
                    mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
                break;
        }
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
                case FADE_OUT:
                    hideOverlay(false);
                    break;
                case SHOW_PROGRESS:
                    break;
                case FADE_OUT_INFO:
                    fadeOutInfo();
                    break;
                case START_PLAYBACK:
                    startPlayback();
                    break;
                case AUDIO_SERVICE_CONNECTION_FAILED:
                    exit(RESULT_CONNECTION_FAILED);
                    break;
                case RESET_BACK_LOCK:
                    mLockBackButton = true;
                    break;
                case CHECK_VIDEO_TRACKS:
                    Log.i(TAG, "No video track, open in audio mode");
                    break;
                case LOADING_ANIMATION:
                    startLoading();
                    break;
                case HW_ERROR:
                    handleHardwareAccelerationError();
                    break;
            }
            return true;
        }
    });

    private boolean canShowProgress() {
        return !mDragging && mShowing && mService != null && mService.isPlaying();
    }

    private void onPlaying() {
        mIsPlaying = true;
        stopLoading();
        updateOverlayPausePlay();
        if (!mService.getCurrentMediaWrapper().hasFlag(MediaWrapper.MEDIA_PAUSED))
            mHandler.sendEmptyMessageDelayed(FADE_OUT, OVERLAY_TIMEOUT);
    }

    private void endReached() {
        if (mService == null) {
            return;
        }
        Log.d(TAG, "Found a video playlist, expanding it");
    }

    private void encounteredError() {
        if (isFinishing())
            return;

    }

    private void handleHardwareAccelerationError() {
        mHardwareAccelerationError = true;
    }

    private void handleVout(int voutCount) {
        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached() && voutCount == 0) {
            /* Video track lost, open in audio mode */
            Log.i(TAG, "Video track lost, switching to audio");
            mSwitchingView = true;
            exit(RESULT_VIDEO_TRACK_LOST);
        }
    }

    public void switchToPopupMode() {
        if (mHardwareAccelerationError || mService == null)
            return;
        mSwitchingView = true;
        mSwitchToPopup = true;
        exitOK();
    }


    private void initBrightnessTouch() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float brightnesstemp = lp.screenBrightness != -1f ? lp.screenBrightness : 0.6f;
        // Initialize the layoutParams screen brightness
        try {
            if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                if (!Permissions.canWriteSettings(this)) {
                    Permissions.checkWriteSettingsPermission(this, Permissions.PERMISSION_SYSTEM_BRIGHTNESS);
                    return;
                }
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                mRestoreAutoBrightness = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else if (brightnesstemp == 0.6f) {
                brightnesstemp = Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        lp.screenBrightness = brightnesstemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture) initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;

        // Set delta : 2f is arbitrary for now, it possibly will change in the future
        float delta = -y_changed / mSurfaceYDisplayRange;
    }


    private void setWindowBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        // Set Brightness
        getWindow().setAttributes(lp);
    }

    /**
     * handle changes of the seekbar (slicer)
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            showOverlayTimeout(OVERLAY_INFINITE);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            showOverlay(true);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!isFinishing() && fromUser && mService.isSeekable()) {
                seek(progress);
                mTime.setText(Strings.millisToString(progress));
                showInfo(Strings.millisToString(progress), 1000);
            }
        }
    };

    private interface TrackSelectedListener {
        boolean onTrackSelected(int trackID);
    }

    private void selectTrack(final MediaPlayer.TrackDescription[] tracks, int currentTrack, int titleId,
                             final TrackSelectedListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        if (tracks == null)
            return;
        final String[] nameList = new String[tracks.length];
        final int[] idList = new int[tracks.length];
        int i = 0;
        int listPosition = 0;
        for (MediaPlayer.TrackDescription track : tracks) {
            idList[i] = track.id;
            nameList[i] = track.name;
            // map the track position to the list position
            if (track.id == currentTrack)
                listPosition = i;
            i++;
        }

        if (!isFinishing()) {
            mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                    .setTitle(titleId)
                    .setSingleChoiceItems(nameList, listPosition, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int listPosition) {
                            int trackID = -1;
                            // Reverse map search...
                            for (MediaPlayer.TrackDescription track : tracks) {
                                if (idList[listPosition] == track.id) {
                                    trackID = track.id;
                                    break;
                                }
                            }
                            listener.onTrackSelected(trackID);
                            dialog.dismiss();
                        }
                    })
                    .create();
            mAlertDialog.setCanceledOnTouchOutside(true);
            mAlertDialog.setOwnerActivity(VideoPlayerActivity.this);
            mAlertDialog.show();
        }
    }


    private long getTime() {
        long time = mService.getTime();
        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mService.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime || time > mLastTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        }
        return mForcedTime == -1 ? time : mForcedTime;
    }

    private void seek(long position) {
        seek(position, mService.getLength());
    }

    private void seek(long position, long length) {
        mForcedTime = position;
        mLastTime = mService.getTime();
        mService.seek(position, length);
    }

    private void seekDelta(int delta) {
        // unseekable stream
        if (mService.getLength() <= 0 || !mService.isSeekable()) return;

        long position = getTime() + delta;
        if (position < 0) position = 0;
        seek(position);
        showInfo(Strings.millisToString(mService.getTime()) + "/" + Strings.millisToString(mService.getLength()), 1000);
    }

    /**
     * show overlay
     * @param forceCheck: adjust the timeout in function of playing state
     */
    private void showOverlay(boolean forceCheck) {
        if (forceCheck)
            mOverlayTimeout = 0;
        showOverlayTimeout(0);
    }

    /**
     * show overlay with the previous timeout value
     */
    private void showOverlay() {
        showOverlay(false);
    }

    /**
     * show overlay
     */
    private void showOverlayTimeout(int timeout) {
        if (mService == null)
            return;
        if (timeout != 0)
            mOverlayTimeout = timeout;
        if (mOverlayTimeout == 0)
            mOverlayTimeout = mService.isPlaying() ? OVERLAY_TIMEOUT : OVERLAY_INFINITE;
        if (mIsNavMenu) {
            mShowing = true;
            return;
        }
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (!mShowing) {
            mShowing = true;
            if (!mIsLocked) {
                mPlayPause.setVisibility(View.VISIBLE);
                if (mTracks != null)
                    mTracks.setVisibility(View.VISIBLE);
                if (mAdvOptions != null)
                    mAdvOptions.setVisibility(View.VISIBLE);
                mSize.setVisibility(View.VISIBLE);
                if (mRewind != null)
                    mRewind.setVisibility(View.VISIBLE);
                if (mForward != null)
                    mForward.setVisibility(View.VISIBLE);
                if (mPlaylistNext != null)
                    mPlaylistNext.setVisibility(View.VISIBLE);
                if (mPlaylistPrevious != null)
                    mPlaylistPrevious.setVisibility(View.VISIBLE);
            }
            dimStatusBar(false);
            mOverlayProgress.setVisibility(View.VISIBLE);
        }
        mHandler.removeMessages(FADE_OUT);
        if (mOverlayTimeout != OVERLAY_INFINITE)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), mOverlayTimeout);
        updateOverlayPausePlay();
        if (!(mObjectFocused == null)) {
            if (mObjectFocused.isFocusable())
                mObjectFocused.requestFocus();
            mObjectFocused = null;
        }
    }


    /**
     * hider overlay
     */
    private void hideOverlay(boolean fromUser) {
        if (mShowing) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.removeMessages(SHOW_PROGRESS);
            Log.i(TAG, "remove View!");
            mObjectFocused = getCurrentFocus();
            if (mOverlayTips != null) mOverlayTips.setVisibility(View.INVISIBLE);
            if (!fromUser && !mIsLocked) {
                mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mPlayPause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mTracks != null)
                    mTracks.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mAdvOptions != null)
                    mAdvOptions.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mRewind != null)
                    mRewind.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mForward != null)
                    mForward.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mPlaylistNext != null)
                    mPlaylistNext.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mPlaylistPrevious != null)
                    mPlaylistPrevious.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mSize.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            }

            mOverlayProgress.setVisibility(View.INVISIBLE);
            mPlayPause.setVisibility(View.INVISIBLE);
            if (mTracks != null)
                mTracks.setVisibility(View.INVISIBLE);
            if (mAdvOptions != null)
                mAdvOptions.setVisibility(View.INVISIBLE);
            if (mRewind != null)
                mRewind.setVisibility(View.INVISIBLE);
            if (mForward != null)
                mForward.setVisibility(View.INVISIBLE);
            if (mPlaylistNext != null)
                mPlaylistNext.setVisibility(View.INVISIBLE);
            if (mPlaylistPrevious != null)
                mPlaylistPrevious.setVisibility(View.INVISIBLE);
            mSize.setVisibility(View.INVISIBLE);
            mShowing = false;
            dimStatusBar(true);
        } else if (!fromUser) {
            /*
             * Try to hide the Nav Bar again.
             * It seems that you can't hide the Nav Bar if you previously
             * showed it in the last 1-2 seconds.
             */
            dimStatusBar(true);
        }
    }

    /**
     * Dim the status bar and/or navigation icons when needed on Android 3.x.
     * Hide it on Android 4.0 and later
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void dimStatusBar(boolean dim) {
        if (dim || mIsLocked)
            mActionBar.hide();
        else
            mActionBar.show();
        if (!AndroidUtil.isHoneycombOrLater() || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;

        if (AndroidUtil.isJellyBeanOrLater()) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (dim || mIsLocked) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater())
                navbar |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            else
                visibility |= View.STATUS_BAR_HIDDEN;
            if (!AndroidDevices.hasCombBar()) {
                navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (AndroidUtil.isKitKatOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                if (AndroidUtil.isJellyBeanOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
        } else {
            mActionBar.show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater())
                visibility |= View.SYSTEM_UI_FLAG_VISIBLE;
            else
                visibility |= View.STATUS_BAR_VISIBLE;
        }

        if (AndroidDevices.hasNavBar())
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    private void showTitle() {
        if (!AndroidUtil.isHoneycombOrLater() || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;
        mActionBar.show();

        if (AndroidUtil.isJellyBeanOrLater()) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (AndroidUtil.isICSOrLater())
            navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

        if (AndroidDevices.hasNavBar())
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);

    }

    private void updateOverlayPausePlay() {
        if (mService == null)
            return;
    }

    private void invalidateESTracks(int type) {
        switch (type) {
            case Media.Track.Type.Audio:
                mAudioTracksList = null;
                break;
            case Media.Track.Type.Text:
                mSubtitleTracksList = null;
                break;
        }
    }


    /**
     *
     */
    private void play() {
        mService.play();
        if (mRootView != null)
            mRootView.setKeepScreenOn(true);
    }

    /**
     *
     */
    private void pause() {
        mService.pause();
        if (mRootView != null)
            mRootView.setKeepScreenOn(false);
    }

    /*
     * Additionnal method to prevent alert dialog to pop up
     */
    @SuppressWarnings({"unchecked"})
    private void loadMedia(boolean fromStart) {
        mAskResume = false;
        getIntent().putExtra(PLAY_EXTRA_FROM_START, fromStart);
        loadMedia();
    }

    /**
     * External extras:
     * - position (long) - position of the video to start with (in ms)
     * - subtitles_location (String) - location of a subtitles file to load
     * - from_start (boolean) - Whether playback should start from start or from resume point
     * - title (String) - video title, will be guessed from file if not set.
     */
    @TargetApi(12)
    @SuppressWarnings({"unchecked"})
    private void loadMedia() {
        if (mService == null)
            return;
        mUri = null;
        mIsPlaying = false;
        boolean fromStart = false;
        String itemTitle = null;
        int positionInPlaylist = -1;
        long savedTime = -1; // position passed in by intent (ms)
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = getIntent().getExtras();
        /*
         * If the activity has been paused by pressing the power button, then
         * pressing it again will show the lock screen.
         * But onResume will also be called, even if vlc-android is still in
         * the background.
         * To workaround this, pause playback if the lockscreen is displayed.
         */
        final KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode())
            mWasPaused = true;
        if (mWasPaused)
            Log.d(TAG, "Video was previously paused, resuming in paused mode");

        if (intent.getData() != null)
            mUri = intent.getData();
        if (extras != null) {
            if (intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION))
                mUri = extras.getParcelable(PLAY_EXTRA_ITEM_LOCATION);
            fromStart = extras.getBoolean(PLAY_EXTRA_FROM_START, true);
            mAskResume &= !fromStart;
            positionInPlaylist = extras.getInt(PLAY_EXTRA_OPENED_POSITION, -1);
        }

        mUri = Uri.parse("http://default.andy.dev.qiqiuyun.cn:8071/video-player/examples/server/playlist.m3u8");
        if (intent.hasExtra(PLAY_EXTRA_SUBTITLES_LOCATION))
            mSubtitleSelectedFiles.add(extras.getString(PLAY_EXTRA_SUBTITLES_LOCATION));
        if (intent.hasExtra(PLAY_EXTRA_ITEM_TITLE))
            itemTitle = extras.getString(PLAY_EXTRA_ITEM_TITLE);


        if (mUri != null) {
            if (mService.hasMedia() && !mUri.equals(mService.getCurrentMediaWrapper().getUri()))
                mService.stop();
            // restore last position

            MediaWrapper media = null;
            // Start playback & seek
            mService.addCallback(this);
            /* prepare playback */
            boolean hasMedia = mService.hasMedia();
            if (hasMedia)
                media = mService.getCurrentMediaWrapper();
            else if (media == null)
                media = new MediaWrapper(mUri);
            if (mWasPaused)
                media.addFlags(MediaWrapper.MEDIA_PAUSED);
            if (mHardwareAccelerationError || intent.hasExtra(PLAY_DISABLE_HARDWARE))
                media.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
            media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
            media.addFlags(MediaWrapper.MEDIA_VIDEO);

            if (savedTime <= 0 && media != null && media.getTime() > 0l) {
                savedTime = media.getTime();
            }
            if (savedTime > 0L && !mService.isPlaying())
                mService.saveTimeToSeek(savedTime);

            // Handle playback
            if (!hasMedia)
                mService.load(media);
            else if (!mService.isPlaying())
                mService.playIndex(positionInPlaylist);
            else {
                onPlaying();
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createPresentation() {
        if (mMediaRouter == null || mEnableCloneMode)
            return;

        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);

        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

        if (presentationDisplay != null) {
            // Show a new presentation if possible.
            Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
        } else
            Log.i(TAG, "No secondary display detected");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void removePresentation() {
        if (mMediaRouter == null)
            return;

        // Dismiss the current presentation if the display has changed.
        Log.i(TAG, "Dismissing presentation because the current route no longer "
                + "has a presentation display.");
        mPresentationDisplayId = -1;
        stopPlayback();

        recreate();
    }


    /**
     * Start the video loading animation.
     */
    private void startLoading() {
        if (mIsLoading)
            return;
        mIsLoading = true;
        AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);

    }

    /**
     * Stop the video loading animation.
     */
    private void stopLoading() {
        mHandler.removeMessages(LOADING_ANIMATION);
        if (!mIsLoading)
            return;
        mIsLoading = false;
    }

    public void onClickOverlayTips(View v) {
        mOverlayTips.setVisibility(View.GONE);
    }

    public void onClickDismissTips(View v) {
        mOverlayTips.setVisibility(View.GONE);
        Editor editor = mSettings.edit();
        editor.putBoolean(PREF_TIPS_SHOWN, true);
        Util.commitPreferences(editor);
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    };

    public PlaybackServiceActivity.Helper getHelper() {
        return mHelper;
    }

    @Override
    public void onConnected(PlaybackService service) {
        mService = service;
        if (!mSwitchingView)
            mHandler.sendEmptyMessage(START_PLAYBACK);
        mSwitchingView = false;
    }

    @Override
    public void onDisconnected() {
        mService = null;
        mHandler.sendEmptyMessage(AUDIO_SERVICE_CONNECTION_FAILED);
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mSarNum = sarNum;
        mSarDen = sarDen;
    }

    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
        mSurfacesAttached = false;
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout vlcVout) {
        mHandler.sendEmptyMessage(HW_ERROR);
    }

    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), PLAY_FROM_SERVICE))
                onNewIntent(intent);
            else if (TextUtils.equals(intent.getAction(), EXIT_PLAYER))
                exitOK();
        }
    };
}
