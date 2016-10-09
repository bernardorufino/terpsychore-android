package com.brufino.terpsychore.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.*;

public interface AuthenticationApi {

    @GET("test/{param}")
    public Call<JsonObject> getTest(@Path("param") String param);

    @POST("auth/code")
    public Call<JsonObject> postCode(@Body JsonObject body);

    @GET("scopes")
    public Call<JsonObject> getScopes();

    @POST("auth/renew")
    public Call<JsonObject> renewToken(@Query("user_id") String userId);

    @POST("user/{userId}/devices")
    public Call<JsonObject> registerDevice(@Path("userId") String userId, @Body JsonObject body);
}
