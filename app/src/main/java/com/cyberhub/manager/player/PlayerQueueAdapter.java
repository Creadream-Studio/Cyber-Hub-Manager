package com.cyberhub.manager.player;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import com.cyberhub.manager.R;

/** 播放队列适配器：原仓库基于 RecyclerView，本项目无 androidx，改为 ListView/BaseAdapter。 */
public class PlayerQueueAdapter extends BaseAdapter {

    private final List<MediaItem> items;
    private final int currentIndex;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int index);
        void onRemoveClick(int index);
    }

    public PlayerQueueAdapter(List<MediaItem> items, int currentIndex, OnItemClickListener listener) {
        this.items = items;
        this.currentIndex = currentIndex;
        this.listener = listener;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public MediaItem getItem(int position) { return items.get(position); }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        if (v == null) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
            holder = new ViewHolder(v);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
        }
        MediaItem item = getItem(position);
        holder.indexView.setText(String.valueOf(position + 1));
        holder.titleView.setText(item.title != null ? item.title : "未知媒体");
        holder.artistView.setText(item.artist != null ? item.artist : "");
        long dur = item.duration;
        holder.durationView.setText(dur > 0 ? String.format("%d:%02d", dur / 60000, (dur / 1000) % 60) : "--:--");
        v.setAlpha(position == currentIndex ? 1.0f : 0.6f);
        holder.btnRemove.setOnClickListener(v2 -> {
            if (listener != null) listener.onRemoveClick(position);
        });
        v.setOnClickListener(v2 -> {
            if (listener != null) listener.onItemClick(position);
        });
        return v;
    }

    static class ViewHolder {
        final TextView indexView, titleView, artistView, durationView;
        final ImageButton btnRemove;
        ViewHolder(View v) {
            indexView = v.findViewById(R.id.indexView);
            titleView = v.findViewById(R.id.titleView);
            artistView = v.findViewById(R.id.artistView);
            durationView = v.findViewById(R.id.durationView);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }
}
