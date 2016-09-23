package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.util.Log;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.ArtistSimple;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.SavedAlbum;
import retrofit.client.Response;

public class AlbumsAdapter extends SpotifyRemoteAdapter<SavedAlbum> {

    @Override
    public MusicPickerList.Item transform(SavedAlbum item) {
        String title = item.album.name;
        StringBuilder artists = new StringBuilder(item.album.artists.get(0).name);
        for (ArtistSimple artist : Iterables.skip(item.album.artists, 1)) {
            artists.append(", ").append(artist.name);
        }
        String description = artists.toString();
        return new MusicPickerList.Item(title, description, null);
    }

    @Override
    protected void loadItems(final int offset, final int limit) {
        Log.d("VFY", "AlbumsAdapter.loadItems(" + offset + ", " + limit + ")");
        super.loadItems(offset, limit);
        getSpotifyService().getMySavedAlbums(
                new ImmutableMap.Builder<String, Object>()
                        .put("offset", offset)
                        .put("limit", limit)
                        .build(),
                new SpotifyCallback<Pager<SavedAlbum>>() {
                    @Override
                    public void success(Pager<SavedAlbum> savedAlbumPager, Response response) {
                        addItemsPreTransform(savedAlbumPager.items);
                    }
                    @Override
                    public void failure(SpotifyError error) {
                        handleError(offset, limit, error, "albums");
                    }
                });
    }
}
