package com.brufino.terpsychore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface SessionApi {

    @GET("user/{userId}/sessions")
    public Call<JsonArray> getSessions(@Path("userId") String userId);

    @GET("user/{userId}/session/{sessionId}")
    public Call<JsonObject> getSession(@Path("userId") String userId, @Path("sessionId") int sessionId);

    @POST("user/{userId}/sessions")
    public Call<String> postSession(@Path("userId") String userId, @Body JsonObject body);

    @DELETE("user/{userId}/session/{sessionId}")
    public Call<String> deleteSession(@Path("userId") String userId, @Path("sessionId") int sessionId);

    @GET("user/{userId}/session/{sessionId}/queue")
    public Call<JsonObject> getQueue(@Path("userId") String userId, @Path("sessionId") int sessionId);

    @POST("session/{sessionId}/queue/status")
    public Call<JsonObject> postQueueStatus(@Path("sessionId") int sessionId, @Body JsonObject body);

    @POST("session/{sessionId}/tracks")
    public Call<String> postTracks(@Path("sessionId") int sessionId, @Body JsonObject body);

    @POST("session/{sessionId}/join")
    public Call<JsonObject> joinSession(@Path("sessionId") int sessionId, @Body JsonObject body);
}
