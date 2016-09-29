package com.brufino.terpsychore.activities;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.common.base.Throwables;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.player.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/* TODO: Absorb some Player methods and delegate them */
public class PlayerManager {

    private static final String SPOTIFY_CLIENT_ID = "69c5ec8781314e52ba8225e8a2d6a84f";
    private static final long UPDATE_INTERVAL_IN_MS = 80;

    private List<TrackProgressListener> mTrackProgressListeners = new LinkedList<>();
    private Player mPlayer;
    private Context mContext;
    private PlayerListener mPlayerListener;
    private volatile boolean mAlive = true; /* TODO: Analyse concurrency issues */
    private boolean mInitializing;

    public PlayerManager(Context context) {
        mContext = context;
    }

    public Player getPlayer() {
        return mPlayer;
    }

    public void setPlayerListener(PlayerListener playerListener) {
        mPlayerListener = playerListener;
    }

    public void addTrackUpdateListener(TrackProgressListener trackProgressListener) {
        mTrackProgressListeners.add(trackProgressListener);
    }

    private void notifyTrackUpdateListeners(boolean playing, int currentPositionInMs, int durationInMs) {
        for (TrackProgressListener listener : mTrackProgressListeners) {
            listener.onTrackProgressUpdate(playing, currentPositionInMs, durationInMs, this, mPlayer);
        }
    }

    public void initialize() {
        String accessToken = ActivityUtils.checkedGetAccessToken(mContext);
        // TODO: Check access token and renew before if needed, but also write code for failure since
        // TODO: the access token can expire between last check and actual use
        Config playerConfig = new Config(mContext, accessToken, SPOTIFY_CLIENT_ID);
        Spotify.getPlayer(playerConfig, this, mSpotifyPlayerInitializationObserver);
        mInitializing = true;
    }

    private Player.InitializationObserver mSpotifyPlayerInitializationObserver = new Player.InitializationObserver() {
        @Override
        public void onInitialized(Player player) {
            mPlayer = player;
            mPlayer.addConnectionStateCallback(mConnectionStateCallback);
            mPlayer.addPlayerNotificationCallback(mPlayerNotificationCallback);
            // Go to mConnectionStateCallback.onLoggedIn() or onLoginFailed()
        }
        @Override
        public void onError(Throwable throwable) {
            Log.e("VFY", "Error in player initialization: " + throwable.getClass().getSimpleName());
            Log.e("VFY", "Error in player initialization: " + throwable.getMessage());
            mInitializing = false;
        }
    };

    private ConnectionStateCallback mConnectionStateCallback = new ConnectionStateCallback() {

        @Override
        public void onLoggedIn() {
            Log.d("VFY", "Logged in");
            mInitializing = false;
            if (mPlayerListener != null) {
                mPlayerListener.onPlayerInitialized(PlayerManager.this, mPlayer);
            }
            new Handler().post(new TrackProgressUpdater(PlayerManager.this));
        }

        @Override
        public void onLoggedOut() {
            Log.d("VFY", "onLoggedOut()");
            mInitializing = false;
        }

        @Override
        public void onLoginFailed(final Throwable throwable) {
            Log.d("VFY", "Login failed, renewing access token and trying again");
            ApiUtils.renewToken(mContext, new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.body().get("status").getAsString().equals("already_fresh")) {
                        Log.e("VFY", "Logging error (tried renewing access token)", throwable);
                        mInitializing = false;
                        throw Throwables.propagate(throwable);
                    } else {
                        // It means it has renewed the token so let's try initializing the player again
                        Log.d("VFY", "Trying destroy then initialize the player again");
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                Log.d("VFY", "Destroying player...");
                                try {
                                    Spotify.awaitDestroyPlayer(PlayerManager.this, 5, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    Log.e("VFY", "Error while destroying player before recreating it", e);
                                }
                                Log.d("VFY", "Player destroyed");
                                return null;
                            }
                            @Override
                            protected void onPostExecute(Void aVoid) {
                                Log.d("VFY", "Initializing player...");
                                initialize();
                            }
                        }.execute();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("VFY", "Error while trying to renew access token because of login error");
                    Log.e("VFY", "- Renew error", t);
                    Log.e("VFY", "- Login error", throwable);
                    mInitializing = false;
                    throw Throwables.propagate(t);
                }
            });
        }

        @Override
        public void onTemporaryError() {
            Log.d("VFY", "onTemporaryError()");
            mInitializing = false;
        }

        @Override
        public void onConnectionMessage(String message) {
            Log.d("VFY", "onConnectionMessage(): message = " + message);
        }
    };

    private PlayerNotificationCallback mPlayerNotificationCallback = new PlayerNotificationCallback() {
        @Override
        public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
            // Apparently we can only reliably seekToPosition() after an AUDIO_FLUSH event
            // See https://github.com/spotify/android-sdk/issues/12
            if (eventType == EventType.AUDIO_FLUSH &&
                    // playerState.durationInMs > 0 &&
                    !mPlayer.isShutdown()) {
                // mPlayer.seekToPosition(mCurrentPosition);
                // mSeekPosition = false;
            }
            // Log.d("VFY", "onPlaybackEvent(): " + eventType.name());
        }

        @Override
        public void onPlaybackError(ErrorType errorType, String s) {
            Log.d("VFY", "onPlaybackError(): " + errorType.name() + " - " + s);
        }
    };

    public void onDestroy() {
        Spotify.destroyPlayer(this);
        mAlive = false;
    }

    public boolean isInitializing() {
        return mInitializing;
    }

    public boolean canControl() {
        // (!mInitializing) can still be false (in which case we return false) while the other conditions are true
        // because the player can become initialized before onLoggedIn(), which is where we set mInitializing = false.
        return mPlayer != null && mPlayer.isInitialized() && !mInitializing;
    }

    public static interface PlayerListener {
        public void onPlayerInitialized(PlayerManager playerManager, Player player);
        /* TODO: onError! */
    }

    public static interface TrackProgressListener {
        public void onTrackProgressUpdate(
                boolean playing,
                int currentPositionInMs,
                int durationInMs,
                PlayerManager playerManager,
                Player player);
    }

    private static class TrackProgressUpdater implements Runnable, PlayerStateCallback {

        private final WeakReference<PlayerManager> mPlayerManagerRef;

        public TrackProgressUpdater(PlayerManager playerManager) {
            mPlayerManagerRef = new WeakReference<>(playerManager);
        }

        @Override
        public void run() {
            PlayerManager playerManager = mPlayerManagerRef.get();
            // Make sure cycle doesn't continue if playerManager is destroyed
            if (playerManager != null && playerManager.mAlive && !playerManager.mPlayer.isShutdown()) {
                playerManager.mPlayer.getPlayerState(this);
            }
        }

        @Override
        public void onPlayerState(PlayerState playerState) {
            PlayerManager playerManager = mPlayerManagerRef.get();
            if (playerManager != null && playerManager.mAlive) {
                if (playerState.durationInMs > 0) {
                    playerManager.notifyTrackUpdateListeners(
                            playerState.playing,
                            playerState.positionInMs,
                            playerState.durationInMs);
                }
                new Handler().postDelayed(this, UPDATE_INTERVAL_IN_MS);
            }
        }
    }
}
