package com.brufino.terpsychore.network;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface AuthenticationApi {

    

    @GET("test/{param}")
    public Call<JsonObject> getTest(@Path("param") String param);
}
