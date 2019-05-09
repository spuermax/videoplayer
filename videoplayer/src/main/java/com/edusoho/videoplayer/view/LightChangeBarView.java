package com.edusoho.videoplayer.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.edusoho.videoplayer.R;

/**
 * Created by suju on 16/12/15.
 */

public class LightChangeBarView extends FrameLayout {

    private View mVerticalBarProgress;

    public LightChangeBarView(Context context) {
        super(context, null);
        initView();
    }

    public LightChangeBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LightChangeBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initView();
    }

    protected void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_light_change_bar_layout, this, true);
        mVerticalBarProgress = findViewById(R.id.verticalbar_progress);
    }


    /**
     * Show text in the info view and vertical progress bar for "duration" milliseconds
     * @param text
     * @param duration
     * @param barNewValue new volume/brightness value (range: 0 - 15)
     */
    public void showInfoWithVerticalBar(String text, int duration, int barNewValue) {
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mVerticalBarProgress.getLayoutParams();
        layoutParams.weight = barNewValue;
        mVerticalBarProgress.setLayoutParams(layoutParams);
        setVisibility(View.VISIBLE);
    }
}
