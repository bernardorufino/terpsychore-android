package com.brufino.terpsychore.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.brufino.terpsychore.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class QueueFragment extends Fragment {

    private RecyclerView vTrackList;
    private List<JsonObject> mTrackList = new ArrayList<>();
    private ViewGroup vTopBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        vTopBar = (ViewGroup) getView().findViewById(R.id.queue_top_bar);
        vTrackList = (RecyclerView) getView().findViewById(R.id.queue_track_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        vTrackList.setLayoutManager(layoutManager);
        TrackListAdapter adapter = new TrackListAdapter(mTrackList);
        vTrackList.setAdapter(adapter);
    }

    public void bind(JsonObject queue) {
        mTrackList.clear();
        JsonArray tracks = queue.get("tracks").getAsJsonArray();
        for (int i = 1; i < tracks.size(); i++) {
            mTrackList.add(tracks.get(i).getAsJsonObject());
        }
    }

    public int getTopBarPlusTrackItemHeight() {
        int trackItemHeight = getResources().getDimensionPixelSize(R.dimen.queue_track_item_height);
        int topBarHeight = getResources().getDimensionPixelSize(R.dimen.queue_top_bar_height);
        return trackItemHeight + topBarHeight;
    }

    public static class TrackListAdapter extends RecyclerView.Adapter<TrackItemViewHolder> {

        private final List<JsonObject> mBackingList;

        public TrackListAdapter(List<JsonObject> backingList) {
            mBackingList = backingList;
        }

        @Override
        public TrackItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.queue_track_item, parent, false);
            return new TrackItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(TrackItemViewHolder holder, int position) {
            JsonObject item = mBackingList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mBackingList.size();
        }
    }

    private static class TrackItemViewHolder extends RecyclerView.ViewHolder {

        private final Context mContext;
        private final RelativeLayout vTrackItem;
        private final TextView vTrackName;
        private final TextView vTrackArtist;

        public TrackItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vTrackItem = (RelativeLayout) itemView.findViewById(R.id.queue_track_item);
            vTrackName = (TextView) itemView.findViewById(R.id.queue_track_name);
            vTrackArtist = (TextView) itemView.findViewById(R.id.queue_track_artist);
        }

        public void bind(JsonObject item) {
            vTrackName.setText(item.get("name").getAsString());
            vTrackArtist.setText(item.get("artist").getAsString());
            boolean current = item.get("current_track").getAsBoolean();
            boolean next = item.get("next_track").getAsBoolean();
            if (current || next) {
                vTrackName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                vTrackArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
                if (current) {
                    int currentColor = ContextCompat.getColor(mContext, R.color.queueTrackCurrentItemBackground);
                    vTrackItem.setBackgroundColor(currentColor);
                } else if (next) {
                    int nextColor = ContextCompat.getColor(mContext, R.color.queueTrackNextItemBackground);
                    vTrackItem.setBackgroundColor(nextColor);
                }
            }
        }
    }
}
