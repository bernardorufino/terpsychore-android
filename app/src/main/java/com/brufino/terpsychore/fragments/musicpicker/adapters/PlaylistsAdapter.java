package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.util.Log;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import retrofit.client.Response;

public class PlaylistsAdapter extends SpotifyRemoteAdapter<PlaylistSimple> {

    @Override
    public MusicPickerList.Item transform(PlaylistSimple item) {
        String title = item.name;
        String description = (item.owner.display_name != null) ? item.owner.display_name : "@" + item.owner.id;
        return new MusicPickerList.Item(item.name, description, null);
    }

    @Override
    protected void loadItems(final int offset, final int limit) {
        Log.d("VFY", "PlaylistsAdapter.loadItems(" + offset + ", " + limit + ")");
        super.loadItems(offset, limit);
        getSpotifyService().getMyPlaylists(
                new ImmutableMap.Builder<String, Object>()
                        .put("offset", offset)
                        .put("limit", limit)
                        .build(),
                new SpotifyCallback<Pager<PlaylistSimple>>() {
                    @Override
                    public void success(Pager<PlaylistSimple> playlistSimplePager, Response response) {
                        addItemsPreTransform(playlistSimplePager.items);
                    }
                    @Override
                    public void failure(final SpotifyError error) {
                        handleError(offset, limit, error, "playlists");
                    }
                });
    }
}
