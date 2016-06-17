package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.network.AuthenticationApi;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

// Do the flow requesting code here on android,
// receive code, send to our servers
// our servers exchange code for refresh token and secret
// use those to get a new token
// send down to client, store token and provide a way to renew token in case it expires v
// TODO: https://github.com/spotify/android-sdk/issues/10
public class LoginActivity extends AppCompatActivity {

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //AuthenticationRequest request = new AuthenticationRequest.Builder(
        //        SPOTIFY_CLIENT_ID,
        //        AuthenticationResponse.Type.CODE,
        //        SPOTIFY_REDIRECT_URI)
        //        .setScopes(new String[] { "user-read-private", "streaming" })
        //        .build();
        //
        //AuthenticationClient.openLoginActivity(this, SPOTIFY_LOGIN_REQUEST_CODE, request);

        //new Handler().postDelayed(new Runnable() {
        //    @Override
        //    public void run() {
        //        Intent intent = new Intent(LoginActivity.this, SessionActivity.class);
        //        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, "12");
        //        intent.putExtra(SessionActivity.TRACK_ID_EXTRA_KEY, "spotify:track:3Gaj5GBeZ8aynvtPkxrr9A");
        //        intent.putExtra(SessionActivity.TRACK_NAME_EXTRA_KEY, "Paradise");
        //        intent.putExtra(SessionActivity.TRACK_ARTIST_EXTRA_KEY, "Tiësto");
        //        startActivity(intent);
        //    }
        //}, 2000);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://vibefy.herokuapp.com")
                .build();
        AuthenticationApi authenticationApi = retrofit.create(AuthenticationApi.class);
        Call<JsonObject> call = authenticationApi.getTest("foo");
        call.enqueue(mCallback);
    }

    private Callback<JsonObject> mCallback = new Callback<JsonObject>() {
        @Override
        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            String param = response.body().get("param").toString();
            Toast.makeText(LoginActivity.this, "Auth response! param = " + param, Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            Toast.makeText(LoginActivity.this, "Auth failure!", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {
            case SPOTIFY_LOGIN_REQUEST_CODE:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                if (response.getType() == AuthenticationResponse.Type.CODE) {
                    String code = response.getCode();
                } else {
                    Log.e("VFY", "Error! returned response type was " + response.getType());
                    if (response.getType() == AuthenticationResponse.Type.ERROR) {
                        Log.e("VFY", "Error: " + response.getError());
                    }
                }

        }
    }
}
