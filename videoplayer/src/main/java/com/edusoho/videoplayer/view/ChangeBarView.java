package com.edusoho.videoplayer.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.edusoho.videoplayer.R;

/**
 * Created by suju on 16/12/15.
 */

public class ChangeBarView extends FrameLayout {

    private static final int FADE_OUT_INFO = 0;

    private ImageView mAudioIcon;
    private TextView mAudioInfoView;
    private Handler mHandler;

    public ChangeBarView(Context context) {
        super(context, null);
        initView();
    }

    public ChangeBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChangeBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initView();
    }

    protected void initBackground() {
        Drawable background = getResources().getDrawable(R.drawable.controller_play_seek_bg);
        background.setAlpha(128);
        setBackground(background);
    }

    @SuppressLint("HandlerLeak")
    protected void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_audio_change_bar_layout, this, true);
        mAudioIcon = findViewById(R.id.iv_icon);
        mAudioInfoView = findViewById(R.id.tv_info);

        initBackground();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case FADE_OUT_INFO:
                        hideInfo();
                }
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeMessages(FADE_OUT_INFO);
    }

    public void hideInfo() {
        startAnimation(AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out));
        setVisibility(View.GONE);
    }

    public void showInfo(int jump, boolean isAudio) {
        setVisibility(View.VISIBLE);
        mAudioInfoView.setText(jump + "%");
        if (isAudio) {
            mAudioIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_controller_audio));
        } else {
            mAudioIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_controller_brightness));
        }
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, 1000);
    }
}
