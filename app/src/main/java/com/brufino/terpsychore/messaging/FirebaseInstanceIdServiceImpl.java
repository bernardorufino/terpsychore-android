package com.brufino.terpsychore.messaging;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import com.brufino.terpsychore.lib.ApiCallback;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.AuthenticationApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.gson.JsonObject;
import com.jaredrummler.android.device.DeviceName;
import retrofit2.Call;
import retrofit2.Response;

public class FirebaseInstanceIdServiceImpl extends FirebaseInstanceIdService {

    public static Call<JsonObject> tryRegisterDevice(Context context) {
        String userId = ActivityUtils.getUserId(context);
        String token = FirebaseInstanceId.getInstance().getToken();
        if (token == null) {
            Log.d("VFY", "Firebase: token returned by FirebaseInstanceId.getToken() is null, trying from shared prefs...");
            token = ActivityUtils.getFirebaseToken(context);
        }
        String providedId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String name = DeviceName.getDeviceName();

        if (userId == null || token == null) {
            Log.w("VFY", "Firebase: not registering device because userId or token is null");
            Log.w("VFY", "  userId = " + userId);
            Log.w("VFY", "  token = " + token);
            return null;
        }

        Log.d("VFY", "Firebase: registering device in server:");
        Log.d("VFY", "  user_id = " + userId);
        Log.d("VFY", "  token = " + token);
        Log.d("VFY", "  type = android");
        Log.d("VFY", "  provided_id = " + providedId);
        Log.d("VFY", "  name = " + name);

        AuthenticationApi api = ApiUtils.createApi(context, AuthenticationApi.class);
        Call<JsonObject> call = api.registerDevice(
                userId,
                CoreUtils.mapToJsonObject(new ImmutableMap.Builder<String, String>()
                        .put("type", "android")
                        .put("provided_id", providedId)
                        .put("name", name)
                        .put("token", token)
                        .build()));
        call.enqueue(new ApiCallback<JsonObject>() {
            @Override
            public void onSuccess(Call<JsonObject> call, Response<JsonObject> response) {
                Log.d("VFY", "Device registered");
                Log.d("VFY", "  body = " + response.body());
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t, Response<JsonObject> response) {
                Log.e("VFY", "Error registering device", t);
            }
        });
        return call;
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
        getBaseContext().getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(SharedPreferencesDefs.Main.KEY_FIREBASE_TOKEN, token)
                .apply();
        Log.d("VFY", "Firebase: fresh token = " + token);
        tryRegisterDevice(getBaseContext());
    }
}
