package com.brufino.terpsychore.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.EditSessionFragment;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 0;
    private RecyclerView vSessionsList;
    private SessionListAdapter mSessionsAdapter;
    private FloatingActionButton mAddSessionButton;
    private SessionApi mSessionApi;
    private String mUserId;
    private LinearLayoutManager mLinearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.mainActivityStatusBarColor));
        }

        vSessionsList = (RecyclerView) findViewById(R.id.sessions_list);
        mSessionsAdapter = new SessionListAdapter();
        mLinearLayoutManager = new LinearLayoutManager(this);
        vSessionsList.setLayoutManager(mLinearLayoutManager);
        vSessionsList.setAdapter(mSessionsAdapter);
        mAddSessionButton = (FloatingActionButton) findViewById(R.id.add_session_button);
        mAddSessionButton.setOnClickListener(mOnAddSessionButtonClick);

        mSessionApi = ApiUtils.createApi(SessionApi.class);
        mUserId = ActivityUtils.getUserId(this);
        if (mUserId != null) {
            mSessionApi.getSessions(mUserId).enqueue(mGetSessionsCallback);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
        }
    }

    private EditSessionFragment.OnSessionEditedListener mOnCreateNewSession =
            new EditSessionFragment.OnSessionEditedListener() {
        @Override
        public void onComplete(boolean success, String sessionName) {
            if (success && !sessionName.trim().isEmpty()) {
                // Construct post body
                JsonObject body = new JsonObject();
                JsonObject session = new JsonObject();
                session.addProperty("name", sessionName);
                body.add("session", session);

                // Post session
                mSessionApi.postSession(mUserId, body).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(Call<String> call, Response<String> response) {
                        // Update sessions
                        mSessionApi.getSessions(mUserId).enqueue(mGetSessionsCallback);
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        throw Throwables.propagate(t);
                    }
                });

                Toast.makeText(MainActivity.this, "Session " + sessionName + " created!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Canceled!", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private View.OnClickListener mOnAddSessionButtonClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            EditSessionFragment dialog = new EditSessionFragment();
            dialog.setOnSessionEditedListener(mOnCreateNewSession);
            dialog.show(getSupportFragmentManager(), EditSessionFragment.class.getSimpleName());
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case LOGIN_REQUEST_CODE:
                Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
                mUserId = ActivityUtils.getUserId(this);
                mSessionApi.getSessions(mUserId).enqueue(mGetSessionsCallback);
        }
    }

    /* TODO: Handle failure */
    private Callback<JsonArray> mGetSessionsCallback = new Callback<JsonArray>() {
        @Override
        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
            JsonArray sessions = response.body().getAsJsonArray();
            Log.v("VFY", "sessions size = " + sessions.size());
            ImmutableList.Builder<JsonObject> builder = new ImmutableList.Builder<>();
            for (JsonElement session : sessions) {
                builder.add(session.getAsJsonObject());
            }
            mSessionsAdapter.setBackingList(builder.build());
        }
        @Override
        public void onFailure(Call<JsonArray> call, Throwable t) {
            Log.e("VFY", "Failure while retrieving sessions list", t);
        }
    };

    public static class SessionListAdapter extends RecyclerView.Adapter<SessionItemHolder> {

        private List<JsonObject> mBackingList = new ArrayList<>();

        public void setBackingList(List<JsonObject> backingList) {
            mBackingList = backingList;
            notifyDataSetChanged();
        }

        @Override
        public SessionItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.session_list_item, parent, false);
            return new SessionItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SessionItemHolder holder, int position) {
            JsonObject session = mBackingList.get(position);
            holder.bind(session);
        }

        @Override
        public int getItemCount() {
            return mBackingList.size();
        }
    }

    private static class SessionItemHolder extends RecyclerView.ViewHolder {

        private int mSessionId;
        private final Context mContext;
        private final TextView vNameText;
        private final TextView vDescriptionText;
        private final TextView vNowPlayingText;
        private final TextView vNextPlayingText;
        private final ImageView vPlayingIcon;

        public SessionItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vNameText = (TextView) itemView.findViewById(R.id.session_list_item_name);
            vDescriptionText = (TextView) itemView.findViewById(R.id.session_list_item_description);
            vNowPlayingText = (TextView) itemView.findViewById(R.id.session_list_item_playing_now);
            vNextPlayingText = (TextView) itemView.findViewById(R.id.session_list_item_playing_next);
            vPlayingIcon = (ImageView) itemView.findViewById(R.id.session_list_item_playing_icon);
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
            String description = session.get("nusers").getAsInt() + " Connected";
            JsonObject queue = session.get("queue_digest").getAsJsonObject();
            JsonElement currentTrack = queue.get("current_track");
            JsonElement nextTrack = queue.get("next_track");

            vNameText.setText(name);
            vDescriptionText.setText(description);

            vPlayingIcon.setVisibility(View.GONE);
            if (currentTrack.isJsonNull()) {
                vNowPlayingText.setVisibility(View.GONE);
            } else {
                vPlayingIcon.setVisibility(View.VISIBLE);
                vNowPlayingText.setVisibility(View.VISIBLE);
                vNowPlayingText.setText(currentTrack.getAsJsonObject().get("name").getAsString());
            }
            if (nextTrack.isJsonNull()) {
                vNextPlayingText.setVisibility(View.GONE);
            } else {
                vPlayingIcon.setVisibility(View.VISIBLE);
                vNextPlayingText.setVisibility(View.VISIBLE);
                vNextPlayingText.setText("> " + nextTrack.getAsJsonObject().get("name").getAsString());
            }



        }

    }
}
