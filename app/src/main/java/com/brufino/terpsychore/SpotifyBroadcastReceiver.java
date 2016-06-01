package com.brufino.terpsychore;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class SpotifyBroadcastReceiver extends BroadcastReceiver {

    private static final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long timeSentInMs = intent.getLongExtra("timeSent", 0L);
        long timeElapsedInMs = System.currentTimeMillis() - timeSentInMs;
        String action = intent.getAction();

        switch (action) {
            case BroadcastTypes.METADATA_CHANGED:
                String trackId = intent.getStringExtra("id");
                String trackArtist = intent.getStringExtra("artist");
                String trackAlbum = intent.getStringExtra("album");
                String trackName = intent.getStringExtra("track");
                int trackLengthInSec = intent.getIntExtra("length", 0);
                Log.d("VFY", "Track changed (notif sent " + timeElapsedInMs + " ms ago)");
                Log.d("VFY", "  Artist = " + trackArtist);
                Log.d("VFY", "  Name = " + trackName);
                Log.d("VFY", "  Album = " + trackAlbum);
                Log.d("VFY", "  Length = " + trackLengthInSec + " s");
                Log.d("VFY", "  Id = " + trackId);
                generateNotification(context, trackName, "Enter Vibefy Session");
                break;
            case BroadcastTypes.PLAYBACK_STATE_CHANGED:
                int positionInMs = intent.getIntExtra("playbackPosition", 0);
                Log.d("VFY", "Playback state changed (notif sent " + timeElapsedInMs + " ms ago)");
                Log.d("VFY", "  Position = " + positionInMs + " ms");
                break;
            case BroadcastTypes.QUEUE_CHANGED:
                Log.d("VFY", "Queue changed (notif sent " + timeElapsedInMs + " ms ago)");
                break;
        }
    }

    private Notification getNotification(Context context, String title, String description) {
        Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.ic_logo_circle)).getBitmap();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_logo_circle)
                .setContentTitle(title)
                .setContentText(description)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setAutoCancel(true)
                .setSound(null);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        return builder.build();
    }

    private void generateNotification(Context context, String title, String description) {
        Notification notification = getNotification(context, title, description);
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification);

    }
}
