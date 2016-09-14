package com.brufino.terpsychore.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.google.common.base.Preconditions.*;

public class ApiUtils {

    //public static final String BASE_URL = "http:// vibefy.herokuapp.com";
    public static final String BASE_URL = "http://192.168.0.103:5000";

    private static Retrofit sRetrofit;

    public static <T> T createApi(Class<T> type) {
        if (sRetrofit == null) {
            sRetrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return sRetrofit.create(type);
    }

    public static Call<JsonObject> renewToken(Context context, final Callback<JsonObject> callback) {
        AuthenticationApi api = createApi(AuthenticationApi.class);
        final SharedPreferences preferences =
                context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE);
        String userId = preferences.getString(SharedPreferencesDefs.Main.KEY_USER_ID, null);
        checkNotNull(userId, "userId can't be null");
        Call<JsonObject> call = api.renewToken(userId);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject user = response.body().get("user").getAsJsonObject();
                String accessToken = user.get("access_token").getAsString();
                String expiresAt = user.get("expires_at").getAsString();
                preferences.edit()
                        .putString(SharedPreferencesDefs.Main.KEY_ACCESS_TOKEN, accessToken)
                        .putString(SharedPreferencesDefs.Main.KEY_EXPIRES_AT, expiresAt)
                        .apply();
                Log.d("VFY", "Attempt to renew token, status = " + response.body().get("status").getAsString());
                Log.d("VFY", "Token valid until " + expiresAt);
                callback.onResponse(call, response);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
        return call;
    }

    // Prevents instantiation
    private ApiUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
