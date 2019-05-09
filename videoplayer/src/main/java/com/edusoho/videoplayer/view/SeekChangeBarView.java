package com.edusoho.videoplayer.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.util.Strings;

/**
 * Created by suju on 16/12/15.
 */

public class SeekChangeBarView extends FrameLayout {

    private static final int FADE_OUT_INFO = 0;

    private TextView mSeekInfoView;
    private ImageView mSeekIconView;
    private Handler mHandler;

    public SeekChangeBarView(Context context) {
        super(context, null);
        initView();
    }

    public SeekChangeBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SeekChangeBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initView();
    }

    protected void initBackground() {
        Drawable background = getResources().getDrawable(R.drawable.controller_play_seek_bg);
        background.setAlpha(128);
        setBackground(background);
    }

    protected void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_seek_change_bar_layout, this, true);
        mSeekInfoView = (TextView) findViewById(R.id.tv_seek_info);
        mSeekIconView = (ImageView) findViewById(R.id.tv_seek_icon);

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

    private void hideInfo() {
        startAnimation(AnimationUtils.loadAnimation(
                getContext(), android.R.anim.fade_out));
        setVisibility(View.INVISIBLE);
    }

    public void showInfo(int coef, int jump, long time, long length) {
        updateSeekIcon(jump < 0);
        mSeekInfoView.setText(getCoverSeekColorText(
                Strings.millisToString(jump + time), "/" + Strings.millisToString(length), getResources().getColor(R.color.seek_secondary_text)));
        setVisibility(View.VISIBLE);

        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, 1000);
    }

    public static SpannableString getCoverSeekColorText(String text, String newStr, int color) {
        StringBuffer stringBuffer = new StringBuffer(text);
        int start = stringBuffer.length();
        stringBuffer.append(newStr);
        SpannableString spannableString = new SpannableString(stringBuffer);
        spannableString.setSpan(
                new ForegroundColorSpan(color), start, stringBuffer.length(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        return spannableString;
    }

    private void updateSeekIcon(boolean isBack) {
        mSeekIconView.setImageResource(isBack ? R.drawable.ic_controller_back : R.drawable.ic_controller_forward);
    }
}
