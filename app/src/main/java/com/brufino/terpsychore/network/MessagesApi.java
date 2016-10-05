package com.brufino.terpsychore.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface MessagesApi {

    @GET("session/{sessionId}/messages")
    public Call<JsonArray> getMessages(
            @Path("sessionId") int sessionId,
            @Query("offset") int offset,
            @Query("limit") int limit);

    @GET("session/{sessionId}/new_messages")
    public Call<JsonArray> getNewMessages(
            @Path("sessionId") int sessionId,
            @Query("newer_than") int newerThanMessageId);

    @POST("session/{sessionId}/messages")
    public Call<JsonObject> postMessage(@Path("sessionId") int sessionId, @Body JsonObject body);
}
