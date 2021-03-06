package com.brufino.terpsychore.lib;

public class SharedPreferencesDefs {

    public static class Main {
        public static final String FILE = Main.class.getCanonicalName() + "_preference_file";
        public static final String KEY_USER_SPOTIFY_ID = "user_spotify_id";
        public static final String KEY_ACCESS_TOKEN = "access_token";
        public static final String KEY_DISPLAY_NAME = "display_name";
        public static final String KEY_USER_ID = "user_id";
        public static final String KEY_IMAGE_URL = "image_url";
        public static final String KEY_EXPIRES_AT = "expires_at";
        public static final String KEY_EMAIL = "email";
        public static final String KEY_FIREBASE_TOKEN = "firebaseToken";
        public static final String KEY_EMOTICON_HISTORY = "emoticonHistory";
        public static final String KEY_DEVICE_ID = "deviceId";
    }

    public static class Messaging {
        public static final String FILE = Messaging.class.getCanonicalName() + "_preference_file";
        public static final String KEY_SESSION_PREFIX = "session_";
    }

    // Prevents instantiation
    private SharedPreferencesDefs() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
