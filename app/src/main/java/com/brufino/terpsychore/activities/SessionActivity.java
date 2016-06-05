package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.GraphTrackFragment;
import com.brufino.terpsychore.view.trackview.TrackProgressBar;
import com.google.common.util.concurrent.AtomicDouble;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.*;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.google.common.base.Preconditions.*;

/* TODO: Implement server-side authentication with access token refresh */
public class SessionActivity extends AppCompatActivity {

    public static final String SESSION_ID_EXTRA_KEY = "sessionId";
    public static final String TRACK_ID_EXTRA_KEY = "trackId";
    public static final String TRACK_NAME_EXTRA_KEY = "trackName";
    public static final String TRACK_ARTIST_EXTRA_KEY = "trackArtist";

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;

    public static final String CURRENT_POSITION_SAVED_STATE_KEY = "currentPosition";
    private static final String GRAPH_TRACK_FRAGMENT_TAG = "graphTrackFragmentTag";
    private static final long UPDATE_INTERVAL_IN_MS = 80;
    private static final int REPLAY_TIME_WINDOW_IN_MS = 10_000;

    private FrameLayout vTrackViewContainer;
    private TrackProgressBar vTrackProgressBar;
    private TextView vDisplayCurrentTrackTime;
    private TextView vDisplayTotalTrackTime;
    private Toolbar vToolbar;

    private Player mPlayer;
    private AtomicDouble mCurrentPosition;
    private GraphTrackFragment mGraphTrackFragment;
    private volatile boolean mActivityAlive; /* TODO: Analyse concurrency issues */
    private volatile boolean mSeekCurrentPosition = false; /* TODO: Analyse concurrency issues */
    private List<TrackUpdateListener> mTrackUpdateListeners = new LinkedList<>();

    private ImageButton vReplayButton;
    private ImageButton vPlayButton;
    private ImageButton vNextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        vTrackViewContainer = (FrameLayout) findViewById(R.id.track_view_container);
        vTrackProgressBar = (TrackProgressBar) findViewById(R.id.track_progress_bar);
        vDisplayCurrentTrackTime = (TextView) findViewById(R.id.display_current_track_time);
        vDisplayCurrentTrackTime.setText(formatTrackTime(0));
        vDisplayTotalTrackTime = (TextView) findViewById(R.id.display_total_track_time);
        vDisplayTotalTrackTime.setText(formatTrackTime(0));
        vReplayButton = (ImageButton) findViewById(R.id.playback_control_replay);
        vReplayButton.setOnClickListener(mOnReplayButtonClickListener);
        vPlayButton = (ImageButton) findViewById(R.id.playback_control_play);
        vPlayButton.setOnClickListener(mOnPlayButtonClickListener);
        vNextButton = (ImageButton) findViewById(R.id.playback_control_next);
        vNextButton.setOnClickListener(mOnNextButtonClickListener);


        String sessionId = getIntent().getStringExtra(SESSION_ID_EXTRA_KEY);
        checkNotNull(sessionId, "Can't start SessionActivity without a session id");
        String trackId = getIntent().getStringExtra(TRACK_ID_EXTRA_KEY);
        String trackName = getIntent().getStringExtra(TRACK_NAME_EXTRA_KEY);
        String trackArtist = getIntent().getStringExtra(TRACK_ARTIST_EXTRA_KEY);

        vToolbar.setTitle(trackArtist + ": " + trackName);

        /* TODO: Extract this logic into a helper / util class! */
        AuthenticationRequest request = new AuthenticationRequest.Builder(
                SPOTIFY_CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                SPOTIFY_REDIRECT_URI)
                .setScopes(new String[] { "user-read-private", "streaming" })
                .build();

        AuthenticationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request);

        if (savedInstanceState != null) {
            mCurrentPosition = new AtomicDouble(savedInstanceState.getDouble(CURRENT_POSITION_SAVED_STATE_KEY));
            // super.onCreate() will restore the fragment
            mGraphTrackFragment =
                    (GraphTrackFragment) getSupportFragmentManager().findFragmentByTag(GRAPH_TRACK_FRAGMENT_TAG);
            Log.d("VFY", "mGraphTrackFragment = " + mGraphTrackFragment);
        } else {
            mCurrentPosition = new AtomicDouble(0.2);
            mGraphTrackFragment = new GraphTrackFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.track_view_container, mGraphTrackFragment, GRAPH_TRACK_FRAGMENT_TAG)
                    .commit();
        }
        mTrackUpdateListeners.add(mGraphTrackFragment);
        mTrackUpdateListeners.add(mOnTrackUpdateListener);

        mActivityAlive = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putDouble(CURRENT_POSITION_SAVED_STATE_KEY, mCurrentPosition.doubleValue());
    }

    /* TODO: Refactor this mess of listeners attached to playback controls registering other listeners. Unify? */

    private View.OnClickListener mOnReplayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlayer.getPlayerState(mOnReplayButtonClickRetrievePlayerState);
        }
    };

    private PlayerStateCallback mOnReplayButtonClickRetrievePlayerState = new PlayerStateCallback() {
        @Override
        public void onPlayerState(PlayerState playerState) {
            int newPosition = Math.max(0, playerState.positionInMs - REPLAY_TIME_WINDOW_IN_MS);
            mPlayer.seekToPosition(newPosition);
        }
    };

    private View.OnClickListener mOnPlayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlayer.getPlayerState(mOnPlayButtonClickRetrievePlayerState);
        }
    };

    private PlayerStateCallback mOnPlayButtonClickRetrievePlayerState = new PlayerStateCallback() {
        @Override
        public void onPlayerState(PlayerState playerState) {
            // Button image is controlled in TrackUpdater.onPlayerState()
            if (playerState.playing) {
                mPlayer.pause();
            } else {
                mPlayer.resume();
            }
        }
    };

    private View.OnClickListener mOnNextButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(SessionActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
        }
    };

    private static String formatTrackTime(int timeInMs) {
        int secs = (int) (timeInMs / 1000.0 + 0.5);
        int mins = secs / 60;
        secs = secs % 60;
        int hours = mins / 60;
        mins = mins % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        }
    }

    private TrackUpdateListener mOnTrackUpdateListener = new TrackUpdateListener() {
        @Override
        public void onTrackUpdate(double currentPosition, int durationInMs, Player player) {
            vDisplayCurrentTrackTime.setText(formatTrackTime((int) (currentPosition * durationInMs)));
            vDisplayTotalTrackTime.setText(formatTrackTime(durationInMs));
            vTrackProgressBar.setProgress(currentPosition);
        }
    };

    private void notifyTrackUpdateListeners(int durationInMs) {
        for (TrackUpdateListener listener : mTrackUpdateListeners) {
            listener.onTrackUpdate(mCurrentPosition.doubleValue(), durationInMs, mPlayer);
        }
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
        mActivityAlive = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case SPOTIFY_LOGIN_REQUEST_CODE:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                    // int expiresIn = response.getExpiresIn();
                    // Log.d("VFY", "access token = " + response.getAccessToken());
                    // Log.d("VFY", "access token expires in " + expiresIn + " s");
                    Config playerConfig = new Config(this, response.getAccessToken(), SPOTIFY_CLIENT_ID);
                    Spotify.getPlayer(playerConfig, this, mSpotifyPlayerInitializationObserver);
                } else {
                    Log.e("VFY", "Error! returned response type was " + response.getType());
                }
        }
    }

    private PlayerNotificationCallback mPlayerNotificationCallback = new PlayerNotificationCallback() {
        @Override
        public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
            // Apparently we can only reliably seekToPosition() after an AUDIO_FLUSH event
            // See https://github.com/spotify/android-sdk/issues/12
            if (mSeekCurrentPosition && eventType == EventType.AUDIO_FLUSH && playerState.durationInMs > 0) {
                Log.d("VFY", "mCurrentPosition = " + mCurrentPosition.doubleValue());
                int position = (int) (mCurrentPosition.doubleValue() * playerState.durationInMs);
                Log.d("VFY", "seeking position " + position + " ms");
                mPlayer.seekToPosition(position);
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
            Log.d("VFY", "onLoginFailed()");
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
            mPlayer = player;
            mPlayer.addConnectionStateCallback(mConnectionStateCallback);
            mPlayer.addPlayerNotificationCallback(mPlayerNotificationCallback);
            mPlayer.play("spotify:track:5CKAVRV6J8sWQBCmnYICZD");
            mSeekCurrentPosition = (mCurrentPosition.doubleValue() > 0);
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
        private boolean mPlaying;

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
                if (mPlaying && !playerState.playing) {
                    activity.vPlayButton.setImageResource(R.drawable.ic_play_arrow_white_36dp);
                } else if (!mPlaying && playerState.playing) {
                    activity.vPlayButton.setImageResource(R.drawable.ic_pause_white_36dp);
                }
                mPlaying = playerState.playing;
                if (playerState.durationInMs > 0 && !activity.mSeekCurrentPosition) {
                    activity.mCurrentPosition.set((double) playerState.positionInMs / playerState.durationInMs);
                    activity.notifyTrackUpdateListeners(playerState.durationInMs);
                }
                new Handler().postDelayed(this, UPDATE_INTERVAL_IN_MS);
            }
        }
    }

    public static interface TrackUpdateListener {
        public void onTrackUpdate(double currentPosition, int durationInMs, Player player);
    }
}
