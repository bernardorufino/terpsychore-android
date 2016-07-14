package com.brufino.terpsychore.lib;

public class SharedPreferencesDefs {

    public static class Main {
        public static final String FILE = Main.class.getCanonicalName() + "_preference_file";

        public static final String KEY_ACCESS_TOKEN = "access_token";
        public static final String KEY_DISPLAY_NAME = "display_name";
        public static final String KEY_IMAGE_URL = "image_url";
        public static final String KEY_EXPIRES_AT = "expires_at";
        public static final String KEY_EMAIL = "email";
    }

    // Prevents instantiation
    private SharedPreferencesDefs() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}