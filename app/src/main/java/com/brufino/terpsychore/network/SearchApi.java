package com.brufino.terpsychore.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SearchApi {

    @GET("search/users/{query}")
    public Call<JsonObject> searchUsers(@Path("query") String query, @Query("user_id") String userId);

}
