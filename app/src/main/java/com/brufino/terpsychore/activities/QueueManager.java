package com.brufino.terpsychore.activities;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.network.SessionApi;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.player.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.*;

/* TODO: Fix when track is unavailable, queue status remains 'playing' but player status is not playing, and
 * TODO: track position is 0 as well, so when user presses next it triggers a changeTrack() event (which shouldn't)
 * TODO: and we receive an unusual long time to wait for the server to switch track (bc probably we receive this
 * TODO: result before posting the queue status) */
public class QueueManager {

    private static final String TRACK_URI_PREFIX = "spotify:track:";
    private static final int POSITION_PRECISION_FOR_ADJUST_IN_MS = 2000;
    private static final int POSITION_PRECISION_FOR_END_IN_MS = 100;

    private Set<QueueListener> mQueueListeners = new LinkedHashSet<>();
    private final Context mContext;
    private final SessionApi mSessionApi;
    private final PlayerManager mPlayerManager;
    private JsonObject mQueue;
    private int mSessionId;
    private boolean mHost;

    public QueueManager(Context context, PlayerManager playerManager) {
        mContext = context;
        mPlayerManager =  playerManager;
        mPlayerManager.setPlayerListener(mPlayerListener);
        mSessionApi = ApiUtils.createApi(SessionApi.class);
    }

    public void addQueueListener(QueueListener queueListener) {
        mQueueListeners.add(queueListener);
    }

    public void setHost(boolean host) {
        mHost = host;
    }

    public void setQueue(JsonObject queue) {
        annotateQueueTracks(queue);
        mQueue = queue;
        mSessionId = mQueue.get("session_id").getAsInt();
        for (QueueListener queueListener : mQueueListeners) {
            queueListener.onQueueChange(this, mQueue);
        }

        if (!mPlayerManager.canControl() && !mPlayerManager.isInitializing()) {
            // loadQueue() will be called in mPlayerListener.onPlayerInitialized()
            mPlayerManager.initialize();
        } else if (!mPlayerManager.isInitializing()) {
            loadQueue();
        }
    }

    private void annotateQueueTracks(JsonObject queue) {
        int currentTrackOrder = queue.get("current_track").getAsInt();
        JsonArray tracks = queue.get("tracks").getAsJsonArray();
        for (int i = 0; i < tracks.size(); i++) {
            JsonObject track = tracks.get(i).getAsJsonObject();
            track.addProperty("played_track", i < currentTrackOrder);
            track.addProperty("current_track", i == currentTrackOrder);
            track.addProperty("next_track", i == currentTrackOrder + 1);
        }
    }

    private final PlayerNotificationCallback mPlayerNotificationCallback = new PlayerNotificationCallback() {
        @Override
        public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
            String playerStatus = getTrackStatus(playerState);
            Log.d("VFY", "onPlaybackEvent(" + playerStatus + "): " + eventType + " " + playerState.positionInMs + " / " + playerState.durationInMs);
            // See https://github.com/spotify/android-sdk/issues/99 and search for END_OF_CONTEXT
            // It means there's nothing more to play
            boolean trackWasPlaying = mQueue != null && mQueue.get("track_status").getAsString().equals("playing");
            if (eventType == EventType.TRACK_CHANGED &&
                    trackWasPlaying &&
                    !playerState.playing &&
                    playerState.positionInMs == 0) {
                changeTrack(3);
            }
        }
        @Override
        public void onPlaybackError(ErrorType errorType, String s) {
            if (errorType == ErrorType.TRACK_UNAVAILABLE) {
                Toast.makeText(mContext, "Track unavailable", Toast.LENGTH_SHORT).show();
                // TODO: Edit canPlay() and canReplay() to disable them in this case
                // TODO: Fix? postQueueStatus("paused", 0, 0, true);
            }
        }
    };

    private void changeTrack(final int tries) {
        Log.d("VFY", "changeTrack(" + tries + " tries)");
        String userId = ActivityUtils.getUserId(mContext);
        mSessionApi.getQueue(userId, mSessionId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject queue = response.body();
                int currentTrackOrder = mQueue.get("current_track").getAsInt();
                int newTrackOrder = queue.get("current_track").getAsInt();
                if (newTrackOrder == currentTrackOrder && tries > 0) {
                    int duration = ApiUtils.getCurrentTrack(queue).get("duration").getAsInt();
                    int serverPosition = queue.get("track_position").getAsInt();
                    checkState(serverPosition < duration, "Track position in the server should be < its duration");

                    int delay = duration - serverPosition;
                    if (delay > 10000) {
                        Log.w("VFY", "  [WARN] Unusual long time to wait (" + delay + " ms)");
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            changeTrack(tries - 1);
                        }
                    }, delay);
                    Log.d("VFY", "  Track hash't changed in the server (order = " + newTrackOrder + "), scheduling " +
                            "another try in " + delay + "ms");
                } else {
                    if (tries <= 0) {
                        Log.d("VFY", "  Run out of tries, updating anyway...");
                    } else {
                        Log.d("VFY", "  Track changed in the server, updating...");
                    }
                    setQueue(queue);
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                /* TODO: No internet? */
                Log.e("VFY", "Error while fetching queue at end of track", t);
            }
        });

    }

    private final PlayerManager.PlayerListener mPlayerListener = new PlayerManager.PlayerListener() {
        @Override
        public void onPlayerInitialized(PlayerManager playerManager, Player player) {
            loadQueue();
            player.addPlayerNotificationCallback(mPlayerNotificationCallback);
        }
    };

    public void loadQueue() {
        JsonObject currentTrack = getCurrentTrack();
        if (currentTrack == null) {
            Log.d("VFY", "loadQueue(): currentTrack is null, aborting...");
            return;
        }
        String spotifyId = currentTrack.get("spotify_id").getAsString();
        final String trackStatus = mQueue.get("track_status").getAsString();
        final int trackPosition = mQueue.get("track_position").getAsInt();
        final String trackUri = TRACK_URI_PREFIX + spotifyId;

        // Log.d("VFY", "loadQueue():");
        // Log.d("VFY", "  trackUri = " + trackUri);
        // Log.d("VFY", "  trackStatus = " + trackStatus);
        // Log.d("VFY", "  trackPosition = " + trackPosition);

        getPlayer().getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                String playerStatus = getTrackStatus(playerState);

                // Log.d("VFY", "  onPlayerState():");
                // Log.d("VFY", "    trackUri = " + playerState.trackUri);
                // Log.d("VFY", "    playerStatus = " + playerStatus + " (" +playerState.positionInMs + " / " +
                //         playerState.durationInMs + ")");

                if (playerStatus.equals("paused")) {
                    if (trackStatus.equals("playing")) {
                        Log.d("VFY", "paused -> playing");
                        getPlayer().play(PlayConfig
                                .createFor(trackUri)
                                .withInitialPosition(trackPosition));
                    } else {
                        Log.d("VFY", "paused -> paused");
                        getPlayer().play(PlayConfig
                                .createFor(trackUri)
                                .withInitialPosition(trackPosition));
                        getPlayer().pause();
                    }
                } else { // Player is playing the track
                    if (playerState.trackUri.equals(trackUri)) {
                        if (trackStatus.equals("paused")) {
                            Log.d("VFY", "playing -> paused (same track)");
                            getPlayer().seekToPosition(trackPosition);
                            getPlayer().pause();
                        } else {
                            if (Math.abs(trackPosition - playerState.positionInMs) > POSITION_PRECISION_FOR_ADJUST_IN_MS) {
                                Log.d("VFY", "playing -> playing (same track): seek");
                                getPlayer().seekToPosition(trackPosition);
                            } else {
                                Log.d("VFY", "playing -> playing (same track): DON'T seek");
                            }
                        }
                    } else {
                        if (trackStatus.equals("playing")) {
                            Log.d("VFY", "playing -> playing (different track)");
                            getPlayer().play(PlayConfig
                                    .createFor(trackUri)
                                    .withInitialPosition(trackPosition));
                        } else {
                            Log.d("VFY", "playing -> paused (different track)");
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
        Log.d("VFY", "refreshQueue()");
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

    public void addTracks(String[] trackUris) {
        // Filter out the spotify prefix
        /* TODO: Put this someplace else? */
        Pattern pattern = Pattern.compile("^" + TRACK_URI_PREFIX);
        for (int i = 0; i < trackUris.length; i++) {
            trackUris[i] = pattern.matcher(trackUris[i]).replaceFirst("");
        }

        ApiUtils.postTracks(mContext, mSessionId, trackUris, new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                refreshQueue();
            }
            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("VFY", "Error adding tracks", t);
                Toast.makeText(mContext, "Error adding tracks, try again later", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public JsonObject getCurrentTrack() {
        return ApiUtils.getCurrentTrack(mQueue);
    }

    public JsonObject getNextTrack() {
        return ApiUtils.getNextTrack(mQueue);
    }

    public boolean isHost() {
        return mHost;
    }

    public boolean canPlay() {
        return mPlayerManager.canControl() && isHost();
    }

    public boolean canReplay() {
        return mPlayerManager.canControl() && isHost();
    }

    public boolean canNext() {
        return mPlayerManager.canControl() && getNextTrack() != null && isHost();
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
                    postQueueStatus("paused", playerState.positionInMs, 0, false);
                } else {
                    getPlayer().resume();
                    postQueueStatus("playing", playerState.positionInMs, 0, false);
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
                int newPosition = Math.max(0,  playerState.positionInMs - rewindTimeInMs);
                getPlayer().seekToPosition(newPosition);
                postQueueStatus(getTrackStatus(playerState), newPosition, 0, false);
            }
        });
    }

    public void next() {
        if (!canNext()) {
            return;
        }
        JsonObject nextTrack = getNextTrack();
        final String trackUri = TRACK_URI_PREFIX + nextTrack.get("spotify_id").getAsString();
        getPlayer().getPlayerState(new PlayerStateCallback() {
            @Override
            public void onPlayerState(PlayerState playerState) {
                String status = getTrackStatus(playerState);
                getPlayer().play(PlayConfig
                        .createFor(trackUri)
                        .withInitialPosition(0));
                if (status.equals("paused")) {
                    getPlayer().pause();
                }
                // Will refresh after post, changing player state
                postQueueStatus(status, 0, 1, true);
            }
        });
    }

    private void postQueueStatus(
            String status,
            int positionInMs,
            int currentTrackOffset,
            final boolean refreshAfterPost) {
        int currentTrack = mQueue.get("current_track").getAsInt() + currentTrackOffset;

        JsonObject body = new JsonObject();
        body.addProperty("track_status", status);
        body.addProperty("current_track", currentTrack);
        body.addProperty("track_position", positionInMs);

        Log.d("VFY", "postQueueStatus(status = " + status + ", positionInMs = " + positionInMs + ", " +
                "currentTrackOffset = " + currentTrackOffset + ", refreshAfterPost = " + refreshAfterPost + ")");
        mSessionApi.postQueueStatus(mSessionId, body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (refreshAfterPost) {
                    refreshQueue();
                }
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e("VFY", "Error trying to post queue status", t);
                Toast.makeText(mContext, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Player getPlayer() {
        return mPlayerManager.getPlayer();
    }

    private static String getTrackStatus(PlayerState playerState) {
        return (playerState.playing) ? "playing" : "paused";
    }

    public static interface QueueListener {
        public void onQueueChange(QueueManager queueManager, JsonObject queue);
        public void onQueueRefreshError(QueueManager queueManager, Throwable t);
    }
}
