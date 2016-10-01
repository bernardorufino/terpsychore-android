package com.brufino.terpsychore.lib;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.google.common.base.Preconditions.checkArgument;

/* TODO: Migrate remaining callbacks */
/**
 * Override onSuccess() and one of onFailure(Call<T>, Throwable) or onFailure(Call<T>, Throwable, Response<T>)
 */
public abstract class ApiCallback<T> implements Callback<T> {

    private Response<T> mResponse = null;

    @Override
    public void onResponse(Call<T> call, Response<T> response) {
        mResponse = response;
        if (response.isSuccessful()) {
            onSuccess(call, response);
        } else {
            HttpErrorException httpError = new HttpErrorException(response.code());
            onFailure(call, httpError);
        }
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        onFailure(call, t, mResponse);
    }

    public void onFailure(Call<T> call, Throwable t, Response<T> response) {
        /* Override */
    }

    public abstract void onSuccess(Call<T> call, Response<T> response);

    public static class HttpErrorException extends RuntimeException {

        private int mStatusCode;

        public HttpErrorException(int statusCode) {
            super(statusCode + " http error");
            initialize(statusCode);
        }

        public HttpErrorException(int statusCode, Throwable throwable) {
            super(statusCode + " http error", throwable);
            initialize(statusCode);
        }

        private void initialize(int statusCode) {
            checkArgument(statusCode < 200 || statusCode >= 300, "statusCode can't be success");
            mStatusCode = statusCode;
        }

        public int getStatusCode() {
            return mStatusCode;
        }
    }

}
