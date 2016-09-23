package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.util.Log;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedTrack;
import retrofit.client.Response;

public class SongsAdapter extends SpotifyRemoteAdapter<SavedTrack> {

    @Override
    public MusicPickerList.Item transform(SavedTrack item) {
        String title = item.track.name;
        StringBuilder artists = new StringBuilder(item.track.artists.get(0).name);
        for (ArtistSimple artist : Iterables.skip(item.track.artists, 1)) {
            artists.append(", ").append(artist.name);
        }
        String description = artists.toString();
        return new MusicPickerList.Item(title, description, null);
    }

    @Override
    protected void loadItems(final int offset, final int limit) {
        Log.d("VFY", "SongsAdapter.loadItems(" + offset + ", " + limit + ")");
        super.loadItems(offset, limit);
        getSpotifyService().getMySavedTracks(
                new ImmutableMap.Builder<String, Object>()
                        .put("offset", offset)
                        .put("limit", limit)
                        .build(),
                new SpotifyCallback<Pager<SavedTrack>>() {
                    @Override
                    public void success(Pager<SavedTrack> savedTrackPager, Response response) {
                        addItemsPreTransform(savedTrackPager.items);
                    }
                    @Override
                    public void failure(SpotifyError error) {
                        handleError(offset, limit, error, "songs");
                    }
                });
    }
}
