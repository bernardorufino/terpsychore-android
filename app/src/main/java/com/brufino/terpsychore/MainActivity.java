package com.brufino.terpsychore;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.*;

import static com.google.common.base.Preconditions.*;

public class MainActivity extends AppCompatActivity
        implements PlayerNotificationCallback, ConnectionStateCallback {

    public static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    public static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    public static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    public static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;

    private Player mSpotifyPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthenticationRequest request = new AuthenticationRequest.Builder(
                SPOTIFY_CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                SPOTIFY_REDIRECT_URI)
                .setScopes(new String[] { "user-read-private", "streaming" })
                .build();

        AuthenticationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case SPOTIFY_LOGIN_REQUEST_CODE:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                checkState(response.getType() == AuthenticationResponse.Type.TOKEN);
                Config playerConfig = new Config(this, response.getAccessToken(), SPOTIFY_CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, mSpotifyPlayerInitializationObserver);
        }
    }

    private Player.InitializationObserver mSpotifyPlayerInitializationObserver = new Player.InitializationObserver() {
        @Override
        public void onInitialized(Player player) {
            mSpotifyPlayer = player;
            mSpotifyPlayer.addConnectionStateCallback(MainActivity.this);
            mSpotifyPlayer.addPlayerNotificationCallback(MainActivity.this);
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
