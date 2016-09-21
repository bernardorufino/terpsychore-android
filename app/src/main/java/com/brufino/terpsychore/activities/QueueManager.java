package com.brufino.terpsychore.activities;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.player.PlayConfig;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.LinkedHashSet;
import java.util.Set;

public class QueueManager {

    private Set<QueueListener> mQueueListeners = new LinkedHashSet<>();
    private final Context mContext;
    private final SessionApi mSessionApi;
    private final PlayerManager mPlayerManager;
    private JsonObject mQueue;
    private int mSessionId;

    public QueueManager(Context context, PlayerManager playerManager) {
        mContext = context;
        mPlayerManager =  playerManager;
        mPlayerManager.setPlayerListener(mPlayerListener);
        mSessionApi = ApiUtils.createApi(SessionApi.class);
    }

    public void addQueueListener(QueueListener queueListener) {
        mQueueListeners.add(queueListener);
    }

    public void setQueue(JsonObject queue) {
        mQueue = queue;
        mSessionId = queue.get("session_id").getAsInt();
        for (QueueListener queueListener : mQueueListeners) {
            queueListener.onQueueChange(this, queue);
        }

        if (!mPlayerManager.canControl() && !mPlayerManager.isInitializing()) {
            // loadQueue() will be called in mPlayerListener.onPlayerInitialized()
            mPlayerManager.initialize();
        } else if (!mPlayerManager.isInitializing()) {
            loadQueue();
        }
    }

    private final PlayerManager.PlayerListener mPlayerListener = new PlayerManager.PlayerListener() {
        @Override
        public void onPlayerInitialized(PlayerManager playerManager, Player player) {
            loadQueue();
        }
    };

    // private static final int POSITION_PRECISION_IN_MS = 1500;
    private static final int POSITION_PRECISION_IN_MS = 2000;

    public void loadQueue() {
        JsonObject currentTrack = getCurrentTrack();
        String spotifyId = currentTrack.get("spotify_id").getAsString();
        final String trackStatus = mQueue.get("track_status").getAsString();
        final int trackPosition = mQueue.get("track_position").getAsInt();
        final String trackUri = "spotify:track:" + spotifyId;

        Log.d("VFY", "loadQueue():");
        Log.d("VFY", "  trackUri = " + trackUri);
        Log.d("VFY", "  trackStatus = " + trackStatus);
        Log.d("VFY", "  trackPosition = " + trackPosition);

        getPlayer().getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                String playerStatus = (playerState.playing) ? "playing" : "paused";

                Log.d("VFY", "onPlayerState():");
                Log.d("VFY", "  trackUri = " + playerState.trackUri);
                Log.d("VFY", "  playerStatus = " + playerStatus);
                Log.d("VFY", "  positionInMs = " + playerState.positionInMs);
                Log.d("VFY", "  durationInMs = " + playerState.durationInMs);

                if (playerStatus.equals("paused")) {
                    if (trackStatus.equals("playing")) {
                        getPlayer().play(PlayConfig
                                .createFor(trackUri)
                                .withInitialPosition(trackPosition));
                    } else {
                        getPlayer().play(PlayConfig
                                .createFor(trackUri)
                                .withInitialPosition(trackPosition));
                        getPlayer().pause();
                    }
                } else { // Player is playing the track
                    if (playerState.trackUri.equals(trackUri)) {
                        if (trackStatus.equals("paused")) {
                            getPlayer().seekToPosition(trackPosition);
                            getPlayer().pause();
                        } else {
                            if (Math.abs(trackPosition - playerState.positionInMs) > POSITION_PRECISION_IN_MS) {
                                getPlayer().seekToPosition(trackPosition);
                            }
                        }
                    } else {
                        if (trackStatus.equals("playing")) {
                            getPlayer().play(PlayConfig
                                    .createFor(trackUri)
                                    .withInitialPosition(trackPosition));
                        } else {
                            getPlayer().play(PlayConfig
                                    .createFor(trackUri)
                                    .withInitialPosition(trackPosition));
                            getPlayer().pause();
                        }
                    }
                }
            }
        });
    }

    public void refreshQueue() {
        String userId = ActivityUtils.getUserId(mContext);
        mSessionApi.getQueue(userId, mSessionId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject queue = response.body();
                setQueue(queue);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("VFY", "Error while refreshing queue", t);
                for (QueueListener queueListener : mQueueListeners) {
                    queueListener.onQueueRefreshError(QueueManager.this, t);
                }
            }
        });
    }

    public JsonObject getCurrentTrack() {
        return ApiUtils.getCurrentTrack(mQueue);
    }

    public JsonObject getNextTrack() {
        return ApiUtils.getNextTrack(mQueue);
    }

    public boolean canPlay() {
        return mPlayerManager.canControl();
    }

    public boolean canReplay() {
        return mPlayerManager.canControl();
    }

    public boolean canNext() {
        return getNextTrack() != null;
    }

    public void togglePlay() {
        if (!canPlay()) {
            return;
        }
        getPlayer().getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                if (playerState.playing) {
                    getPlayer().pause();
                } else {
                    getPlayer().resume();
                }
            }
        });
    }

    public void replay(final int rewindTimeInMs) {
        if (!canReplay()) {
            return;
        }
        getPlayer().getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                int newPosition = Math.max(0, playerState.positionInMs + rewindTimeInMs);
                getPlayer().seekToPosition(newPosition);
            }
        });
    }

    public void next() {
        if (!canNext()) {
            return;
        }
        Toast.makeText(mContext, "TODO: Implement!", Toast.LENGTH_SHORT).show();
    }

    private Player getPlayer() {
        return mPlayerManager.getPlayer();
    }

    public static interface QueueListener {
        public void onQueueChange(QueueManager queueManager, JsonObject queue);
        public void onQueueRefreshError(QueueManager queueManager, Throwable t);
    }
}
