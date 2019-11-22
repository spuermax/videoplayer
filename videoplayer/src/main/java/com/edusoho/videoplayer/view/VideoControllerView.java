package com.edusoho.videoplayer.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.helper.ControllerViewTouchHelper;
import com.edusoho.videoplayer.media.M3U8Stream;
import com.edusoho.videoplayer.ui.adapter.OnItemClickListener;
import com.edusoho.videoplayer.ui.adapter.StreamListAdapter;
import com.edusoho.videoplayer.util.ControllerOptions;
import com.edusoho.videoplayer.util.Strings;
import com.edusoho.videoplayer.util.VLCOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by suju on 16/12/7.
 */

public class VideoControllerView extends FrameLayout {

    static String TAG = "VideoControllerView";

    private static final int FADE_OUT = 1;
    private static final int DEFAULT_TIMEOUT_COUNT = 5000;

    private ImageView mScreenChangeView;
    protected SeekBar mProgressView;
    protected TextView mTimeView;
    protected TextView mRateView;
    protected TextView mStreamListView;
    protected ImageView mPlayBtn;
    private PopupWindow streamListPopupWindow;
    private List<ControllerListener> mControllerListenerList;

    private Handler mHandler;
    private String mCurrentStreamName;
    private Map<String, M3U8Stream> mM3U8StreamList;
    private float[] defaultRateArray;
    private int mCurrentRateIndex;
    private int mOrientation;
    private int mSecProcess;

    private View mToolsView;
    private boolean isSeekByUser;
    private SeekChangeBarView mSeekChangeBarView;
    private ChangeBarView mAudioChangeBarView;
    private ControllerViewTouchHelper mControllerViewTouchHelper;
    private GestureDetectorCompat mGestureDetector;
    private ControllerOptions mControllerOptions;
    private boolean isCached;

    public VideoControllerView(Context context) {
        super(context, null);
        initView();
    }

    public VideoControllerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoControllerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initView();
    }

    public void setControllerOptions(ControllerOptions controllerOptions) {
        this.mControllerOptions = controllerOptions;
        renderViewByOptions();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (streamListPopupWindow != null) {
            streamListPopupWindow.dismiss();
            streamListPopupWindow = null;
        }
        mHandler.removeMessages(FADE_OUT);
    }

    public void setControllerViewTouchHelper(ControllerViewTouchHelper controllerViewTouchHelper) {
        controllerViewTouchHelper.setControllerView(this);
        this.mControllerViewTouchHelper = controllerViewTouchHelper;
    }

    public Map<String, M3U8Stream> getM3U8StreamList() {
        return mM3U8StreamList;
    }

    public void setM3U8StreamList(List<M3U8Stream> mM3U8StreamList) {
        Map<String, M3U8Stream> m3U8StreamMap = new LinkedHashMap<>();
        for (M3U8Stream m3U8Stream : mM3U8StreamList) {
            m3U8StreamMap.put(m3U8Stream.getName(), m3U8Stream);
        }
        this.mM3U8StreamList = m3U8StreamMap;
        if (mM3U8StreamList != null && !mM3U8StreamList.isEmpty()) {
            if (TextUtils.isEmpty(mCurrentStreamName)) {
                mCurrentStreamName = mM3U8StreamList.get(0).getName();
            }
            updateStreamListView(mCurrentStreamName);
        }
    }

    public void setSeekChangeBarView(SeekChangeBarView seekChangeBarView) {
        this.mSeekChangeBarView = seekChangeBarView;
    }

    public SeekChangeBarView getSeekChangeBarView() {
        return mSeekChangeBarView;
    }

    public ChangeBarView getAudioChangeBarView() {
        return mAudioChangeBarView;
    }

    public void setControllerListener(ControllerListener controllerListener) {
        this.mControllerListenerList.add(controllerListener);
    }

    public void clearControllerListener() {
        this.mControllerListenerList.clear();
    }

    @SuppressLint("HandlerLeak")
    protected void initView() {
        mOrientation = getContext().getResources().getConfiguration().orientation;
        setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
        LayoutInflater.from(getContext()).inflate(R.layout.content_video_controller_layout, this, true);
        mScreenChangeView = findViewById(R.id.iv_controller_screen);
        mPlayBtn = (ImageView) findViewById(R.id.tv_controller_play);
        mProgressView = (SeekBar) findViewById(R.id.sb_controller_progress);
        mTimeView = (TextView) findViewById(R.id.tv_controller_time);
        mStreamListView = (TextView) findViewById(R.id.tv_controller_streamlist);
        mRateView = (TextView) findViewById(R.id.tv_controller_rate);
        mToolsView = findViewById(R.id.ll_controller_tools);
        mSeekChangeBarView = findViewById(R.id.sbview_controller_seekinfo);
        mAudioChangeBarView = findViewById(R.id.change_view);

        mControllerListenerList = new LinkedList<>();
        setControllerOptions(ControllerOptions.getDefault());
        mPlayBtn.setSelected(false);
        bindControllerListener();
        //设置默认倍速
        setDefaultRateArray(new float[]{1.0f, 1.25f, 1.5f, 2.0f});
        updateRateView(defaultRateArray[mCurrentRateIndex]);

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case FADE_OUT:
                        hideOverlay();
                }
            }
        };

        mGestureDetector = new GestureDetectorCompat(getContext(), mGestureListener);
        mGestureDetector.setOnDoubleTapListener(getScreenDoubleTapListener());

        renderViewByOptions();

    }

    private void renderViewByOptions() {
        boolean isShowRate = mControllerOptions.getOption(ControllerOptions.RATE);// 修改 横竖屏都显示
        ((View) mRateView.getParent()).setVisibility(isShowRate ? VISIBLE : GONE);
        mScreenChangeView.setVisibility(mControllerOptions.getOption(ControllerOptions.SCREEN) ? VISIBLE : GONE);
        mProgressView.setEnabled(mControllerOptions.getOption(ControllerOptions.SEEK, true));


        ((View) mRateView.getParent()).setVisibility(mControllerOptions.getOption(ControllerOptions.RATE) ? VISIBLE : GONE);
        if (this.isCached) {
            ((View) mStreamListView.getParent()).setVisibility(View.VISIBLE);
            mStreamListView.setText("已缓存");
        } else {
            ((View) mStreamListView.getParent()).setVisibility(mM3U8StreamList != null && !mM3U8StreamList.isEmpty() ? VISIBLE : GONE);
        }
    }

    protected GestureDetector.OnDoubleTapListener getScreenDoubleTapListener() {
        return new GestureDetector.OnDoubleTapListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return false;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mPlayBtn.setSelected(!mPlayBtn.isSelected());
                for (ControllerListener listener : mControllerListenerList) {
                    listener.onPlayStatusChange(mPlayBtn.isSelected());
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return false;
            }
        };
    }

    public void showControllerBar(boolean show) {
        if (show) {
            showOverlay(DEFAULT_TIMEOUT_COUNT);
        } else {
            hideOverlayDelayed(DEFAULT_TIMEOUT_COUNT);
        }
    }

    private void showOverlay(int overlayTimeout) {
        mToolsView.setVisibility(VISIBLE);
//        if (getBackground() == null) {
//            setBackgroundResource(R.drawable.video_controller_view_bg);
//        }
//        getBackground().setAlpha(128);
        mHandler.removeMessages(FADE_OUT);
        if (overlayTimeout > 0) {
            hideOverlayDelayed(overlayTimeout);
        }
        for (ControllerListener listener : mControllerListenerList) {
            listener.onChangeOverlay(true);
        }
    }

    private void hideOverlayDelayed(int overlayTimeout) {
        mHandler.removeMessages(FADE_OUT);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), overlayTimeout);
    }

    private void hideOverlay() {
//        if (getBackground() == null) {
//            setBackgroundResource(R.drawable.video_controller_view_bg);
//        }
//        getBackground().setAlpha(0);
        hidePopWindows();
        mToolsView.setVisibility(INVISIBLE);
        mHandler.removeMessages(FADE_OUT);
        for (ControllerListener listener : mControllerListenerList) {
            listener.onChangeOverlay(false);
        }
    }

    protected void bindControllerListener() {
        mPlayBtn.setOnClickListener(getPlayClickListener());
        ((View) mRateView.getParent()).setOnClickListener(getRateClickListener());
        ((View) mStreamListView.getParent()).setOnClickListener(getStreamListClickListener());
        mScreenChangeView.setOnClickListener(getOnScreenChangeListener());
        mProgressView.setOnSeekBarChangeListener(getOnProgressChangeListener());
        mToolsView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                hideOverlayDelayed(DEFAULT_TIMEOUT_COUNT);
                return true;
            }
        });
    }

    public M3U8Stream getCurrentM3U8Stream() {
        if (TextUtils.isEmpty(mCurrentStreamName)
                || mM3U8StreamList == null
                || mM3U8StreamList.isEmpty()) {
            return null;
        }

        return mM3U8StreamList.get(mCurrentStreamName);
    }

    protected void updateStreamListView(String streamIndex) {
        if (TextUtils.isEmpty(streamIndex) || !mM3U8StreamList.containsKey(mCurrentStreamName)) {
            return;
        }
        ((View) mStreamListView.getParent()).setVisibility(mM3U8StreamList != null && !mM3U8StreamList.isEmpty() ? VISIBLE : GONE);
        mStreamListView.setText(mM3U8StreamList.get(mCurrentStreamName).getName());

        if (this.isCached) {
            ((View) mStreamListView.getParent()).setVisibility(View.VISIBLE);
            mStreamListView.setText("已缓存");
        } else {
            ((View) mStreamListView.getParent()).setVisibility(mM3U8StreamList != null && !mM3U8StreamList.isEmpty() ? VISIBLE : GONE);
        }
    }

    protected void updateRateView(float rate) {
        mRateView.setText(rate + "x");
    }

    protected void hidePopWindows() {
        if (streamListPopupWindow != null) {
            streamListPopupWindow.dismiss();
            streamListPopupWindow = null;
        }
    }

    protected void showPopupWindows() {
        if (streamListPopupWindow == null) {
            List<M3U8Stream> streamList = new ArrayList();
            for (Map.Entry<String, M3U8Stream> entry : mM3U8StreamList.entrySet()) {
                if (mCurrentStreamName.equals(entry.getKey())) {
                    continue;
                }
                streamList.add(entry.getValue());
            }
            streamListPopupWindow = initPopupWindows(streamList, ((View) mStreamListView.getParent()).getWidth(), mStreamListView.getHeight());
        }

        streamListPopupWindow.showAsDropDown((View) mStreamListView.getParent(), 0, 0);
    }

    protected PopupWindow initPopupWindows(final List<M3U8Stream> streamInfoLists, int width, int height) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.popup_stream, null);
        RecyclerView listView = view.findViewById(R.id.list_item);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listView.addItemDecoration(new ListDecoration(getContext(), ListDecoration.VERTICAL_LIST, R.drawable.stream_list_decoration));
        StreamListAdapter adapter = new StreamListAdapter(getContext(), streamInfoLists);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                M3U8Stream m3U8Stream = streamInfoLists.get(position);
                mCurrentStreamName = m3U8Stream.getName();
                updateStreamListView(mCurrentStreamName);
                for (ControllerListener listener : mControllerListenerList) {
                    listener.onChangePlaySource(m3U8Stream.getUrl());
                }
                hidePopWindows();
            }
        });
        listView.setAdapter(adapter);

        PopupWindow popupWindow = new PopupWindow(getContext());
        popupWindow.setWidth(width);
        int decoration = getContext().getResources().getDimensionPixelOffset(R.dimen.stream_list_decoration);
        popupWindow.setHeight((height + decoration) * streamInfoLists.size());
//        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setBackgroundDrawable(new ColorDrawable(getContext().getResources().getColor(R.color.video_item_bg)));
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.setContentView(view);

        return popupWindow;
    }

    public void setDefaultRateArray(float[] defaultRateArray) {
        this.defaultRateArray = defaultRateArray;
    }

    protected OnClickListener getRateClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentRateIndex = mCurrentRateIndex + 1;
                if (mCurrentRateIndex >= defaultRateArray.length) {
                    mCurrentRateIndex = 0;
                }
                updateRateView(defaultRateArray[mCurrentRateIndex]);
                for (ControllerListener listener : mControllerListenerList) {
                    listener.onChangeRate(defaultRateArray[mCurrentRateIndex]);
                }
            }
        };
    }

    protected OnClickListener getStreamListClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (VideoControllerView.this.isCached) {
                    return;
                }
                for (ControllerListener listener : mControllerListenerList) {
                    showPopupWindows();
                }
            }
        };
    }

    protected OnClickListener getPlayClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlayBtn.setSelected(!mPlayBtn.isSelected());
                for (ControllerListener listener : mControllerListenerList) {
                    listener.onPlayStatusChange(mPlayBtn.isSelected());
                }
            }
        };
    }

//    protected CompoundButton.OnCheckedChangeListener getOnScreenChangeListener() {
//        return new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                for (ControllerListener listener : mControllerListenerList) {
//                    listener.onChangeScreen(isChecked ?
//                            Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT);
//                }
//            }
//        };
//    }

    protected OnClickListener getOnScreenChangeListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                for (ControllerListener listener : mControllerListenerList) {
                    if (!mScreenChangeView.isSelected()) {
                        mScreenChangeView.setSelected(true);
                        listener.onChangeScreen(Configuration.ORIENTATION_LANDSCAPE);
                    } else {
                        mScreenChangeView.setSelected(false);
                        listener.onChangeScreen(Configuration.ORIENTATION_PORTRAIT);
                    }
                }
            }
        };
    }

    protected SeekBar.OnSeekBarChangeListener getOnProgressChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                isSeekByUser = fromUser;
                updateTime(progress, seekBar.getMax());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekByUser = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekByUser = false;
                for (ControllerListener listener : mControllerListenerList) {
                    listener.onSeek(seekBar.getProgress());
                }
            }
        };
    }

    //横竖屏 切换
    public void updateControllerConfiguration(int orientation) {
        mOrientation = orientation;
        mScreenChangeView.setSelected(orientation == Configuration.ORIENTATION_LANDSCAPE);
        mScreenChangeView.setVisibility(orientation == Configuration.ORIENTATION_LANDSCAPE ? GONE : VISIBLE);
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            ((View) mRateView.getParent()).setVisibility(GONE);
//            ((View) mStreamListView.getParent()).setVisibility(GONE);
            ((View) mRateView.getParent()).setVisibility(VISIBLE);
            ((View) mStreamListView.getParent()).setVisibility(VISIBLE);
            mScreenChangeView.setVisibility(mControllerOptions.getOption(ControllerOptions.SCREEN) ? VISIBLE : GONE);
            return;
        }
        ((View) mRateView.getParent()).setVisibility(mControllerOptions.getOption(ControllerOptions.RATE) ? VISIBLE : GONE);
        if (this.isCached) {
            ((View) mStreamListView.getParent()).setVisibility(View.VISIBLE);
            mStreamListView.setText("已缓存");
        } else {
            ((View) mStreamListView.getParent()).setVisibility(mM3U8StreamList != null && !mM3U8StreamList.isEmpty() ? VISIBLE : GONE);
        }
    }

    public void setScreenChangeVisible(int visible) {
        mScreenChangeView.setVisibility(visible);
    }

    public void setCached(boolean cached) {
        this.isCached = cached;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("vc:", "onTouchEvent:" + event.getAction());
        if (mGestureDetector != null && mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        if (mControllerViewTouchHelper == null) {
            onTouchEventBySelf(event);
            return true;
        }
        mControllerViewTouchHelper.updateTimeLength(mProgressView.getProgress(), mProgressView.getMax());
        if (mControllerViewTouchHelper.onTouchEvent(event)) {
            return true;
        }
        onTouchEventBySelf(event);
        return true;
    }

    private void onTouchEventBySelf(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mToolsView.getVisibility() == VISIBLE) {
                hideOverlay();
            } else {
                showOverlay(DEFAULT_TIMEOUT_COUNT);
            }
        }
    }

    public void onSeek(int position) {
        for (ControllerListener listener : mControllerListenerList) {
            listener.onSeek(position);
        }
    }

    public void updatePlayStatus(boolean isPlay) {
        mPlayBtn.setSelected(isPlay);
    }

    public void updateMediaBufferState(float process) {
        mSecProcess = (int) (process / 100) * VLCOptions.NETWORK_CACHE / 1000;
        mProgressView.setSecondaryProgress(mProgressView.getProgress() + mSecProcess);
    }

    public void updateMediaProgress(int position, int duration) {
        if (isSeekByUser) {
            return;
        }
        mProgressView.setMax(duration);
        mProgressView.setProgress(position);
        mProgressView.setSecondaryProgress(position + mSecProcess);
        updateTime(position, duration);
        for (ControllerListener listener : mControllerListenerList) {
            listener.onPosition(position, duration);
        }
    }

    private void updateTime(int position, int duration) {
        mTimeView.setText(String.format("%s/%s", Strings.millisToString(position), Strings.millisToString(duration)));
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

    public interface ControllerListener {

        void onSeek(int position);

        void onChangeScreen(int orientation);

        void onPlayStatusChange(boolean isPlay);

        void onChangeRate(float rate);

        void onChangePlaySource(String url);

        void onChangeOverlay(boolean isShow);

        void onPosition(int position, int duration);
    }
}
