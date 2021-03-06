package com.brufino.terpsychore.fragments.session;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.MusicPickerActivity;
import com.brufino.terpsychore.activities.QueueManager;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class QueueFragment extends Fragment {

    private static final int REQUEST_SELECT_TRACKS = 1;

    private RecyclerView vTrackList;
    private ViewGroup vTopBar;
    private ImageView vControlAdd;
    private ImageView vControlRefresh;
    private ImageView vControlFullscreen;

    private List<JsonObject> mTrackList = new ArrayList<>();
    private int mPlayedTracks = 0;
    private int mCurrentTracks = 0;
    private int mUpcomingTracks = 0;
    private QueueManager mQueueManager;
    private TrackListAdapter mTrackListAdapter;
    private LinearLayoutManager mLayoutManager;

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
        mLayoutManager = new LinearLayoutManager(getContext());
        vTrackList.setLayoutManager(mLayoutManager);
        mTrackListAdapter = new TrackListAdapter(mTrackList);
        vTrackList.setAdapter(mTrackListAdapter);
        vControlAdd = (ImageView) getView().findViewById(R.id.queue_control_add);
        vControlAdd.setOnClickListener(mOnControlAddClickListener);
        vControlRefresh = (ImageView) getView().findViewById(R.id.queue_control_refresh);
        vControlRefresh.setOnClickListener(mOnControlRefreshClickListener);
        vControlFullscreen = (ImageView) getView().findViewById(R.id.queue_control_fullscreen);
        vControlFullscreen.setOnClickListener(mOnControlFullscreenClickListener);
    }

    public void setQueueManager(QueueManager queueManager) {
        mQueueManager = queueManager;
        mQueueManager.addQueueListener(mQueueListener);
    }

    private View.OnClickListener mOnControlAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mQueueManager.isHost()) {
                return;
            }
            Intent musicPicker = new Intent(getContext(), MusicPickerActivity.class);
            startActivityForResult(musicPicker, REQUEST_SELECT_TRACKS);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_TRACKS:
                if (resultCode == Activity.RESULT_OK) {
                    ArrayList<String> trackUris = data.getStringArrayListExtra(MusicPickerActivity.RESULT_TRACK_URIS);
                    mQueueManager.addTracks(trackUris);
                } else {
                    //Toast.makeText(getContext(), "No tracks selected", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private View.OnClickListener mOnControlRefreshClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mQueueManager.refreshQueue(true);
        }
    };

    private View.OnClickListener mOnControlFullscreenClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getContext(), "TODO: Implement!", Toast.LENGTH_SHORT).show();
        }
    };

    private QueueManager.QueueListener mQueueListener = new QueueManager.QueueListener() {
        @Override
        public void onQueueChange(QueueManager queueManager, JsonObject queue) {
            mTrackList.clear();
            JsonArray tracksJson = queue.get("tracks").getAsJsonArray();
            List<JsonObject> tracks = CoreUtils.jsonArrayToJsonObjectList(tracksJson);
            mTrackList.addAll(tracks);
            mTrackListAdapter.notifyDataSetChanged();
            vControlAdd.setVisibility(mQueueManager.isHost() ? View.VISIBLE : View.GONE);

            mPlayedTracks = mCurrentTracks = mUpcomingTracks = 0;
            for (JsonObject track : mTrackList) {
                if (track.get("played_track").getAsBoolean()) {
                    mPlayedTracks++;
                } else if (track.get("current_track").getAsBoolean()) {
                    mCurrentTracks++;
                } else {
                    mUpcomingTracks++;
                }
            }
        }
        @Override
        public void onQueueRefreshError(QueueManager queueManager, Throwable t) {
            Toast.makeText(getContext(), "Error refreshing queue, try again later", Toast.LENGTH_SHORT).show();
        }
    };

    public void onVisible() {
        mLayoutManager.scrollToPositionWithOffset(mPlayedTracks + mCurrentTracks, 0);
    }

    public int getVerticalOffset() {
        int trackItemHeight = getResources().getDimensionPixelSize(R.dimen.queue_track_item_height);
        int topBarHeight = getResources().getDimensionPixelSize(R.dimen.queue_top_bar_height);
        return -(trackItemHeight + topBarHeight);
    }

    public int getDesiredHeight(int maxHeight) {
        int heightIfEnoughItems = getDesiredHeightIfEnoughItems(maxHeight);
        int actualItemsHeight = heightForItems(Math.max(3, mTrackList.size()));
        return Math.min(actualItemsHeight, heightIfEnoughItems);
    }

    private int getDesiredHeightIfEnoughItems(int maxHeight) {
        int h;

        h = maxHeight - heightOfItems(2);
        if (h >= heightForItems(4)) return h;

        h = heightForItems(4);
        if (maxHeight - heightOfItems(1) >= h) return h;

        h = maxHeight - heightOfItems(1);
        if (h >= heightForItems(3)) return h;

        h = heightForItems(3);
        if (maxHeight >= h) return h;

        return maxHeight;
    }

    private int heightForItems(int numberOfItems) {
        int trackItemHeight = getResources().getDimensionPixelSize(R.dimen.queue_track_item_height);
        int trackItemMargin = getResources().getDimensionPixelOffset(R.dimen.queue_track_item_margin);
        int topBarHeight = getResources().getDimensionPixelSize(R.dimen.queue_top_bar_height);
        return numberOfItems * (trackItemHeight + trackItemMargin) + topBarHeight;
    }

    private int heightOfItems(int numberOfItems) {
        int trackItemHeight = getResources().getDimensionPixelSize(R.dimen.queue_track_item_height);
        int trackItemMargin = getResources().getDimensionPixelOffset(R.dimen.queue_track_item_margin);
        return numberOfItems * (trackItemHeight + trackItemMargin);
    }

    /* TODO: Use a non-static inner class and call everything from activity, just like UserPickerActivity */
    public static class TrackListAdapter extends RecyclerView.Adapter<TrackItemViewHolder> {

        private final List<JsonObject> mBackingList;

        public TrackListAdapter(List<JsonObject> backingList) {
            mBackingList = backingList;
        }

        @Override
        public TrackItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_track, parent, false);
            return new TrackItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(TrackItemViewHolder holder, int position) {
            JsonObject item = mBackingList.get(position);
            //Log.v("VFY", position + " = " + item.get("name").getAsString());
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
        private final ImageView vStatusIcon;
        private final ImageView vRemoveIcon;

        public TrackItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vTrackItem = (RelativeLayout) itemView.findViewById(R.id.queue_track_item);
            vTrackName = (TextView) itemView.findViewById(R.id.queue_track_name);
            vTrackArtist = (TextView) itemView.findViewById(R.id.queue_track_artist);
            vStatusIcon = (ImageView) itemView.findViewById(R.id.queue_track_item_status_icon);
            vRemoveIcon = (ImageView) itemView.findViewById(R.id.queue_track_item_remove);
        }

        public void bind(JsonObject item) {
            vTrackName.setText(item.get("name").getAsString());
            vTrackArtist.setText(item.get("artist").getAsString());
            boolean played = item.get("played_track").getAsBoolean();
            boolean current = item.get("current_track").getAsBoolean();
            boolean next = item.get("next_track").getAsBoolean();

            vStatusIcon.setVisibility(View.GONE);
            vRemoveIcon.setVisibility(View.VISIBLE);
            int trackColor = ContextCompat.getColor(mContext, R.color.queueTrackItemBg);
            vTrackItem.setBackgroundColor(trackColor);
            int foregroundText = ContextCompat.getColor(mContext, R.color.queueTrackItemText);
            vTrackName.setTextColor(foregroundText);
            int secondaryText = ContextCompat.getColor(mContext, R.color.queueTrackItemTextSecondary);
            vTrackArtist.setTextColor(secondaryText);
            if (played) {
                vStatusIcon.setImageResource(R.drawable.ic_history_white_24dp);
                vStatusIcon.setColorFilter(ContextCompat.getColor(mContext, R.color.queueTrackPlayedIconTint));
                vStatusIcon.setVisibility(View.VISIBLE);
                vRemoveIcon.setVisibility(View.GONE);
                int playedForeground = ContextCompat.getColor(mContext, R.color.queueTrackPlayedText);
                vTrackName.setTextColor(playedForeground);
                int playedSecondary = ContextCompat.getColor(mContext, R.color.queueTrackPlayedTextSecondary);
                vTrackArtist.setTextColor(playedSecondary);
            }
            if (current) {
                vStatusIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                vStatusIcon.setColorFilter(null);
                vStatusIcon.setVisibility(View.VISIBLE);
                vRemoveIcon.setVisibility(View.GONE);
                int currentColor = ContextCompat.getColor(mContext, R.color.queueTrackCurrentItemBg);
                vTrackItem.setBackgroundColor(currentColor);
            }
            if (next) {
                int nextColor = ContextCompat.getColor(mContext, R.color.queueTrackNextItemBg);
                vTrackItem.setBackgroundColor(nextColor);
            }
        }
    }
}
