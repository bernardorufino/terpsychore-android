package com.brufino.terpsychore.activities;

import android.app.Activity;
import android.content.*;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.session.ChatFragment;
import com.brufino.terpsychore.fragments.session.QueueFragment;
import com.brufino.terpsychore.fragments.session.TrackPlaybackFragment;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.messaging.FirebaseMessagingServiceImpl;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.sdk.android.player.Player;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.*;

/* TODO: Handle rotation for e.g. */
public class SessionActivity extends AppCompatActivity {

    public static final String SESSION_ID_EXTRA_KEY = "sessionId";
    public static final String SAVED_STATE_KEY_SESSION = "session";
    private static final int REQUEST_SELECT_USERS = 1;
    private static Set<String> SUPPORTED_MESSAGE_TYPES = Sets.newHashSet("playback_message");

    private Toolbar vToolbar;
    private TextView vTrackTitleName;
    private TextView vTrackTitleArtist;
    private RelativeLayout vOverlayLayer;
    private FrameLayout vOverlayFragmentContainer;
    private ViewGroup vTrackPlaybackFragmentContainer;

    private View mLastViewHintForQueueFragment;
    private TrackPlaybackFragment mTrackPlaybackFragment;
    private QueueFragment mQueueFragment;
    private ChatFragment mChatFragment;
    private Player mPlayer;
    private PlayerManager mPlayerManager;
    private QueueManager mQueueManager;
    private SessionApi mSessionApi;
    private int mSessionId;
    private String mUserId;
    private JsonObject mSession;
    private boolean mHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("VFY", "SessionActivity.onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        vOverlayLayer = (RelativeLayout) findViewById(R.id.session_overlay_layer);
        vOverlayLayer.setVisibility(View.GONE);
        vOverlayLayer.setOnClickListener(mOnOverlayLayerClickListener);
        vOverlayLayer.addOnLayoutChangeListener(mOnOverlayLayoutChangeListener);
        vOverlayFragmentContainer = (FrameLayout) findViewById(R.id.session_overlay_fragment_container);
        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vTrackPlaybackFragmentContainer = (ViewGroup) findViewById(R.id.session_track_playback_fragment_container);

        mQueueFragment = new QueueFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.session_overlay_fragment_container, mQueueFragment, QueueFragment.class.getSimpleName())
                .commit();

        mTrackPlaybackFragment =
                (TrackPlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.session_track_playback_fragment);
        mTrackPlaybackFragment.setQueueViewManager(mQueueViewManager);
        mChatFragment = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.session_chat_fragment);

        mSessionApi = ApiUtils.createApi(this, SessionApi.class);
        mUserId = checkNotNull(ActivityUtils.getUserId(this), "User id can't be null");
        mSessionId = getIntent().getIntExtra(SESSION_ID_EXTRA_KEY, -1);
        checkState(mSessionId != -1, "Can't start SessionActivity without a session id");
        mChatFragment.setSessionId(mSessionId);

        mPlayerManager = new PlayerManager(this);
        mPlayerManager.addTrackUpdateListener(mTrackPlaybackFragment);
        mQueueManager = new QueueManager(this, mPlayerManager);
        mTrackPlaybackFragment.setQueueManager(mQueueManager);
        mQueueFragment.setQueueManager(mQueueManager);
        // mQueue_Post_Listener because it's run after all the other listeners, if a listener before is needed please
        // name it mQueue_Pre_Listener (_ only for emphasis)
        mQueueManager.addQueueListener(mQueuePostListener);

        KeyboardVisibilityEvent.setEventListener(this, mKeyboardVisibilityListener);

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
        // Refresh queue as well?
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Update session's queue property before saving state
        mSession.add("queue", mQueueManager.getUpdatedQueue());
        outState.putString(SAVED_STATE_KEY_SESSION, mSession.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_session, menu);
        int[] showIfHost = {R.id.action_delete, R.id.action_add_user};
        for (int resId : showIfHost) {
            menu.findItem(resId).setVisible(mHost);
        }
        return true;
    }

    @Override
    protected void onStart() {
        Log.d("VFY", "SessionActivity.onStart()");
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.d("VFY", "SessionActivity.onResume()");
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(FirebaseMessagingServiceImpl.MESSAGE_RECEIVED));
        if (mSession != null) {
            // If it's null we are creating the activity, thus there's already a session request which will
            // update the queue
            mQueueManager.refreshQueue(true);
        }

    }

    @Override
    protected void onPause() {
        Log.d("VFY", "SessionActivity.onPause()");
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onStop() {
        Log.d("VFY", "SessionActivity.onStop()");
        super.onStop();
        mPlayerManager.getPlayer().pause();
    }

    @Override
    protected void onDestroy() {
        Log.d("VFY", "SessionActivity.onDestroy()");
        mPlayerManager.onDestroy();
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_MESSAGE_TYPE);
            int sessionId = intent.getIntExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_SESSION_ID, -1);
            if (SUPPORTED_MESSAGE_TYPES.contains(type) && mSessionId == sessionId) {
                String messageString = intent.getStringExtra(FirebaseMessagingServiceImpl.EXTRA_KEY_MESSAGE);
                boolean includeTracks = false;
                if (messageString != null) {
                    JsonObject message = new JsonParser().parse(messageString).getAsJsonObject();
                    String action = message.get("action").getAsString();
                    if (action.equals("sync_tracks")) {
                        includeTracks = true;
                    }
                }
                mQueueManager.refreshQueue(includeTracks);
            }
        }
    };

    private KeyboardVisibilityEventListener mKeyboardVisibilityListener = new KeyboardVisibilityEventListener() {
        @Override
        public void onVisibilityChanged(boolean isOpen) {
            vTrackPlaybackFragmentContainer.setVisibility((isOpen) ? View.GONE : View.VISIBLE);
        }
    };

    private DialogInterface.OnClickListener mOnUnjoinSessionListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mSessionApi.unjoinSession(mSessionId, mUserId).enqueue(new ApiCallback<JsonObject>() {
                @Override
                public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
                    Toast.makeText(SessionActivity.this, "Left session", Toast.LENGTH_SHORT).show();
                    Intent sessionsIntent = new Intent(SessionActivity.this, MainActivity.class);
                    sessionsIntent.putExtra(MainActivity.EXTRA_FRAGMENT, R.id.main_drawer_sessions);
                    navigateUpTo(sessionsIntent);
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t, Response<JsonObject> response) {
                    boolean shouldLog = true;
                    String message = "Error leaving session";

                    if (response != null) {
                        JsonObject body = CoreUtils.getAsJsonObjectOrNull(ApiUtils.getErrorBodyAsJsonElement(response));
                        if (body != null) {
                            String error = CoreUtils.getAsStringOrNull(body.get("error"));
                            if (Objects.equals("last_admin", error)) {
                                shouldLog = false;
                                String returnMessage = CoreUtils.getAsStringOrNull(body.get("message"));
                                if (returnMessage != null) {
                                    message = returnMessage;
                                }
                            }
                        }
                    }

                    if (shouldLog) {
                        Log.e("VFY", "Error leaving/unjoining session", t);
                    }
                    Toast.makeText(SessionActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private DialogInterface.OnClickListener mOnDeleteSessionListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mSessionApi.deleteSession(mUserId, mSessionId).enqueue(new ApiCallback<String>() {
                @Override
                public void onSuccess(Call<String> call, Response<String> response) {
                    Toast.makeText(SessionActivity.this, "Session deleted", Toast.LENGTH_SHORT).show();
                    Intent sessionsIntent = new Intent(SessionActivity.this, MainActivity.class);
                    sessionsIntent.putExtra(MainActivity.EXTRA_FRAGMENT, R.id.main_drawer_sessions);
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
                    final List<String> userIds = data.getStringArrayListExtra(UserPickerActivity.RESULT_USER_IDS);
                    ApiUtils.joinSession(this, mSessionId, userIds, new ApiCallback<JsonObject>() {
                        @Override
                        public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
                            // A new session image may be formed with the new people just added
                            String imageUrl = ApiUtils.getServerUrl(
                                    SessionActivity.this,
                                    mSession.get("image_url").getAsString());
                            ViewUtils.refreshImageInCaches(SessionActivity.this, imageUrl);

                            int nUsersAdded = response.body().get("nusers").getAsInt();
                            String message = (nUsersAdded == 0) ? "No friend was added to this session" :
                                             (nUsersAdded == 1) ? "1 friend added to this session"
                                                                : nUsersAdded + " friends added to this session";
                            Toast.makeText(SessionActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            Log.e("VFY", "Error adding users to session", t);
                            Toast.makeText(SessionActivity.this, "Error adding friends to session", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "No friends selected", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_unjoin_session:
                new AlertDialog.Builder(this)
                        .setMessage("Leave session?")
                        .setPositiveButton("Leave", mOnUnjoinSessionListener)
                        .setNegativeButton("Cancel", null)
                        .show();
                return true;
            case R.id.action_delete:
                new AlertDialog.Builder(this)
                        .setMessage("Delete session?")
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
        JsonObject queue = session.get("queue").getAsJsonObject();
        mHost = session.get("host").getAsBoolean();
        mQueueManager.setHost(mHost);
        mQueueManager.setQueue(queue);
        setTitle(session.get("name").getAsString());
        invalidateOptionsMenu();
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

    private Callback<JsonObject> mGetSessionCallback = new ApiCallback<JsonObject>() {
        @Override
        public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
            mSession = response.body().getAsJsonObject();
            loadSession(mSession);
        }
        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            Log.e("VFY", "Error retrieving session " + mSessionId, t);
            Toast.makeText(SessionActivity.this, "Error retrieving session", Toast.LENGTH_SHORT).show();
            Intent sessionsIntent = new Intent(SessionActivity.this, MainActivity.class);
            sessionsIntent.putExtra(MainActivity.EXTRA_FRAGMENT, R.id.main_drawer_sessions);
            navigateUpTo(sessionsIntent);
        }
    };

    private QueueManager.QueueListener mQueuePostListener = new QueueManager.QueueListener() {
        @Override
        public void onQueueChange(QueueManager queueManager, JsonObject queue) {
            positionQueueFragmentIfOverlayVisible();
            String trackStatus = queue.get("track_status").getAsString();
            if (trackStatus.equals("playing")) {
                Log.d("VFY", "SessionActivity: [1] turn ON WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON");
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                Log.d("VFY", "SessionActivity: [0] turn OFF WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON");
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

        }
        @Override
        public void onQueueRefreshError(QueueManager queueManager, Throwable t) {
        }
    };

    private View.OnLayoutChangeListener mOnOverlayLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int l, int t, int r, int b, int oldL, int oldT, int oldR, int oldB) {
            if (l != oldL || t != oldT || r != oldR || b != oldB) {
                positionQueueFragmentIfOverlayVisible();
            }
        }
    };

    private void positionQueueFragmentIfOverlayVisible() {
        if (!vOverlayLayer.isShown()) {
            return;
        }

        Rect position = ViewUtils.getRelativeGlobalVisibleRect(mLastViewHintForQueueFragment, vOverlayLayer);
        int left = position.left;
        int top = position.bottom + mQueueFragment.getVerticalOffset();
        int maxHeight = vOverlayLayer.getHeight() - top;
        int desiredHeight = mQueueFragment.getDesiredHeight(maxHeight);

        RelativeLayout.LayoutParams layoutParams
                = (RelativeLayout.LayoutParams) vOverlayFragmentContainer.getLayoutParams();
        layoutParams.setMargins(left, top, 0, 0);
        layoutParams.width = position.width();
        layoutParams.height = desiredHeight;
        vOverlayFragmentContainer.requestLayout();
    }

    private TrackPlaybackFragment.QueueViewManager mQueueViewManager = new TrackPlaybackFragment.QueueViewManager() {
        @Override
        public void onOpenQueueView(View viewHint) {
            mLastViewHintForQueueFragment = viewHint;
            vOverlayLayer.setVisibility(View.VISIBLE);
            positionQueueFragmentIfOverlayVisible();
            mQueueFragment.onVisible();
        }
    };
}
