package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.brufino.terpsychore.activities.MusicPickerActivity;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.SpotifyService;
import retrofit2.Call;
import retrofit2.Callback;

import java.util.Collection;

import static com.google.common.base.Preconditions.*;

public abstract class SpotifyRemoteAdapter<T> extends MusicPickerList.Adapter<T> {

    private static final int INITIAL_WAIT_TIME = 1000;
    private static final int WAIT_TIME_LIMIT = 5 * 60 * 1000;

    private MusicPickerListFragment mFragment;
    private int mWaitTime = INITIAL_WAIT_TIME;
    private Bundle mParameters;

    public void setParameters(Bundle parameters) {
        mParameters = parameters;
    }

    public Bundle getParameters() {
        return mParameters;
    }

    public void setFragment(MusicPickerListFragment fragment) {
        mFragment = fragment;
    }

    public MusicPickerActivity getActivity() {
        return mFragment.getMusicPickerActivity();
    }

    public MusicPickerListFragment getFragment() {
        return mFragment;
    }

    public Context getContext() {
        return mFragment.getContext();
    }

    public SpotifyService getSpotifyService() {
        /* TODO: Check access token validity */
        SpotifyApi api = new SpotifyApi();
        String accessToken = ActivityUtils.checkedGetAccessToken(getContext());
        api.setAccessToken(accessToken);
        return api.getService();
    }

    @Override
    protected void addItemsPreTransform(Collection<? extends T> items) {
        mWaitTime = INITIAL_WAIT_TIME;
        super.addItemsPreTransform(items);
    }

    protected void handleError(
            final int offset,
            final int limit,
            final SpotifyError error,
            String objectDesc) {

        int status = error.getRetrofitError().getResponse().getStatus();
        if (status == 401) {
            mWaitTime = INITIAL_WAIT_TIME;
            onUnauthorizedErrorRenewAccessTokenAndRetryLoadItems(offset, limit, error, objectDesc);
        } else if (500 <= status && status < 600) {
            if (mWaitTime <= WAIT_TIME_LIMIT) {
                int waitTimeSeconds = (int) (0.5 + mWaitTime / 1000);
                Log.e("VFY", getClass().getSimpleName() + ": Server error, trying again in " + waitTimeSeconds + " s", error);
                Toast.makeText(getContext(), "Server error, retrying in " + waitTimeSeconds + " s", Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadItems(offset, limit);
                    }
                }, mWaitTime);
                mWaitTime *= 2;
            } else {
                Log.e("VFY", getClass().getSimpleName() + ": Server error [retry time limit reached]", error);
                reportError();
            }
        } else {
            mWaitTime = INITIAL_WAIT_TIME;
            Log.e("VFY", getClass().getSimpleName() + ": Error fetching", error);
            Toast.makeText(getContext(), "Error fetching " + objectDesc, Toast.LENGTH_SHORT).show();
            reportError();
        }
    }

    protected void onUnauthorizedErrorRenewAccessTokenAndRetryLoadItems(
            final int offset,
            final int limit,
            final SpotifyError error,
            String objectDesc) {
        checkArgument(error.getRetrofitError().getResponse().getStatus() == 401, "Must be a 401 error");

        Log.d("VFY", "Error 401 while retrieving " + objectDesc + ", renewing access token...");
        ApiUtils.renewToken(getContext(), new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                /* TODO: Handle infinite loops */
                Log.d("VFY", "Access token renewed, trying again");
                loadItems(offset, limit);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("VFY", "Error while trying to renew access token because of 401 error");
                Log.e("VFY", "- Renew error", t);
                Log.e("VFY", "- 401 error", error);
                reportError();
                throw Throwables.propagate(t);
            }
        });
    }
}
