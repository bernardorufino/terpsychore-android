package com.brufino.terpsychore.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.google.common.base.Preconditions.*;

/* TODO: Rewrite remote calls throughout the app and create a helper as a middle-man btw view requests and retrofit */
public class ApiUtils {

    //public static final String BASE_URL = "http:// vibefy.herokuapp.com";
    public static final String BASE_URL = "http://192.168.0.31:5000";

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

    public static String getServerUrl(String endpoint) {
        return BASE_URL + "/" + endpoint.replaceFirst("^/", "");
    }

    public static JsonObject getCurrentTrack(JsonObject queue) {
        return getTrackAfterCurrent(queue, 0);
    }

    public static JsonObject getNextTrack(JsonObject queue) {
        return getTrackAfterCurrent(queue, 1);
    }

    private static JsonObject getTrackAfterCurrent(JsonObject queue, int offset) {
        JsonElement currentTrackIndex = queue.get("current_track");
        int i = currentTrackIndex.isJsonNull() ? -1 : currentTrackIndex.getAsInt();
        i += offset;
        JsonArray tracks = queue.get("tracks").getAsJsonArray();
        if (0 <= i && i < tracks.size()) {
            return tracks.get(i).getAsJsonObject();
        }
        return null;
    }

    public static Call<String> postTracks(
            Context context,
            int sessionId,
            String[] trackUris,
            Callback<String> callback) {
        SessionApi api = createApi(SessionApi.class);
        JsonObject body = new JsonObject();
        JsonArray tracks = new JsonArray();
        for (String trackUri : trackUris) {
            tracks.add(trackUri);
        }
        body.add("tracks", tracks);
        Call<String> call = api.postTracks(sessionId, body);
        call.enqueue(callback);
        return call;
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
