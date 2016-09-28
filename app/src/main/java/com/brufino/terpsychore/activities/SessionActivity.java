package com.brufino.terpsychore.activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.session.ChatFragment;
import com.brufino.terpsychore.fragments.session.QueueFragment;
import com.brufino.terpsychore.fragments.session.TrackPlaybackFragment;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.sdk.android.player.Player;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.common.base.Preconditions.*;

/* TODO: Handle rotation for e.g. */
public class SessionActivity extends AppCompatActivity {

    public static final String SESSION_ID_EXTRA_KEY = "sessionId";
    public static final String TRACK_ID_EXTRA_KEY = "trackId";
    public static final String TRACK_NAME_EXTRA_KEY = "trackName";
    public static final String TRACK_ARTIST_EXTRA_KEY = "trackArtist";
    public static final String SAVED_STATE_KEY_SESSION = "session";
    private static final String GRAPH_TRACK_FRAGMENT_TAG = "graphTrackFragmentTag";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;
    private static final int REQUEST_SELECT_USERS = 1;

    private Toolbar vToolbar;
    private TextView vTrackTitleName;
    private TextView vTrackTitleArtist;
    private RelativeLayout vOverlayLayer;
    private FrameLayout vOverlayFragmentContainer;
    private QueueFragment mQueueFragment;
    private View vRootView;

    private TrackPlaybackFragment mTrackPlaybackFragment;
    private ChatFragment mChatFragment;
    private Player mPlayer;
    private PlayerManager mPlayerManager;
    private QueueManager mQueueManager;
    private SessionApi mSessionApi;
    private int mSessionId;
    private String mUserId;
    private JsonObject mSession;
    private JsonObject mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("VFY", "SessionActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        vRootView = findViewById(R.id.session_root_view);
        vOverlayLayer = (RelativeLayout) findViewById(R.id.session_overlay_layer);
        vOverlayLayer.setVisibility(View.GONE);
        vOverlayLayer.setOnClickListener(mOnOverlayLayerClickListener);
        vOverlayFragmentContainer = (FrameLayout) findViewById(R.id.session_overlay_fragment_container);
        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mQueueFragment = new QueueFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.session_overlay_fragment_container, mQueueFragment, QueueFragment.class.getSimpleName())
                .commit();
        mTrackPlaybackFragment =
                (TrackPlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.session_track_playback_fragment);
        mTrackPlaybackFragment.setQueueViewManager(mQueueViewManager);
        mChatFragment = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.session_chat_fragment);

        mSessionApi = ApiUtils.createApi(SessionApi.class);
        mUserId = checkNotNull(ActivityUtils.getUserId(this), "User id can't be null");
        mSessionId = getIntent().getIntExtra(SESSION_ID_EXTRA_KEY, -1);
        checkState(mSessionId != -1, "Can't start SessionActivity without a session id");
        mPlayerManager = new PlayerManager(this);
        mPlayerManager.addTrackUpdateListener(mTrackPlaybackFragment);
        mQueueManager = new QueueManager(this, mPlayerManager);
        mTrackPlaybackFragment.setQueueManager(mQueueManager);
        mQueueFragment.setQueueManager(mQueueManager);

        /* TODO: Don't fetch again if rotation etc. initializePlayer(); */
        if (savedInstanceState == null) {
            // mSession is assigned in mGetSessionCallback.onResponse()
            // loadSession() is called in same method
            mSessionApi.getSession(mUserId, mSessionId).enqueue(mGetSessionCallback);
        }
    }

    @Override
    // Use onRestoreInstanceState instead of doing so in the onCreate() method because this happens after the
    // fragments' onActivityCreated() method is called
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d("VFY", "SessionActivity.onRestoreInstanceState()");
        checkNotNull(savedInstanceState);
        String sessionJson = savedInstanceState.getString(SAVED_STATE_KEY_SESSION);
        mSession = new JsonParser().parse(sessionJson).getAsJsonObject();
        loadSession(mSession);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_session, menu);
        return true;
    }

    private DialogInterface.OnClickListener mOnDeleteSessionListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mSessionApi.deleteSession(mUserId, mSessionId).enqueue(new ApiCallback<String>() {
                @Override
                public void onSuccess(Call<String> call, Response<String> response) {
                    Intent sessionsIntent = new Intent(SessionActivity.this, MainActivity.class);
                    sessionsIntent.putExtra(MainActivity.EXTRA_FRAGMENT, R.id.main_drawer_music_inbox);
                    navigateUpTo(sessionsIntent);
                }
                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("VFY", "Error deleting session", t);
                    Toast.makeText(SessionActivity.this, "Error deleting session", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_USERS:
                if (resultCode == Activity.RESULT_OK) {
                    String[] userIds = data.getStringArrayExtra(UserPickerActivity.RESULT_USER_IDS);
                    Toast.makeText(this, "TODO: Do something with " + userIds.length + " user ids", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No friends selected", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setMessage("Remove session?")
                        .setPositiveButton("Remove", mOnDeleteSessionListener)
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            case R.id.action_add_user:
                Intent intent = new Intent(SessionActivity.this, UserPickerActivity.class);
                startActivityForResult(intent, REQUEST_SELECT_USERS);
                return true;
            case R.id.action_settings:
                Toast.makeText(this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadSession(JsonObject session) {
        mQueue = session.get("queue").getAsJsonObject();
        boolean host = session.get("host").getAsBoolean();
        mQueueManager.setHost(host);
        mQueueManager.setQueue(mQueue);
        vToolbar.setTitle(session.get("name").getAsString());
    }

    @Override
    public void onBackPressed() {
        if (vOverlayLayer.isShown()) {
            vOverlayLayer.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    private View.OnClickListener mOnOverlayLayerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Overlay contains the queue fragment
            vOverlayLayer.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_STATE_KEY_SESSION, mSession.toString());
    }

    private Callback<JsonObject> mGetSessionCallback = new Callback<JsonObject>() {
        @Override
        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            mSession = response.body().getAsJsonObject();
            loadSession(mSession);
        }
        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            throw Throwables.propagate(t);
        }
    };

    @Override
    protected void onDestroy() {
        mPlayerManager.onDestroy();
        super.onDestroy();
    }

    private TrackPlaybackFragment.QueueViewManager mQueueViewManager = new TrackPlaybackFragment.QueueViewManager() {
        @Override
        public void onOpenQueueView(View viewHint) {
            Rect position = ViewUtils.getRelativeGlobalVisibleRect(viewHint, vOverlayLayer);
            int left = position.left;
            int top = position.bottom - mQueueFragment.getTopBarPlusTrackItemHeight();

            vOverlayLayer.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams layoutParams
                    = (RelativeLayout.LayoutParams) vOverlayFragmentContainer.getLayoutParams();
            layoutParams.setMargins(left, top, 0, 0);
            layoutParams.width = position.width();
            layoutParams.height = 900;
            vOverlayFragmentContainer.requestLayout();
        }
    };

}
