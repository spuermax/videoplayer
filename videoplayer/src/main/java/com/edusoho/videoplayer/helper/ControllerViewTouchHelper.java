package com.edusoho.videoplayer.helper;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.edusoho.videoplayer.view.VideoControllerView;
import com.edusoho.videoplayer.util.AndroidDevices;
import com.edusoho.videoplayer.util.Permissions;

import java.lang.ref.WeakReference;

/**
 * Created by suju on 16/12/14.
 */

public class ControllerViewTouchHelper {

    //Volume
    private int mAudioMax;
    private int mVolSave;
    private float mVol;
    private boolean mMute = false;
    private AudioManager mAudioManager;
    private WeakReference<Activity> mActivityRef;

    //stick event
    private static final int JOYSTICK_INPUT_DELAY = 300;
    private long mLastMove;

    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction = TOUCH_NONE;
    private int mSurfaceYDisplayRange;
    private float mInitTouchY, mTouchY = -1f, mTouchX = -1f;
    // Brightness
    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;
    private int mTouchControls = 0;
    private GestureDetectorCompat mDetector = null;
    private VideoControllerView mControllerView;

    private int mVideoWidth;
    private int mVideoHeight;
    private long mTime;
    private long mLength;

    public ControllerViewTouchHelper(Activity activity) {
        mAudioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        this.mActivityRef = new WeakReference(activity);
    }

    public void setControllerView(VideoControllerView controllerView) {
        this.mControllerView = controllerView;
    }

    public void updateTimeLength(long time, long length) {
        this.mTime = time;
        this.mLength = length;
    }

    public void updateVideoSize(int w, int h) {
        this.mVideoWidth = w;
        this.mVideoHeight = h;
    }

    /**
     * 修改 进度
     */
    private void doSeekTouch(int coef, float gesturesize, boolean seek) {
        if (coef == 0)
            coef = 1;
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (Math.abs(gesturesize) < 1)
            return;

        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK) {
            return;
        }

        mTouchAction = TOUCH_SEEK;
        // Size of the jump, 10 minutes max (600000), with a bi-cubic progression, for a 8cm gesture
        int jump = (int) ((Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000)) / coef);

        // Adjust the jump
        if ((jump > 0) && ((mTime + jump) > mLength))
            jump = (int) (mLength - mTime);
        if ((jump < 0) && ((mTime + jump) < 0))
            jump = (int) -mTime;

        //Jump !
        if (seek && mLength > 0) {
            mControllerView.onSeek((int) (mTime + jump));
        }
        mControllerView.getSeekChangeBarView().showInfo(coef, jump, mTime, mLength);
    }

    /**
     * 判断是否 自动调节亮度
     */
    private void initBrightnessTouch() {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
        float brightnesstemp = lp.screenBrightness != -1f ? lp.screenBrightness : 0.6f;
        /*try {
            if (Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                if (!Permissions.canWriteSettings(activity)) {
                    Permissions.checkWriteSettingsPermission(activity, Permissions.PERMISSION_SYSTEM_BRIGHTNESS);
                    return;
                }
                Settings.System.putInt(activity.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                mRestoreAutoBrightness = android.provider.Settings.System.getInt(activity.getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else if (brightnesstemp == 0.6f) {
                brightnesstemp = android.provider.Settings.System.getInt(activity.getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }*/
        lp.screenBrightness = brightnesstemp;
        activity.getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;

    }

    /**
     * 修改 屏幕亮度
     */
    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS) {
            return;
        }
        if (mIsFirstBrightnessGesture) {
            initBrightnessTouch();
        }
        mTouchAction = TOUCH_BRIGHTNESS;

        float delta = -y_changed * 2 / mSurfaceYDisplayRange;
        changeBrightness(delta);
    }

    private void changeBrightness(float delta) {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = activity.getWindow().getAttributes();

        lp.screenBrightness = Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1f);
        activity.getWindow().setAttributes(lp);

        float sb = lp.screenBrightness;

        mControllerView.getAudioChangeBarView().showInfo(Math.round(sb * 100), false);
    }

    /**
     * 修改 音量
     */
    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME) {
            return;
        }
        mTouchAction = TOUCH_VOLUME;
        float delta = -((y_changed * 2 / mSurfaceYDisplayRange) * mAudioMax);
        mVol += delta;
        int vol = (int) Math.min(Math.max(mVol, 0), mAudioMax);
        if (delta != 0f) {
            setAudioVolume(vol);
        }
    }

    private void setAudioVolume(int vol) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);

        int newVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mControllerView.getAudioChangeBarView().showInfo(newVol, true);
    }

    private void sendMouseEvent(int action, int button, int x, int y) {
        /*IPlayerServcie service = mVLCPlayServiceRef.get();
        if (service == null) {
            return;
        }
        IVLCVout vout = service.getVLCVout();
        if (vout != null) {
            vout.sendMouseEvent(action, button, x, y);
        }*/
    }

    /*public boolean dispatchGenericMotionEvent(MotionEvent event) {
        Log.d("touchHelper", "dispatchGenericMotionEvent");
        //Check for a joystick event
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) !=
                InputDevice.SOURCE_JOYSTICK ||
                event.getAction() != MotionEvent.ACTION_MOVE)
            return false;

        InputDevice mInputDevice = event.getDevice();

        float dpadx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float dpady = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if (mInputDevice == null || Math.abs(dpadx) == 1.0f || Math.abs(dpady) == 1.0f) {
            return false;
        }

        float x = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X);
        float y = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y);
        float rz = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_RZ);

        if (System.currentTimeMillis() - mLastMove > JOYSTICK_INPUT_DELAY) {
            if (Math.abs(x) > 0.3) {
                seekDelta(x > 0.0f ? 10000 : -10000);
            } else if (Math.abs(y) > 0.3) {
                if (mIsFirstBrightnessGesture) {
                    initBrightnessTouch();
                }

                changeBrightness(-y / 10f);
            } else if (Math.abs(rz) > 0.3) {
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int delta = -(int) ((rz / 7) * mAudioMax);
                int vol = (int) Math.min(Math.max(mVol + delta, 0), mAudioMax);
                setAudioVolume(vol);
            }
            mLastMove = System.currentTimeMillis();
        }
        return true;
    }*/

    public boolean onTouchEvent(MotionEvent event) {
        DisplayMetrics screen = new DisplayMetrics();
        mActivityRef.get().getWindowManager().getDefaultDisplay().getMetrics(screen);

        if (mSurfaceYDisplayRange == 0) {
            mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);
        }

        float x_changed, y_changed;
        if (mTouchX != -1f && mTouchY != -1f) {
            y_changed = event.getRawY() - mTouchY;
            x_changed = event.getRawX() - mTouchX;
        } else {
            x_changed = 0f;
            y_changed = 0f;
        }

        // coef is the gradient's move to determine a neutral zone
        float coef = Math.abs(y_changed / x_changed);
        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);
        float delta_y = Math.max(1f, (Math.abs(mInitTouchY - event.getRawY()) / screen.xdpi + 0.5f) * 2f);

        /* Offset for Mouse Events */
        int[] offset = new int[2];
        mControllerView.getLocationOnScreen(offset);
        int xTouch = Math.round((event.getRawX() - offset[0]) * mVideoWidth / mControllerView.getWidth());
        int yTouch = Math.round((event.getRawY() - offset[1]) * mVideoHeight / mControllerView.getHeight());

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = mInitTouchY = event.getRawY();
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_DOWN, 0, xTouch, yTouch);
                break;

            case MotionEvent.ACTION_MOVE:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_MOVE, 0, xTouch, yTouch);

                Log.d("touchHelper", "mTouchY:" + mTouchY);
                // No volume/brightness action if coef < 2 or a secondary display is connected
                if (mTouchAction != TOUCH_SEEK && coef > 2) {
                    if (Math.abs(y_changed / mSurfaceYDisplayRange) < 0.05) {
                        return false;
                    }

                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    // Volume (Up or Down - Right side)
                    if (mTouchControls == 1 || (int) mTouchX > (3 * screen.widthPixels / 5)) {
                        doVolumeTouch(y_changed);
                    }
                    // Brightness (Up or Down - Left side)
                    else if ((int) mTouchX < (2 * screen.widthPixels / 5)) {
                        Log.d("touchHelper", "doBrightnessTouch");
                        doBrightnessTouch(y_changed);
                    }
                } else {
                    // Seek (Right or Left move)
                    Log.d("touchHelper", "doSeekTouch");
                    doSeekTouch(Math.round(delta_y), xgesturesize, false);
                }
                break;

            case MotionEvent.ACTION_UP:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);
                if (mTouchAction == TOUCH_NONE) {
                    return false;
                }
                // Seek
                if (mTouchAction == TOUCH_SEEK) {
                    doSeekTouch(Math.round(delta_y), xgesturesize, true);
                }
                mTouchX = -1f;
                mTouchY = -1f;
                break;
        }
        return mTouchAction != TOUCH_NONE;
    }
}
