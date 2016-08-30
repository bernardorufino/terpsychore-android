package com.brufino.terpsychore.network;

import com.google.gson.JsonArray;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SessionApi {

    @GET("user/{userId}/sessions")
    public Call<JsonArray> getSessions(@Path("userId") String userId);

}
