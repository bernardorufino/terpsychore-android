package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.os.Bundle;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.client.Response;

/* TODO: Fix, not scrolling */
public class AlbumSongsAdapter extends SpotifyRemoteAdapter<Track> {

    @Override
    public MusicPickerList.Item transform(Track item) {
        Album album = getParameters().getParcelable(MusicPickerListFragment.ARG_ALBUM);
        MusicPickerList.Item musicPickerItem = SongsAdapter.transformTrack(item, album);
        musicPickerItem.type = MusicPickerListFragment.ContentType.ALBUM_SONGS;
        musicPickerItem.data = item;
        musicPickerItem.selected = getActivity().isTrackSelected(item.uri);
        return musicPickerItem;
    }

    @Override
    protected void loadItems(final int offset, final int limit) {
        super.loadItems(offset, limit);
        Bundle params = getParameters();
        String albumId = params.getString(MusicPickerListFragment.ARG_ALBUM_ID);
        getSpotifyService().getAlbumTracks(
                albumId,
                new ImmutableMap.Builder<String, Object>()
                        .put("offset", offset)
                        .put("limit", limit)
                        .build(),
                new SpotifyCallback<Pager<Track>>() {
                    @Override
                    public void success(Pager<Track> trackPager, Response response) {
                        addItemsPreTransform(trackPager.items);
                    }
                    @Override
                    public void failure(SpotifyError error) {
                        handleError(offset, limit, error, "songs");
                    }
                }
        );
    }

    @Override
    protected void onItemClickListener(
            MusicPickerList.MusicPickerListItemHolder holder,
            int position,
            MusicPickerList.Item item) {
        Track track = (Track) item.data;
        onTrackClick(track, item);
    }
}