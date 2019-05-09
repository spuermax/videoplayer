package suju.org.videodemo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import suju.org.videodemo.R;
import suju.org.videodemo.adapter.VideoListAdapter;
import suju.org.videodemo.data.ResultCallback;
import suju.org.videodemo.data.entity.NetEaseVideoListEntity;
import suju.org.videodemo.data.entity.VideoEntity;
import suju.org.videodemo.data.impl.NetEaseVideoModel;

/**
 * Created by suju on 17/2/6.
 */

public class VideoListFragment extends Fragment implements VideoListAdapter.OnItemClickListener {

    public static final String SEARCH = "search";
    private RecyclerView mRecyclerView;
    private VideoListAdapter mAdapter;
    private String search;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_videolist_layout, null);
        ViewGroup parent = (ViewGroup) contentView.getParent();
        if (parent != null) {
            parent.removeView(contentView);
        }

        return contentView;
    }

    public static VideoListFragment newInstance(String search) {
        VideoListFragment fragment = new VideoListFragment();
        Bundle args = new Bundle();
        args.putString(SEARCH, search);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.listview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new VideoListAdapter();
        mAdapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(mAdapter);

        new NetEaseVideoModel().searchVideos(getArguments().getString(SEARCH), new ResultCallback() {
            @Override
            public void onResponse(String result) {
                mUIUpdateHandler.obtainMessage(0, result).sendToTarget();
            }
        });
    }

    @Override
    public void onClick(VideoEntity videoEntity) {
        Intent intent = new Intent(getContext(), VideoInfoActivity.class);
        intent.putExtra(VideoInfoActivity.VIDEO_ID, videoEntity.getPlid());
        startActivity(intent);
    }

    private Handler mUIUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            NetEaseVideoListEntity listEntity = new Gson().fromJson(msg.obj.toString(), NetEaseVideoListEntity.class);
            if (listEntity != null && listEntity.getData() != null) {
                mAdapter.addDataList(listEntity.getData());
            }
        }
    };
}
