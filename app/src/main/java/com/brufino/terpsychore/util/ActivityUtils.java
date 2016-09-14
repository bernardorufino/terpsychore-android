package com.brufino.terpsychore.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.v4.content.ContextCompat;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;

public class ActivityUtils {

    public static String getUserId(Context context) {
        return context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_USER_ID, null);
    }

    public static ColorStateList getColorList(Context context, int colorResId) {
        return ColorStateList.valueOf(ContextCompat.getColor(context, colorResId));
    }

    // Prevents instantiation
    private ActivityUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
