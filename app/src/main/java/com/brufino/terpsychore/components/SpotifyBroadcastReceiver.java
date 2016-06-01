package com.brufino.terpsychore.components;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.MainActivity;
import com.brufino.terpsychore.activities.SessionActivity;

import static com.google.common.base.Preconditions.checkNotNull;

public class SpotifyBroadcastReceiver extends BroadcastReceiver {

    private static final String SHARED_PREFERENCES_FILE = MainActivity.class.getPackage().toString() + "_preferences";

    private static final String CURRENT_TRACK_ID_KEY = "currentTrackId";
    private static final String CURRENT_TRACK_NAME_KEY = "currentTrackName";
    private static final String CURRENT_TRACK_ARTIST_KEY = "currentTrackArtist";
    private static final String CURRENT_TRACK_ALBUM_KEY = "currentTrackAlbum";
    private static final String CURRENT_TRACK_LENGTH_KEY = "currentTrackLength";

    private static final class BroadcastTypes {
        public static final String SPOTIFY_PACKAGE = "com.spotify.music";
        public static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        public static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        public static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // long timeSentInMs = intent.getLongExtra("timeSent", 0L);
        // long timeElapsedInMs = System.currentTimeMillis() - timeSentInMs;
        String action = intent.getAction();

        String trackId, trackArtist, trackAlbum, trackName;
        int trackLength;
        SharedPreferences preferences;
        switch (action) {
            case BroadcastTypes.METADATA_CHANGED:
                preferences = context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

                trackId = intent.getStringExtra("id");
                trackName = intent.getStringExtra("track");
                trackArtist = intent.getStringExtra("artist");
                trackAlbum = intent.getStringExtra("album");
                trackLength = intent.getIntExtra("length", 0);

                // Using commit() instead of apply() because otherwise the process might have been killed before
                // writing to disk.
                // See https://developer.android.com/reference/android/content/BroadcastReceiver.html#ReceiverLifecycle
                preferences
                        .edit()
                        .putString(CURRENT_TRACK_ID_KEY, trackId)
                        .putString(CURRENT_TRACK_NAME_KEY, trackName)
                        .putString(CURRENT_TRACK_ARTIST_KEY, trackArtist)
                        .putString(CURRENT_TRACK_ALBUM_KEY, trackAlbum)
                        .putInt(CURRENT_TRACK_LENGTH_KEY, trackLength)
                        .commit();

                generateNotification(context, trackId, trackName, trackArtist);
                break;
            case BroadcastTypes.PLAYBACK_STATE_CHANGED:
                preferences = context.getSharedPreferences(SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

                /* TODO: Check for edge case where application is killed (process stopped), song is skipped,
                   TODO: application comes back to life and then song is played. Shared preferences will have
                   TODO: the outdated song in the records. Using memory instead of disk might be safer. At least we
                   TODO: won't confuse invalid state with a valid one. However, we can't ensure that we'll have the
                   TODO: current song playing most of the time (since the app can be killed between onReceive calls).
                   TODO: TODO is to analyse this last case, I strongly believe that we can rely on memory most of the
                   TODO: times. Please exhaust the possibility of Spotify allowing us to fetch current song playing
                   TODO: before, since this would solve all our problems, instead of having to keep track of the
                   TODO: player's state */
                trackId = preferences.getString(CURRENT_TRACK_ID_KEY, null);
                if (trackId != null) {
                    trackName = checkNotNull(preferences.getString(CURRENT_TRACK_NAME_KEY, null));
                    trackArtist = checkNotNull(preferences.getString(CURRENT_TRACK_ARTIST_KEY, null));
                    generateNotification(context, trackId, trackName, trackArtist);
                }
                break;
            case BroadcastTypes.QUEUE_CHANGED:
                /* No-op */
                break;
        }
    }

    private Notification getNotification(Context context, String trackId, String trackName, String trackArtist) {
        Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.ic_logo_circle)).getBitmap();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logo_circle)
                .setContentTitle(trackName)
                .setContentText("Enter Vibefy Session")
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setAutoCancel(true)
                .setShowWhen(false)
                .setNumber(234)
                .setSound(null);

        Intent intent = new Intent(context, SessionActivity.class);
        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, "12");
        intent.putExtra(SessionActivity.TRACK_ID_EXTRA_KEY, trackId);
        intent.putExtra(SessionActivity.TRACK_NAME_EXTRA_KEY, trackName);
        intent.putExtra(SessionActivity.TRACK_ARTIST_EXTRA_KEY, trackArtist);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    private void generateNotification(Context context, String trackId, String trackName, String trackArtist) {
        Notification notification = getNotification(context, trackId, trackName, trackArtist);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);
    }
}
