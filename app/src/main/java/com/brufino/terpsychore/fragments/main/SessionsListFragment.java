package com.brufino.terpsychore.fragments.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.SessionActivity;
import com.brufino.terpsychore.fragments.EditSessionFragment;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.messaging.FirebaseMessagingServiceImpl;
import com.brufino.terpsychore.messaging.LocalMessagesManager;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.brufino.terpsychore.util.FontUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

public class SessionsListFragment extends MainFragment {

    private static Set<String> SUPPORTED_MESSAGE_TYPES = Sets.newHashSet("chat_message", "session_message");
    private static final long REFRESH_LIMIT_TO_INVALIDATE_SESSION_IMAGES_IN_MS = 500;

    private RecyclerView vSessionList;
    private ProgressBar vLoading;

    private long mLastRefreshRequest = System.currentTimeMillis();
    private volatile boolean mInvalidateSessionImages = false;
    private List<JsonObject> mSessionList = new ArrayList<>();
    private SessionListAdapter mSessionListAdapter;
    private LinearLayoutManager mSessionListLayoutManager;
    private boolean mLoading = false;
    private SessionApi mSessionApi;
    private String mUserId;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sessions_list, container, false);
    }

    @Override
    public FloatingActionButton onCreateFab(LayoutInflater inflater, ViewGroup parent) {
        return (FloatingActionButton) inflater.inflate(R.layout.layout_fab_sessions_list, parent, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        vSessionList = (RecyclerView) getView().findViewById(R.id.session_list);
        mSessionListAdapter = new SessionListAdapter();
        mSessionListLayoutManager = new LinearLayoutManager(getContext());
        vSessionList.setAdapter(mSessionListAdapter);
        vSessionList.setLayoutManager(mSessionListLayoutManager);
        vLoading = (ProgressBar) getView().findViewById(R.id.sessions_list_loading);

        mSessionApi = ApiUtils.createApi(getContext(), SessionApi.class);
        mUserId = ActivityUtils.getUserId(getContext());
        checkNotNull(mUserId, "mUserId shouldn't be null at SessionsListFragment.onActivityCreated()");

        setHasOptionsMenu(true);
        loadSessions(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadSessions(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("Sessions");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(FirebaseMessagingServiceImpl.MESSAGE_RECEIVED));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_MESSAGE_TYPE);
            int sessionId = intent.getIntExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_SESSION_ID, -1);
            if (SUPPORTED_MESSAGE_TYPES.contains(type)) {
                // TODO: Only reload the session with session   id
                loadSessions(false);
            }
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_sessions_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                /* TODO: Remove this double refresh when GCM is working */
                long now = System.currentTimeMillis();
                if (now - mLastRefreshRequest < REFRESH_LIMIT_TO_INVALIDATE_SESSION_IMAGES_IN_MS) {
                    Log.d("VFY", "Session images invalidation marked");
                    mInvalidateSessionImages = true;
                }
                mLastRefreshRequest = now;
                loadSessions(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadSessions(boolean showLoading) {
        if (mLoading) {
            return;
        }
        if (showLoading) {
            vLoading.setVisibility(View.VISIBLE);
        }
        mLoading = true;
        mSessionApi.getSessions(mUserId).enqueue(mGetSessionsCallback);
    }

    private Callback<JsonArray> mGetSessionsCallback = new ApiCallback<JsonArray>() {
        @Override
        public void onSuccess(Call<JsonArray> call, Response<JsonArray> response) {
            mLoading = false;
            vLoading.setVisibility(View.GONE);
            JsonArray sessions = response.body().getAsJsonArray();
            Log.v("VFY", "Sessions loaded: size = " + sessions.size());
            mSessionList.clear();
            mSessionList.addAll(CoreUtils.jsonArrayToJsonObjectList(sessions));
            if (mInvalidateSessionImages) {
                for (JsonObject session : mSessionList) {
                    String imageUrl = ApiUtils.getServerUrl(getContext(), session.get("image_url").getAsString());
                    ViewUtils.refreshImageInCaches(getContext(), imageUrl);
                }
                mInvalidateSessionImages = false;
            }
            mSessionListAdapter.notifyDataSetChanged();
        }
        @Override
        public void onFailure(Call<JsonArray> call, Throwable t) {
            mLoading = false;
            vLoading.setVisibility(View.GONE);
            Log.e("VFY", "Error retrieving sessions list", t);
            Toast.makeText(getContext(), "Error retrieving sessions list", Toast.LENGTH_SHORT).show();
        }
    };

    private EditSessionFragment.OnSessionEditedListener mOnCreateNewSession =
            new EditSessionFragment.OnSessionEditedListener() {
        @Override
        public void onComplete(boolean success, String sessionName) {
            if (success && !sessionName.trim().isEmpty()) {
                JsonObject body = new JsonObject();
                JsonObject session = new JsonObject();
                session.addProperty("name", sessionName);
                body.add("session", session);

                mSessionApi.postSession(mUserId, body).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        loadSessions(true);
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.d("VFY", "Error creating session", t);
                        Toast.makeText(getContext(), "Error creating session", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    @Override
    protected void onFabClick(FloatingActionButton fab) {
        EditSessionFragment dialog = new EditSessionFragment();
        dialog.setOnSessionEditedListener(mOnCreateNewSession);
        dialog.show(getFragmentManager(), EditSessionFragment.class.getSimpleName());
    }

    public class SessionListAdapter extends RecyclerView.Adapter<SessionItemHolder> {

        @Override
        public SessionItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_list, parent, false);
            return new SessionItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SessionItemHolder holder, int position) {
            JsonObject item = mSessionList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return mSessionList.size();
        }
    }

    private static class SessionItemHolder extends RecyclerView.ViewHolder {

        private final TextView vTitle;
        private final TextView vDescription;
        private final ImageView vImage;
        private final ImageView vPlayingImage;

        private final Context mContext;
        private int mSessionId;

        public SessionItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vTitle = (TextView) itemView.findViewById(R.id.item_session_title);
            vDescription = (TextView) itemView.findViewById(R.id.item_session_description);
            vImage = (ImageView) itemView.findViewById(R.id.item_session_image);
            vPlayingImage = (ImageView) itemView.findViewById(R.id.item_session_playing_image);
            itemView.setOnClickListener(mOnClickListener);
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SessionActivity.class);
                intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, mSessionId);
                mContext.startActivity(intent);
            }
        };

        public void bind(JsonObject session) {
            mSessionId = session.get("id").getAsInt();
            String name = session.get("name").getAsString();
            JsonObject lastMessage = CoreUtils.getAsJsonObjectOrNull(session.get("last_message"));
            JsonObject queue = session.get("queue").getAsJsonObject();
            int tracksTotal = session.get("ntracks_total").getAsInt();
            int tracksRemaining = session.get("ntracks_remaining").getAsInt();
            String status = queue.get("track_status").getAsString();
            String imageUrl = ApiUtils.getServerUrl(mContext, session.get("image_url").getAsString());
            CharSequence description =  (lastMessage == null)
                    ? mContext.getString(R.string.no_messages_session_description)
                    : LocalMessagesManager.getInstance().getDescriptionForMessage(lastMessage);

            Picasso.with(mContext)
                    .load(imageUrl)
                    .into(vImage);
            FontUtils.setTextWithEmojis(vTitle, name);
            FontUtils.setTextWithEmojis(vDescription, description);

            vPlayingImage.setVisibility(View.GONE);
            if (tracksRemaining > 0) {
                vPlayingImage.setVisibility(View.VISIBLE);
                if (status.equals("playing")) {
                    vPlayingImage.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                } else {
                    vPlayingImage.setImageResource(R.drawable.ic_pause_white_24dp);
                }
            }
        }
    }
}
