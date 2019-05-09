package com.edusoho.videoplayer.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.edusoho.videoplayer.R;
import com.edusoho.videoplayer.media.M3U8Stream;

import java.util.List;

/**
 * Created by suju on 16/12/13.
 */

public class StreamListAdapter extends RecyclerView.Adapter<StreamListAdapter.ViewHolder> {

    private List<M3U8Stream> m3U8Streams;
    private Context mContext;
    private OnItemClickListener mOnItemClickListener;

    public StreamListAdapter(Context context, List<M3U8Stream> m3U8Streams) {
        this.mContext = context;
        this.m3U8Streams = m3U8Streams;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.setTitle(m3U8Streams.get(position).getName());

        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(createTitleView());
    }

    private TextView createTitleView() {
        TextView textView = new TextView(mContext);
        textView.setTextColor(mContext.getResources().getColor(R.color.textIcons));
        int padding = mContext.getResources().getDimensionPixelSize(R.dimen.caption);
        textView.setWidth(mContext.getResources().getDimensionPixelSize(R.dimen.stream_btn_w));
        textView.setHeight(mContext.getResources().getDimensionPixelSize(R.dimen.stream_btn_h));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, padding);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundResource(R.drawable.white_shape_rectangle_bg);
        return textView;
    }

    @Override
    public int getItemCount() {
        return m3U8Streams.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitleView;

        public ViewHolder(View view) {
            super(view);
            mTitleView = (TextView) view;
        }

        public void setTitle(String title) {
            mTitleView.setText(title);
        }
    }
}
