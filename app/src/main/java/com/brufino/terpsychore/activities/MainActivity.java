package com.brufino.terpsychore.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.EditSessionFragment;
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

import static com.google.common.base.Preconditions.*;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 0;
    private RecyclerView vSessionsList;
    private SessionListAdapter mSessionsAdapter;
    private FloatingActionButton mAddSessionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vSessionsList = (RecyclerView) findViewById(R.id.sessions_list);
        mSessionsAdapter = new SessionListAdapter();
        vSessionsList.setAdapter(mSessionsAdapter);
        mAddSessionButton = (FloatingActionButton) findViewById(R.id.add_session_button);
        mAddSessionButton.setOnClickListener(mOnAddSessionButtonClick);

        String userId = ActivityUtils.getUserId(this);
        if (userId != null) {
            loadSessions();
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

    private void loadSessions() {
        String userId = checkNotNull(ActivityUtils.getUserId(this));
        SessionApi sessionApi = ApiUtils.createApi(SessionApi.class);
        sessionApi.getSessions(userId).enqueue(mGetSessionsCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case LOGIN_REQUEST_CODE:
                Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
                loadSessions();
        }
    }

    /* TODO: Handle failure */
    private Callback<JsonArray> mGetSessionsCallback = new Callback<JsonArray>() {
        @Override
        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
            JsonArray sessions = response.body().getAsJsonArray();
            Log.v("VFY", "sessions size = " + sessions.size());
            //new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            //    @Override
            //    public void run() {
            //        Intent intent = new Intent(MainActivity.this, SessionActivity.class);
            //        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, "12");
            //        intent.putExtra(SessionActivity.TRACK_ID_EXTRA_KEY, "spotify:track:3Gaj5GBeZ8aynvtPkxrr9A");
            //        intent.putExtra(SessionActivity.TRACK_NAME_EXTRA_KEY, "Paradise");
            //        intent.putExtra(SessionActivity.TRACK_ARTIST_EXTRA_KEY, "Tiësto");
            //        startActivity(intent);
            //    }
            //}, 1000);
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
            JsonObject item = mBackingList.get(position);
            holder.bind("Title", "Subtitle");
        }

        @Override
        public int getItemCount() {
            return mBackingList.size();
        }
    }

    private static class SessionItemHolder extends RecyclerView.ViewHolder {

        private final Context mContext;
        private final TextView vNameText;
        private final TextView vDescriptionText;

        public SessionItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vNameText = (TextView) itemView.findViewById(R.id.session_list_item_name);
            vDescriptionText = (TextView) itemView.findViewById(R.id.session_list_item_description);
            itemView.setOnClickListener(mOnClickListener);
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "Foo", Toast.LENGTH_SHORT).show();
            }
        };

        public void bind(String name, String description) {
            vNameText.setText(name);
            vDescriptionText.setText(description);
        }

    }
}
