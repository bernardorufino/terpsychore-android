package com.brufino.terpsychore.util;

import android.content.Context;
import com.brufino.terpsychore.lib.SharedPreferencesDefs;

public class ActivityUtils {

    public static String getUserId(Context context) {
        return context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .getString(SharedPreferencesDefs.Main.KEY_USER_ID, null);
    }

    // Prevents instantiation
    private ActivityUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
