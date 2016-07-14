package com.brufino.terpsychore.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AuthenticationApi {

    @GET("test/{param}")
    public Call<JsonObject> getTest(@Path("param") String param);

    @POST("auth/code")
    public Call<JsonObject> postCode(@Body JsonObject body);

    @GET("scopes")
    public Call<JsonObject> getScopes();

}
