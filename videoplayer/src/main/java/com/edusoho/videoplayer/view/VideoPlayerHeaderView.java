package com.edusoho.videoplayer.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.edusoho.videoplayer.R;

/**
 * Created by suju on 16/12/15.
 */

public class VideoPlayerHeaderView extends FrameLayout {

    private HeaderEventListener mHeaderEventListener;

    private TextView mNavigationView;
    private ViewGroup mMenuViewGroup;

    public VideoPlayerHeaderView(Context context) {
        super(context, null);
        initView();
    }

    public VideoPlayerHeaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
        initView();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public VideoPlayerHeaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        initView();
    }

    protected void initView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_controller_header_layout, this, true);
        mNavigationView = (TextView) findViewById(R.id.tv_header_navigation);
        mMenuViewGroup = (ViewGroup) findViewById(R.id.fl_header_menu);
        bindClickListener();
    }

    public void setHeaderEventListener(HeaderEventListener headerEventListener) {
        this.mHeaderEventListener = headerEventListener;
    }

    private void bindClickListener() {
        mNavigationView.setOnClickListener(getNavigationViewClickListener());
    }

    protected OnClickListener getNavigationViewClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHeaderEventListener != null) {
                    mHeaderEventListener.onBack();
                }
            }
        };
    }

    public void setTitle(String title) {
        mNavigationView.setText(title);
    }

    public void setNavigationIcon(int icon) {
        mNavigationView.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0);
    }

    public void setMenuLayout(View menuView) {
        mMenuViewGroup.removeAllViews();
        mMenuViewGroup.addView(menuView);
    }

    public interface HeaderEventListener {

        void onBack();
    }
}
