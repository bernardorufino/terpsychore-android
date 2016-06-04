package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.view.trackview.graph.GraphTrackView;
import com.brufino.terpsychore.view.trackview.graph.TrackCurve;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.*;

import java.lang.ref.WeakReference;

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

    public static final String TRACK_CURVE_SAVED_STATE_KEY = "trackCurve";
    public static final String CURRENT_POSITION_SAVED_STATE_KEY = "currentPosition";

    // EPS for threshold of when to consider track finished
    private static final double EPS = 0.0005;
    private static final int GRAPH_UPDATE_INTERVAL_IN_MS = 80;

    private Player mSpotifyPlayer;
    private GraphTrackView vGraphTrackView;
    private TrackCurve mTrackCurve;
    private TrackCurve mLiveTrackCurve;
    private boolean mActivityAlive;
    private double mCurrentPosition;
    private volatile boolean mSeekCurrentPosition = false; /* TODO: Analyse concurrency issues here */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        vGraphTrackView = (GraphTrackView) findViewById(R.id.graph_track_view);

        String sessionId = getIntent().getStringExtra(SESSION_ID_EXTRA_KEY);
        checkNotNull(sessionId, "Can't start SessionActivity without a session id");
        String trackId = getIntent().getStringExtra(TRACK_ID_EXTRA_KEY);
        String trackName = getIntent().getStringExtra(TRACK_NAME_EXTRA_KEY);
        String trackArtist = getIntent().getStringExtra(TRACK_ARTIST_EXTRA_KEY);

        setTitle(trackArtist + ": " + trackName);

        /* TODO: Extract this logic into a helper / util class! */
        AuthenticationRequest request = new AuthenticationRequest.Builder(
                SPOTIFY_CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                SPOTIFY_REDIRECT_URI)
                .setScopes(new String[] { "user-read-private", "streaming" })
                .build();

        AuthenticationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request);

        // Track Graph
        TrackCurve.Style trackCurveStyle = new TrackCurve.Style()
                .setFillColor(ContextCompat.getColor(this, R.color.graphForeground))
                .setStroke(ContextCompat.getColor(this, R.color.graphStroke), 6)
                .setFillColorTop(ContextCompat.getColor(this, R.color.graphForegroundTop))
                .setStrokeTop(ContextCompat.getColor(this, R.color.graphStrokeTop), 6);

        mActivityAlive = true;
        if (savedInstanceState != null) {
            mTrackCurve = savedInstanceState.getParcelable(TRACK_CURVE_SAVED_STATE_KEY);
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
            mCurrentPosition = savedInstanceState.getDouble(CURRENT_POSITION_SAVED_STATE_KEY);
            Log.d("VFY", "restoring from current position = " + mCurrentPosition);
        } else {            mTrackCurve = TrackCurve.random();
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
            mCurrentPosition = 0;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TRACK_CURVE_SAVED_STATE_KEY, mTrackCurve);
        outState.putDouble(CURRENT_POSITION_SAVED_STATE_KEY, mCurrentPosition);
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
                Log.d("VFY", "mCurrentPosition = " + mCurrentPosition);
                int position = (int) (mCurrentPosition * playerState.durationInMs);
                Log.d("VFY", "seeking position " + position + " ms");
                mSpotifyPlayer.seekToPosition(position);
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
            mSpotifyPlayer = player;
            mSpotifyPlayer.addConnectionStateCallback(mConnectionStateCallback);
            mSpotifyPlayer.addPlayerNotificationCallback(mPlayerNotificationCallback);
            mSpotifyPlayer.play("spotify:track:5CKAVRV6J8sWQBCmnYICZD");
            mSeekCurrentPosition = (mCurrentPosition > 0);
            new Handler().post(new UpdateLiveTrackCurve(SessionActivity.this));
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e("VFY", "Error in player initialization: " + throwable.getClass().getSimpleName());
            Log.e("VFY", "Error in player initialization: " + throwable.getMessage());
        }
    };


    private static class UpdateLiveTrackCurve implements Runnable, PlayerStateCallback {

        // Prevent leakage if it were otherwise used with non-static inner class
        private WeakReference<SessionActivity> mActivityRef;

        public UpdateLiveTrackCurve(SessionActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            SessionActivity activity = mActivityRef.get();
            // Make sure cycle doesn't continue if activity was destroyed
            if (activity != null && activity.mActivityAlive) {
                checkNotNull(activity.mSpotifyPlayer, "mSpotifyPlayer is null but shouldn't");
                activity.mSpotifyPlayer.getPlayerState(this);
            }
        }

        @Override
        public void onPlayerState(PlayerState playerState) {
            SessionActivity activity = mActivityRef.get();
            if (activity != null && activity.mActivityAlive) {

                if (playerState.durationInMs > 0 && !activity.mSeekCurrentPosition) {
                    activity.mCurrentPosition = (double) playerState.positionInMs / playerState.durationInMs;
                    activity.mTrackCurve.seekPosition(activity.mCurrentPosition);
                    activity.vGraphTrackView.postInvalidate();
                }
                new Handler().postDelayed(this, GRAPH_UPDATE_INTERVAL_IN_MS);
            }
        }
    }
}
