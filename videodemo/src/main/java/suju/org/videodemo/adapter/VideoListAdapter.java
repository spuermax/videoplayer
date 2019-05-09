package suju.org.videodemo.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import suju.org.videodemo.R;
import suju.org.videodemo.data.entity.VideoEntity;

/**
 * Created by suju on 17/2/6.
 */

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.ViewHolder> {

    private List<VideoEntity> mList;
    private OnItemClickListener mOnItemClickListener;

    public VideoListAdapter() {
        mList = new ArrayList<>();
    }

    public void addDataList(List<VideoEntity> list) {
        this.mList.addAll(list);
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.render(mList.get(position));
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mOnItemClickListener.onClick(mList.get(holder.getAdapterPosition()));
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_videolist_layout, null));
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView mCoverView;
        private TextView mTitleView;

        public ViewHolder(View view) {
            super(view);
            mCoverView = (ImageView) view.findViewById(R.id.iv_videolist_cover);
            mTitleView = (TextView) view.findViewById(R.id.tv_videolist_title);
        }

        public void render(VideoEntity videoEntity) {
            ImageLoader.getInstance().displayImage(videoEntity.getPicUrl(), mCoverView);
            mTitleView.setText(videoEntity.getTitle());
        }
    }

    public interface OnItemClickListener {
        void onClick(VideoEntity videoEntity);
    }
}
