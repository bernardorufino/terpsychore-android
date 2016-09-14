package com.brufino.terpsychore.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.ChatFragment;
import com.brufino.terpsychore.fragments.TrackPlaybackFragment;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.sdk.android.player.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/* TODO: Renew token automatically */
/* TODO: Handle rotation for e.g. */
public class SessionActivity extends AppCompatActivity {

    public static final String SESSION_ID_EXTRA_KEY = "sessionId";
    public static final String TRACK_ID_EXTRA_KEY = "trackId";
    public static final String TRACK_NAME_EXTRA_KEY = "trackName";
    public static final String TRACK_ARTIST_EXTRA_KEY = "trackArtist";

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;

    public static final String SAVED_STATE_KEY_CURRENT_POSITION = "currentPosition";
    public static final String SAVED_STATE_KEY_SESSION = "session";
    private static final String GRAPH_TRACK_FRAGMENT_TAG = "graphTrackFragmentTag";
    private static final long UPDATE_INTERVAL_IN_MS = 80;

    private Toolbar vToolbar;
    private TextView vTrackTitleName;
    private TextView vTrackTitleArtist;

    private Player mPlayer;
    private volatile int mCurrentPosition; /* TODO: Analyse concurrency issues */
    private volatile boolean mActivityAlive; /* TODO: Analyse concurrency issues */
    private volatile boolean mSeekCurrentPosition = false; /* TODO: Analyse concurrency issues */
    private List<TrackUpdateListener> mTrackUpdateListeners = new LinkedList<>();
    private TrackPlaybackFragment mTrackPlaybackFragment;
    private ChatFragment mChatFragment;
    private SessionApi mSessionApi;
    private int mSessionId;
    private String mUserId;
    private JsonObject mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        vTrackTitleName = (TextView) findViewById(R.id.track_title_name);
        vTrackTitleArtist = (TextView) findViewById(R.id.track_title_artist);
        setSupportActionBar(vToolbar);

        mTrackPlaybackFragment =
                (TrackPlaybackFragment) getSupportFragmentManager().findFragmentById(R.id.session_track_playback_fragment);
        mChatFragment = (ChatFragment) getSupportFragmentManager().findFragmentById(R.id.session_chat_fragment);

        mSessionApi = ApiUtils.createApi(SessionApi.class);
        mUserId = checkNotNull(ActivityUtils.getUserId(this), "User id can't be null");
        mSessionId = getIntent().getIntExtra(SESSION_ID_EXTRA_KEY, -1);
        checkState(mSessionId != -1, "Can't start SessionActivity without a session id");

        /* TODO: Don't fetch again if rotation etc. initializePlayer(); */
        if (savedInstanceState != null) {
            String sessionJson = savedInstanceState.getString(SAVED_STATE_KEY_SESSION);
            mSession = new JsonParser().parse(sessionJson).getAsJsonObject();
            mCurrentPosition = savedInstanceState.getInt(SAVED_STATE_KEY_CURRENT_POSITION);
            loadSession(mSession);
        } else {
            // mSession is assigned in mGetSessionCallback.onResponse()
            // loadSession() is called in same method
            mSessionApi.getSession(mUserId, mSessionId).enqueue(mGetSessionCallback);
            mCurrentPosition = 60 * 1000;
        }
        mTrackUpdateListeners.add(mTrackPlaybackFragment);
        mTrackUpdateListeners.add(mOnTrackUpdateListener);

        mActivityAlive = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_STATE_KEY_SESSION, mSession.toString());
        outState.putInt(SAVED_STATE_KEY_CURRENT_POSITION, mCurrentPosition);
    }

    private void loadSession(JsonObject session) {
        String trackId = getIntent().getStringExtra(TRACK_ID_EXTRA_KEY);
        String trackName = getIntent().getStringExtra(TRACK_NAME_EXTRA_KEY);
        String trackArtist = getIntent().getStringExtra(TRACK_ARTIST_EXTRA_KEY);
        vTrackTitleName.setText(trackName);
        vTrackTitleArtist.setText(trackArtist);
        vToolbar.setTitle(session.get("name").getAsString());
        initializePlayer();
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

    private void initializePlayer() {
        SharedPreferences sharedPreferences =
                getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE);
        String accessToken = sharedPreferences.getString(SharedPreferencesDefs.Main.KEY_ACCESS_TOKEN, null);
        checkNotNull(accessToken, "Main shared preferences doesn't have access_token");
        // TODO: Manage access token life cycle (check expiration, renew beforehand, etc)
        Config playerConfig = new Config(this, accessToken, SPOTIFY_CLIENT_ID);
        Spotify.getPlayer(playerConfig, this, mSpotifyPlayerInitializationObserver);
    }

    private TrackUpdateListener mOnTrackUpdateListener = new TrackUpdateListener() {
        @Override
        public void onTrackUpdate(boolean playing, int currentPositionInMs, int durationInMs, Player player) {
            mCurrentPosition = currentPositionInMs;
        }
    };

    private void notifyTrackUpdateListeners(boolean playing, int currentPositionInMs, int durationInMs) {
        for (TrackUpdateListener listener : mTrackUpdateListeners) {
            listener.onTrackUpdate(playing, currentPositionInMs, durationInMs, mPlayer);
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
        mActivityAlive = false;
    }

    private PlayerNotificationCallback mPlayerNotificationCallback = new PlayerNotificationCallback() {
        @Override
        public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
            // Apparently we can only reliably seekToPosition() after an AUDIO_FLUSH event
            // See https://github.com/spotify/android-sdk/issues/12
            if (mSeekCurrentPosition && eventType == EventType.AUDIO_FLUSH && playerState.durationInMs > 0) {
                Log.d("VFY", "mCurrentPosition = " + mCurrentPosition);
                mPlayer.seekToPosition(mCurrentPosition);
                mSeekCurrentPosition = false;
            }
            Log.d("VFY", "onPlaybackEvent(): " + eventType.name());
        }

        @Override
        public void onPlaybackError(ErrorType errorType, String s) {
            Log.d("VFY", "onPlaybackError(): " + errorType.name());
        }
    };

    private ConnectionStateCallback mConnectionStateCallback = new ConnectionStateCallback() {
        @Override
        public void onLoggedIn() {
            Log.d("VFY", "onLoggedIn()");
        }

        @Override
        public void onLoggedOut() {
            Log.d("VFY", "onLoggedOut()");
        }

        @Override
        public void onLoginFailed(Throwable throwable) {
            Log.e("VFY", "onLoginFailed()", throwable);
        }

        @Override
        public void onTemporaryError() {
            Log.d("VFY", "onTemporaryError()");
        }

        @Override
        public void onConnectionMessage(String message) {
            Log.d("VFY", "onConnectionMessage(): message = " + message);
        }
    };

    private Player.InitializationObserver mSpotifyPlayerInitializationObserver = new Player.InitializationObserver() {

        @Override
        public void onInitialized(Player player) {
            mTrackPlaybackFragment.setPlayer(player);
            mPlayer = player;
            mPlayer.addConnectionStateCallback(mConnectionStateCallback);
            mPlayer.addPlayerNotificationCallback(mPlayerNotificationCallback);
            mPlayer.play("spotify:track:5CKAVRV6J8sWQBCmnYICZD");
            mSeekCurrentPosition = (mCurrentPosition > 0);
            new Handler().post(new TrackUpdater(SessionActivity.this));
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e("VFY", "Error in player initialization: " + throwable.getClass().getSimpleName());
            Log.e("VFY", "Error in player initialization: " + throwable.getMessage());
        }
    };

    private static class TrackUpdater implements Runnable, PlayerStateCallback {

        private final WeakReference<SessionActivity> mActivityRef;

        public TrackUpdater(SessionActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            SessionActivity activity = mActivityRef.get();
            // Make sure cycle doesn't continue if activity was destroyed
            if (activity != null && activity.mActivityAlive) {
                activity.mPlayer.getPlayerState(this);
            }
        }

        @Override
        public void onPlayerState(PlayerState playerState) {
            SessionActivity activity = mActivityRef.get();
            if (activity != null && activity.mActivityAlive) {
                if (playerState.durationInMs > 0 && !activity.mSeekCurrentPosition) {
                    activity.notifyTrackUpdateListeners(
                            playerState.playing,
                            playerState.positionInMs,
                            playerState.durationInMs);
                }
                new Handler().postDelayed(this, UPDATE_INTERVAL_IN_MS);
            }
        }
    }

    public static interface TrackUpdateListener {
        public void onTrackUpdate(boolean playing, int currentPositionInMs, int durationInMs, Player player);
    }
}
