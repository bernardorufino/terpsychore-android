package com.brufino.terpsychore.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.EditSessionFragment;
import com.brufino.terpsychore.lib.CircleTransformation;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int LOGIN_REQUEST_CODE = 0;

    private RecyclerView vSessionList;
    private FloatingActionButton vAddSessionButton;
    private ProgressBar vLoading;
    private Toolbar vToolbar;
    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;

    private List<JsonObject> mSessionList = new ArrayList<>();
    private boolean mLoading = false;
    private SessionListAdapter mSessionListAdapter;
    private SessionApi mSessionApi;
    private String mUserId;
    private LinearLayoutManager mSessionListLayoutManager;
    private ImageView vHeaderImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        vDrawer = (DrawerLayout) findViewById(R.id.main_drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, vDrawer, vToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        vDrawer.addDrawerListener(toggle);
        toggle.syncState();
        vNavigationView = (NavigationView) findViewById(R.id.nav_view);
        vHeaderImage = (ImageView) vNavigationView.getHeaderView(0).findViewById(R.id.main_nav_header_image);
        vNavigationView.setNavigationItemSelectedListener(this);
        vSessionList = (RecyclerView) findViewById(R.id.sessions_list);
        mSessionListAdapter = new SessionListAdapter(mSessionList);
        mSessionListLayoutManager = new LinearLayoutManager(this);
        vSessionList.setLayoutManager(mSessionListLayoutManager);
        vSessionList.setAdapter(mSessionListAdapter);
        vAddSessionButton = (FloatingActionButton) findViewById(R.id.add_session_button);
        vAddSessionButton.setOnClickListener(mOnAddSessionButtonClick);
        vLoading = (ProgressBar) findViewById(R.id.sessions_list_loading);

         Picasso.with(this)
                 .load(ActivityUtils.getImageUrl(this))
                 .transform(new CircleTransformation())
                 .placeholder(R.drawable.ic_account_circle_white_48dp)
                 .into(vHeaderImage);

        mSessionApi = ApiUtils.createApi(SessionApi.class);
        mUserId = ActivityUtils.getUserId(this);
        if (mUserId != null) {
            loadSessions(true);
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
        }
        vNavigationView.setCheckedItem(R.id.main_drawer_sessions);
    }

    @Override
    public void onBackPressed() {
        if (vDrawer.isDrawerOpen(GravityCompat.START)) {
            vDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Fragment fragment;
        int id = item.getItemId();
        switch (id) {
            case R.id.main_drawer_sessions:
                Toast.makeText(MainActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_drawer_queue:
                Toast.makeText(MainActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new AssertionError("Unknown id");
        }

        // getSupportFragmentManager()
        //         .beginTransaction()
        //         .replace(R.id.music_picker_content, fragment)
        //         .addToBackStack(null)
        //         .commit();

        vDrawer.closeDrawer(GravityCompat.START);
        return true;
    }



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
                        loadSessions(false);
                    }
                    @Override
                    public void onFailure(Call<String> call, Throwable t) {
                        Log.d("VFY", "Error creating session", t);
                        Toast.makeText(MainActivity.this, "Error creating session", Toast.LENGTH_SHORT).show();
                    }
                });
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
                loadSessions(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("VFY", "MainActivity.onStart()");
        loadSessions(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("VFY", "MainActivity.onResume()");
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

    private Callback<JsonArray> mGetSessionsCallback = new Callback<JsonArray>() {
        @Override
        public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
            mLoading = false;
            vLoading.setVisibility(View.GONE);
            JsonArray sessions = response.body().getAsJsonArray();
            Log.v("VFY", "Sessions loaded: size = " + sessions.size());
            mSessionList.clear();
            for (JsonElement session : sessions) {
                mSessionList.add(session.getAsJsonObject());
            }
            mSessionListAdapter.notifyDataSetChanged();
        }
        @Override
        public void onFailure(Call<JsonArray> call, Throwable t) {
            mLoading = false;
            vLoading.setVisibility(View.GONE);
            Log.e("VFY", "Error retrieving sessions list", t);
            Toast.makeText(MainActivity.this, "Error retrieving sessions list", Toast.LENGTH_SHORT).show();
        }
    };

    public static class SessionListAdapter extends RecyclerView.Adapter<SessionItemHolder> {

        private List<JsonObject> mBackingList;

        public SessionListAdapter(List<JsonObject> backingList) {
            mBackingList = backingList;
        }

        @Override
        public SessionItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_list, parent, false);
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
        private final ImageView vPlayingIcon;
        private final ImageView vItemIcon;

        public SessionItemHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();
            vNameText = (TextView) itemView.findViewById(R.id.session_list_item_name);
            vDescriptionText = (TextView) itemView.findViewById(R.id.session_list_item_description);
            vPlayingIcon = (ImageView) itemView.findViewById(R.id.session_list_item_playing_icon);
            vItemIcon = (ImageView) itemView.findViewById(R.id.session_list_item_icon);
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
            JsonObject queue = session.get("queue").getAsJsonObject();
            JsonObject currentTrack = ApiUtils.getCurrentTrack(queue);
            JsonObject nextTrack = ApiUtils.getNextTrack(queue);
            String status = queue.get("track_status").getAsString();
            String imageUrl = session.get("image_url").getAsString();
            imageUrl = ApiUtils.getServerUrl(imageUrl);

            Picasso.with(mContext)
                    .load(imageUrl)
                    .into(vItemIcon);
            vNameText.setText(name);
            vDescriptionText.setText(description);

            vPlayingIcon.setVisibility(View.GONE);
            if (currentTrack != null) {
                vPlayingIcon.setVisibility(View.VISIBLE);
                if (status.equals("playing")) {
                    vPlayingIcon.setImageResource(R.drawable.ic_play_arrow_white_24dp);
                } else {
                    vPlayingIcon.setImageResource(R.drawable.ic_pause_white_24dp);
                }
            }
        }

    }
}
