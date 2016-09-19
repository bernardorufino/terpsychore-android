package com.brufino.terpsychore.fragments;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class QueueFragment extends Fragment {

    private List<JsonObject> mTrackList = new ArrayList<>();
    private RecyclerView vTrackList;
    private ViewGroup vTopBar;
    private ImageView vControlAdd;
    private ImageView vControlRefresh;
    private ImageView vControlFullscreen;
    private SessionApi mSessionApi;
    private int mSessionId;
    private TrackListAdapter mTrackListAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_queue, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSessionApi = ApiUtils.createApi(SessionApi.class);

        vTopBar = (ViewGroup) getView().findViewById(R.id.queue_top_bar);
        vTrackList = (RecyclerView) getView().findViewById(R.id.queue_track_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        vTrackList.setLayoutManager(layoutManager);
        mTrackListAdapter = new TrackListAdapter(mTrackList);
        vTrackList.setAdapter(mTrackListAdapter);
        vControlAdd = (ImageView) getView().findViewById(R.id.queue_control_add);
        vControlAdd.setOnClickListener(mOnControlAddClickListener);
        vControlRefresh = (ImageView) getView().findViewById(R.id.queue_control_refresh);
        vControlRefresh.setOnClickListener(mOnControlRefreshClickListener);
        vControlFullscreen = (ImageView) getView().findViewById(R.id.queue_control_fullscreen);
        vControlFullscreen.setOnClickListener(mOnControlFullscreenClickListener);
    }

    private View.OnClickListener mOnControlAddClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    private View.OnClickListener mOnControlRefreshClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = ActivityUtils.getUserId(getContext());
            mSessionApi.getQueue(userId, mSessionId).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    mTrackListAdapter.notifyDataSetChanged();
                    bind(mSessionId, response.body());
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("VFY", "Error while refreshing queue", t);
                    Toast.makeText(getContext(), "Error, try again later", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private View.OnClickListener mOnControlFullscreenClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    public void bind(int sessionId, JsonObject queue) {
        mSessionId = sessionId;
        mTrackList.clear();
        JsonArray tracks = queue.get("tracks").getAsJsonArray();
        for (int i = 0; i < tracks.size(); i++) {
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
            int trackColor = ContextCompat.getColor(mContext, R.color.queueTrackItemBackground);
            vTrackItem.setBackgroundColor(trackColor);
            int foregroundText = ContextCompat.getColor(mContext, R.color.queueTrackItemForegroundText);
            vTrackName.setTextColor(foregroundText);
            int secondaryText = ContextCompat.getColor(mContext, R.color.queueTrackItemSecondaryText);
            vTrackArtist.setTextColor(secondaryText);
            if (played) {
                vStatusIcon.setImageResource(R.drawable.ic_history_white_24dp);
                ColorStateList colorList = ActivityUtils.getColorList(mContext, R.color.queueTrackPlayedIconTint);
                vStatusIcon.setImageTintList(colorList);
                vStatusIcon.setVisibility(View.VISIBLE);
                vRemoveIcon.setVisibility(View.GONE);
                int playedForeground = ContextCompat.getColor(mContext, R.color.queueTrackPlayedForegroundText);
                vTrackName.setTextColor(playedForeground);
                int playedSecondary = ContextCompat.getColor(mContext, R.color.queueTrackPlayedSecondaryText);
                vTrackArtist.setTextColor(playedSecondary);
            }
            if (current) {
                vStatusIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                vStatusIcon.setImageTintList(null);
                vStatusIcon.setVisibility(View.VISIBLE);
                vRemoveIcon.setVisibility(View.GONE);
                int currentColor = ContextCompat.getColor(mContext, R.color.queueTrackCurrentItemBackground);
                vTrackItem.setBackgroundColor(currentColor);
            }
            if (next) {
                int nextColor = ContextCompat.getColor(mContext, R.color.queueTrackNextItemBackground);
                vTrackItem.setBackgroundColor(nextColor);
            }
        }
    }
}
