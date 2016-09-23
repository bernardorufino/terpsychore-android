package com.brufino.terpsychore.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.CircleTransformation;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.AuthenticationApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.squareup.picasso.Picasso;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Do the flow requesting code here on android,
// receive code, send to our servers
// our servers exchange code for refresh token and secret
// use those to get a new token
// send down to client, store token and provide a way to renew token in case it expires v
// TODO: https://github.com/spotify/android-sdk/issues/10
// TODO: Provide buttons for retry in case of failure
// TODO: Check what happens if click outside spotify loading modal
public class LoginActivity extends AppCompatActivity {

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final String SPOTIFY_CLIENT_SECRET = "ad319f9d5e6d48dfa81974e3d9b2c831";
    private static final String SPOTIFY_REDIRECT_URI = "vibefy://spotify/callback";
    private static final int SPOTIFY_LOGIN_REQUEST_CODE = 36175;
    private static final int DELIVER_RESULT_DELAY_MS = 1500;
    private static final int RESULT_SUCCESS_LOGIN = 1;

    private TextView vMessageText;
    private ImageView vProfileImage;
    private ProgressBar vProgressBar;

    private AuthenticationApi mAuthenticationApi;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        vProfileImage = (ImageView) findViewById(R.id.login_profile_image);
        vMessageText = (TextView) findViewById(R.id.login_message);
        vProgressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
        vProgressBar.setProgress(0);
        vProgressBar.setMax(100);

        mAuthenticationApi = ApiUtils.createApi(AuthenticationApi.class);
        Call<JsonObject> call = mAuthenticationApi.getScopes();
        call.enqueue(mGetScopesCallback);
    }

    /* TODO: Think about activity life cycle */
    private Callback<JsonObject> mGetScopesCallback = new Callback<JsonObject>() {
        @Override
        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            JsonArray scopesJson = response.body().getAsJsonArray("scopes");
            String[] scopes = new String[scopesJson.size()];
            for (int i = 0; i < scopes.length; i++) {
                scopes[i] = scopesJson.get(i).getAsString();
            }
            vProgressBar.setProgress(33);

            AuthenticationRequest request = new AuthenticationRequest.Builder(
                    SPOTIFY_CLIENT_ID,
                    AuthenticationResponse.Type.CODE,
                    SPOTIFY_REDIRECT_URI)
                    .setScopes(scopes)
                    .build();
            AuthenticationClient.openLoginActivity(LoginActivity.this, SPOTIFY_LOGIN_REQUEST_CODE, request);
        }
        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            /* TODO: Handle error */
            Toast.makeText(LoginActivity.this, "Auth failure while getting scopes!", Toast.LENGTH_SHORT).show();
            Log.e("VFY", "Auth failure while getting scopes", t);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // TODO: check resultCode for when the user dismisses the modal
        switch (requestCode) {
            case SPOTIFY_LOGIN_REQUEST_CODE:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
                if (response.getType() == AuthenticationResponse.Type.CODE) {
                    String code = response.getCode();
                    vProgressBar.setProgress(66);

                    JsonObject body = new JsonObject();
                    body.addProperty("code", code);
                    Call<JsonObject> call = mAuthenticationApi.postCode(body);
                    call.enqueue(mPostCodeCallback);
                } else {
                    Log.e("VFY", "Error! returned response type was " + response.getType());
                    if (response.getType() == AuthenticationResponse.Type.ERROR) {
                        Log.e("VFY", "Error: " + response.getError());
                    }
                }
        }
    }

    private Callback<JsonObject> mPostCodeCallback = new Callback<JsonObject>() {

        @Override
        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
            if (response.body() == null) {
                fail(call, null, null);
                return;
            }
            Log.d("VFY", response.body().toString());
            if (response.body().get("error") != null) {
                String message = response.body().get("message").getAsString();
                fail(call, null, message);
                return;
            }

            String userSpotifyId = response.body().get("spotify_id").getAsString();
            String accessToken = response.body().get("access_token").getAsString();
            String displayName = response.body().get("display_name").getAsString();
            String userId = response.body().get("id").getAsString();
            String email = response.body().get("email").getAsString();
            String imageUrl = response.body().get("image_url").getAsString();
            String expiresAt = response.body().get("expires_at").getAsString(); // TODO: Transform into date

            Picasso.with(LoginActivity.this)
                    .load(imageUrl)
                    .transform(new CircleTransformation())
                    .placeholder(R.drawable.ic_account_circle_white_148dp)
                    .into(vProfileImage);

            SharedPreferences preferences =
                    getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE);
            preferences.edit()
                    .putString(SharedPreferencesDefs.Main.KEY_USER_SPOTIFY_ID, userSpotifyId)
                    .putString(SharedPreferencesDefs.Main.KEY_ACCESS_TOKEN, accessToken)
                    .putString(SharedPreferencesDefs.Main.KEY_DISPLAY_NAME, displayName)
                    .putString(SharedPreferencesDefs.Main.KEY_USER_ID, userId)
                    .putString(SharedPreferencesDefs.Main.KEY_EMAIL, email)
                    .putString(SharedPreferencesDefs.Main.KEY_IMAGE_URL, imageUrl)
                    .putString(SharedPreferencesDefs.Main.KEY_EXPIRES_AT, expiresAt)
                    .apply();

            // TODO: Use string resource with placeholder
            vMessageText.setText("Welcome, " + displayName);
            vProgressBar.setProgress(100);
            new Handler(Looper.getMainLooper()).postDelayed(mDeliverResultRunnable, DELIVER_RESULT_DELAY_MS);
        }

        @Override
        public void onFailure(Call<JsonObject> call, Throwable t) {
            /* TODO: Handle error */
            fail(call, t, null);
        }

        private void fail(Call<JsonObject> call, Throwable t, String message) {
            Toast.makeText(LoginActivity.this, "Auth failure while posting code to server!", Toast.LENGTH_SHORT).show();
            Log.e("VFY", "Auth failure while posting code to server", t);
            if (message != null) {
                Log.e("VFY", "  " + message);
            }
        }
    };

    private Runnable mDeliverResultRunnable = new Runnable() {
        @Override
        public void run() {
            setResult(RESULT_SUCCESS_LOGIN);
            finish();
        }
    };
}
