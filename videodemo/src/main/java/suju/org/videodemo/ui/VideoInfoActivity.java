package suju.org.videodemo.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.edusoho.videoplayer.ui.VideoPlayerFragment;
import com.google.gson.Gson;

import java.util.List;

import suju.org.videodemo.R;
import suju.org.videodemo.data.ResultCallback;
import suju.org.videodemo.data.entity.NetEaseVideoInfoEntity;
import suju.org.videodemo.data.entity.VideoEntity;
import suju.org.videodemo.data.impl.NetEaseVideoModel;

/**
 * Created by suju on 17/2/7.
 */

public class VideoInfoActivity extends AppCompatActivity {

    public static final String VIDEO_ID = "videoId";
    public static final String VIDEO_TITLE = "videoTitle";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoinfo);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(getIntent().getStringExtra(VIDEO_TITLE));
        String videoId = getIntent().getStringExtra(VIDEO_ID);
        new NetEaseVideoModel().getVideoInfo(videoId, new ResultCallback() {
            @Override
            public void onResponse(String result) {
                mUIUpdateHandler.obtainMessage(0, result).sendToTarget();
            }
        });
    }

    private Handler mUIUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            findViewById(R.id.pb_video_load).setVisibility(View.GONE);
            NetEaseVideoInfoEntity listEntity = new Gson().fromJson(msg.obj.toString(), NetEaseVideoInfoEntity.class);
            if (listEntity != null && listEntity.getData() != null) {
                NetEaseVideoInfoEntity.DataEntity dataEntity = listEntity.getData();
                List<VideoEntity> videoEntityList = dataEntity.getVideoList();
                if (videoEntityList != null && !videoEntityList.isEmpty()) {
                    loadVideoPlayer(videoEntityList.get(0).getM3u8SdUrl());
                }
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void loadVideoPlayer(String mediaUrl) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        VideoPlayerFragment fragment = new VideoPlayerFragment();
        getIntent().putExtra(VideoPlayerFragment.PLAY_URI, mediaUrl);
        fragment.setArguments(getIntent().getExtras());
        transaction.add(R.id.fl_video_container, fragment);

        transaction.commit();
    }
}
