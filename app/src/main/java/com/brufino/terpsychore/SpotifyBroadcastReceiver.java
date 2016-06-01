package com.brufino.terpsychore;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
}
