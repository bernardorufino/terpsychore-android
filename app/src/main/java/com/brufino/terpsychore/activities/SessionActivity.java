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

import static com.google.common.base.Preconditions.*;

/* TODO: Implement server-side authentication with access token refresh */
public class SessionActivity extends AppCompatActivity
        implements PlayerNotificationCallback, ConnectionStateCallback {

    public static final String SESSION_ID_EXTRA_KEY = "sessionId";
    public static final String TRACK_ID_EXTRA_KEY = "trackId";
    public static final String TRACK_NAME_EXTRA_KEY = "trackName";
    public static final String TRACK_ARTIST_EXTRA_KEY = "trackArtist";

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;

    public static final String LIVE_TRACK_CURVE_SAVED_STATE_KEY = "liveTrackCurve";
    public static final String TRACK_CURVE_SAVED_STATE_KEY = "trackCurve";

    private Player mSpotifyPlayer;
    private GraphTrackView vGraphTrackView;
    private TrackCurve mTrackCurve;
    private TrackCurve mLiveTrackCurve;

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

        if (savedInstanceState != null) {
            mTrackCurve = savedInstanceState.getParcelable(TRACK_CURVE_SAVED_STATE_KEY);
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
            Log.d("VFY", "graph track view = " + vGraphTrackView.hashCode());
        } else {
            mTrackCurve = TrackCurve.random();
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
            new Handler().postDelayed(mUpdateLiveTrackCurve, 50);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TRACK_CURVE_SAVED_STATE_KEY, mTrackCurve);
    }

    private double mX = -0.005;

    /* TODO:     We have a leakage here, observe the hash codes... */
    private Runnable mUpdateLiveTrackCurve = new Runnable() {
        @Override
        public void run() {
            mX = Math.min(1, mX + 0.005);
            mTrackCurve.seekPosition(mX);
            vGraphTrackView.postInvalidate();
            Log.d("VFY", "runnable: graph track view = " + vGraphTrackView.hashCode());
            if (mX < 1) {
                new Handler().postDelayed(mUpdateLiveTrackCurve, 80);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case SPOTIFY_LOGIN_REQUEST_CODE:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                checkState(response.getType() == AuthenticationResponse.Type.TOKEN);
                // int expiresIn = response.getExpiresIn();
                // Log.d("VFY", "access token = " + response.getAccessToken());
                // Log.d("VFY", "access token expires in " + expiresIn + " s");
                Config playerConfig = new Config(this, response.getAccessToken(), SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, mSpotifyPlayerInitializationObserver);
        }
    }

    private Player.InitializationObserver mSpotifyPlayerInitializationObserver = new Player.InitializationObserver() {
        @Override
        public void onInitialized(Player player) {
            mSpotifyPlayer = player;
            mSpotifyPlayer.addConnectionStateCallback(SessionActivity.this);
            mSpotifyPlayer.addPlayerNotificationCallback(SessionActivity.this);
            //mSpotifyPlayer.play("spotify:track:5CKAVRV6J8sWQBCmnYICZD");
        }

        @Override
        public void onError(Throwable throwable) {
            Log.e("VFY", "Error in player initialization: " + throwable.getClass().getSimpleName());
            Log.e("VFY", "Error in player initialization: " + throwable.getMessage());
        }
    };

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


    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("VFY", "onPlaybackEvent(): " + eventType.name());
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {
        Log.d("VFY", "onPlaybackError(): " + errorType.name());
    }

    @Override
    protected void onDestroy() {
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }
}
