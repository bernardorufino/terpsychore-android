package com.brufino.terpsychore.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.QueueManager;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.common.base.Throwables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

/* TODO: Rewrite remote calls throughout the app and create a helper as a middle-man btw view requests and retrofit */
public class ApiUtils {

    private static Retrofit sRetrofit;

    public static <T> T createApi(Context context, Class<T> type) {
        if (sRetrofit == null) {
            sRetrofit = new Retrofit.Builder()
                    .baseUrl(getBaseUrl(context))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return sRetrofit.create(type);
    }

    public static <T> String getErrorBodyAsString(Response<T> response) {
        try {
            return response.errorBody().string();
        } catch (IOException e) {
            Log.e("VFY", "Error parsing error body", e);
            throw Throwables.propagate(e);
        }
    }

    public static <T> JsonElement getErrorBodyAsJsonElement(Response<T> response) {
        return new JsonParser().parse(getErrorBodyAsString(response));
    }

    private static String getBaseUrl(Context context) {
        String baseUrl = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.preference_base_url), null);
        checkNotNull(baseUrl, "Preference preference_base_url is null");
        return baseUrl;
    }

    public static String getServerUrl(Context context, String endpoint) {
        return getBaseUrl(context) + "/" + endpoint.replaceFirst("^/", "");
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

    public static <T> Call<JsonObject> postMessage(
            Context context,
            int sessionId,
            String type,
            Map<String, T> attributes,
            Callback<JsonObject> callback) {
        String userId = ActivityUtils.getUserId(context);
        MessagesApi api = createApi(context, MessagesApi.class);
        JsonObject body = new JsonObject();
        body.addProperty("type", type);
        JsonObject message = CoreUtils.mapToJsonObject(attributes);
        message.addProperty("session_id", sessionId);
        body.add("message", message);
        Call<JsonObject> call = api.postMessage(sessionId, userId, body);
        call.enqueue(callback);
        return call;

    }

    public static Call<JsonObject> joinSession(
            Context context,
            int sessionId,
            List<String> userIds,
            Callback<JsonObject> callback) {
        SessionApi api = createApi(context, SessionApi.class);
        JsonObject body = new JsonObject();
        body.add("user_ids", CoreUtils.stringListToJsonArray(userIds));
        Call<JsonObject> call = api.joinSession(sessionId, body);
        call.enqueue(callback);
        return call;
    }

    public static Call<String> postTracks(
            Context context,
            int sessionId,
            List<String> trackUris,
            Callback<String> callback) {

        // Filter out the spotify prefix
        Pattern pattern = Pattern.compile("^" + QueueManager.TRACK_URI_PREFIX);
        List<String> trackIds = new ArrayList<>(trackUris.size());
        for (String trackUri : trackUris) {
            trackIds.add(pattern.matcher(trackUri).replaceFirst(""));
        }

        SessionApi api = createApi(context, SessionApi.class);
        String userId = ActivityUtils.getUserId(context);
        JsonObject body = new JsonObject();
        body.add("track_ids", CoreUtils.stringListToJsonArray(trackIds));
        Call<String> call = api.postTracks(sessionId, userId, body);
        call.enqueue(callback);
        return call;
    }

    public static Call<JsonObject> renewToken(Context context, final Callback<JsonObject> callback) {
        AuthenticationApi api = createApi(context, AuthenticationApi.class);
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
