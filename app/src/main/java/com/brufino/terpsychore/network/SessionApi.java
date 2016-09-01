package com.brufino.terpsychore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface SessionApi {

    @GET("user/{userId}/sessions")
    public Call<JsonArray> getSessions(@Path("userId") String userId);

    @GET("user/{userId}/session/{sessionId}")
    public Call<JsonObject> getSession(@Path("userId") String userId, @Path("sessionId") int sessionId);

    @POST("user/{userId}/sessions")
    public Call<String> postSession(@Path("userId") String userId, @Body JsonObject body);

}
