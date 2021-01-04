package suju.org.videodemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.edusoho.videoplayer.media.listener.SimpleVideoControllerListener;
import com.edusoho.videoplayer.ui.VideoPlayerFragment;
import com.edusoho.videoplayer.util.VLCOptions;
import com.edusoho.videoplayer.view.EduExoPlayerView;
import com.edusoho.videoplayer.view.VideoControllerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.videolan.libvlc.util.AndroidUtil;

import suju.org.videodemo.util.MessageEvent;

/**
 * @Author yinzh
 * @Date 2020/11/23 16:08
 * @Description
 */
public class VideoTestFragment extends VideoPlayerFragment {
    private static String COVER_URL = "cover_url";
    private static String VIDEO_LENGTH = "video_length";
    private static String VIDEO_URL = "video_url";


    private View mAudioCover;
    private ImageView mCoverImageView;

    private String mCoverUrl;
    private String mVideoUrl;
    private String mVideoLength;

    private FrameLayout frameLayout;

    public static VideoTestFragment newInstance(String coverUrl, String videoUrl, String videoLength) {
        Bundle args = new Bundle();
        Log.i("AAAAAAA", "视频地址" + videoUrl);

        VideoTestFragment fragment = new VideoTestFragment();
        args.putString(VIDEO_LENGTH, videoLength);
        args.putString(COVER_URL, coverUrl);
        args.putString(VIDEO_URL, videoUrl);
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mCoverUrl = bundle.getString(COVER_URL);
        mVideoLength = bundle.getString(VIDEO_LENGTH);
        mVideoUrl = bundle.getString(VIDEO_URL);
        setAudioCover(mAudioCover);

        Log.i("AAAAAAA", "视频地址" + mVideoUrl);


        //事件监听
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup mPlayContainer = view.findViewById(R.id.fl_player_contaner);
        mPlayContainer.bringToFront();
        EduExoPlayerView exoPlayerView = (EduExoPlayerView) mPlayContainer.getChildAt(0);
        exoPlayerView.bringToFront();
        Log.i("AAAAAAA", "视频 Fragment 状态 getUserVisibleHint" + getUserVisibleHint());
//        if(getUserVisibleHint()){
            loadPlay();
//        }

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.i("AAAAAAA", "视频 Fragment 状态 onHiddenChanged" + hidden);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlay();
        Log.i("AAAAAAA", "视频 Fragment 状态 = onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        videoStop();
        Log.i("AAAAAAA", "视频 Fragment 状态 = onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("AAAAAAA", "视频 Fragment 状态 = onStop");

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }

        Log.i("AAAAAAA", "视频 Fragment 状态 = onDestroyView");

    }

    @Override
    protected void changeScreenLayout(int orientation) {
        super.changeScreenLayout(orientation);
    }

    @Override
    protected VideoControllerView.ControllerListener getDefaultControllerListener() {
        return new SimpleVideoControllerListener() {
            @Override
            public void onSeek(int position) {
                super.onSeek(position);
            }

            @Override
            public void onPlayStatusChange(boolean isPlay) {
                super.onPlayStatusChange(isPlay);

                loadPlay();
            }

            @Override
            public void onChangeOverlay(boolean isShow) {
                super.onChangeOverlay(isShow);
            }

            @Override
            public void onChangePlaySource(String url) {
                super.onChangePlaySource(url);
            }

            @Override
            public void onChangeRate(float rate) {
                super.onChangeRate(rate);
            }

            @Override
            public void onChangeScreen(int orientation) {
                super.onChangeScreen(orientation);
                Log.i("AAAAAAAAAAA", "屏幕方向 1" + orientation);
                changeScreenLayout(orientation);

//                if (getOnScreenOrientation() != null) { //回调视频横竖屏
//                    getOnScreenOrientation().screenOrientation(orientation);
//                }
            }
        };
    }


    private void loadPlay() {
        if (!TextUtils.isEmpty(mVideoUrl)) {
            setCached(false);
            playUri();
        } else {
        }
    }

    private void playUri() {
        String uri = mVideoUrl;
        String mPlayM3u8Url = filterJsonFormat(uri);
        playVideo(mPlayM3u8Url, null);
    }

    private String filterJsonFormat(String url) {
        if (url != null && url.contains("?")) {
            String[] urls = url.split("\\?");
            if (urls.length > 1) {
                return urls[0];
            }
        }
        return url;
    }


    @Subscribe
    public void onReceiveMessage(MessageEvent messageEvent) {
        switch (messageEvent.getType()) {
            case MessageEvent.EXAM_NEXT_QUESTION_VIDEO:
                Log.i("AAAAA", "EXAM_NEXT_QUESTION + 下一题关闭视频");
//                stop();
//                videoStop();
//                FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
//                Fragment fragmentById = getFragmentManager().findFragmentById(R.id.video_container);
//                if (fragmentById == null) {
//                    Log.i("AAAAA", "EXAM_NEXT_QUESTION + 下一题关闭视频  | fragmentById == null");
//                    return;
//                }
//                fragmentTransaction.remove(fragmentById).commitAllowingStateLoss();
                break;
            case MessageEvent.EXAM_CARD_JUMP:
                Log.i("AAAAA", "EXAM_NEXT_QUESTION + 跳题关闭视频");
                stop();
                break;
        }
    }
}
