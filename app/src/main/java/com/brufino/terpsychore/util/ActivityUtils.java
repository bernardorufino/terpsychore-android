package com.brufino.terpsychore.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;

import static com.google.common.base.Preconditions.checkNotNull;

public class ActivityUtils {

    public static String getUserSpotifyId(Context context) {
        return context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_USER_SPOTIFY_ID, null);
    }

    public static String getImageUrl(Context context) {
        return context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_IMAGE_URL, null);
    }

    public static String getUserId(Context context) {
        return context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_USER_ID, null);
    }

    public static String checkedGetAccessToken(Context context) {
        String accessToken = context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_ACCESS_TOKEN, null);
        checkNotNull(accessToken, "Shared preferences doesn't have access_token");
        return accessToken;
    }

    public static ColorStateList getColorList(Context context, int colorResId) {
        return ColorStateList.valueOf(ContextCompat.getColor(context, colorResId));
    }

    // Prevents instantiation
    private ActivityUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
