package suju.org.videodemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;

import com.edusoho.videoplayer.media.IVideoPlayer;
import com.edusoho.videoplayer.media.VideoPlayerFactory;
import com.edusoho.videoplayer.ui.VideoPlayerFragment;

/**
 * @Author yinzh
 * @Date 2020/11/24 20:09
 * @Description
 */
public class TestVideoActivity extends AppCompatActivity {

    private ViewGroup mPlayContainer;
    private IVideoPlayer mVideoPlayer;
    private int mMediaCoder;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_video);
        mPlayContainer = findViewById(com.edusoho.videoplayer.R.id.fl_player_contaner);
        if (isFinishing()) {
            return;
        }

    }

    private void createVideoPlayer(int mediaPlayerType, VideoPlayerGetCallback videoPlayerGetCallback) {
        if (mVideoPlayer != null) {
            mPlayContainer.removeView(mVideoPlayer.getView());
            mVideoPlayer.onStop();
            mVideoPlayer = null;
        }
        mVideoPlayer = getVideoPlayer(mediaPlayerType);
        mVideoPlayer.setDigestKey("");
        mPlayContainer.addView(mVideoPlayer.getView());
    }

    interface VideoPlayerGetCallback {

        void success();
    }


    private IVideoPlayer getVideoPlayer(int mediaPlayerType) {
        if (mVideoPlayer != null) {
            return mVideoPlayer;
        }
        synchronized (this) {
            if (mVideoPlayer == null) {
                mVideoPlayer = VideoPlayerFactory.getInstance().createPlayer(this, mMediaCoder, mediaPlayerType);
            }
        }

        return mVideoPlayer;
    }

}
