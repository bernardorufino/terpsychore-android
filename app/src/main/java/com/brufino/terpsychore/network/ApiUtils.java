package com.brufino.terpsychore.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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

    // Prevents instantiation
    private ApiUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
