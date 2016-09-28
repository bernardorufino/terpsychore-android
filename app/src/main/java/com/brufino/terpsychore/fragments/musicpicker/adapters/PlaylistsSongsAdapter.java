package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.os.Bundle;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import retrofit.client.Response;

public class PlaylistsSongsAdapter extends SpotifyRemoteAdapter<PlaylistTrack> {

    @Override
    public MusicPickerList.Item transform(PlaylistTrack item) {
        MusicPickerList.Item musicPickerItem = SongsAdapter.transformTrack(item.track, item.track.album);
        musicPickerItem.type = MusicPickerListFragment.ContentType.PLAYLIST_SONGS;
        musicPickerItem.data = item;
        musicPickerItem.selected = getActivity().isTrackSelected(item.track.uri);
        return musicPickerItem;
    }

    @Override
    protected void loadItems(final int offset, final int limit) {
        super.loadItems(offset, limit);
        Bundle params = getParameters();
        String playlistId = params.getString(MusicPickerListFragment.ARG_PLAYLIST_ID);
        String userId = params.getString(MusicPickerListFragment.ARG_USER_ID);
        getSpotifyService().getPlaylistTracks(
                userId,
                playlistId,
                new ImmutableMap.Builder<String, Object>()
                        .put("offset", offset)
                        .put("limit", limit)
                        .build(),
                new SpotifyCallback<Pager<PlaylistTrack>>() {
                    @Override
                    public void success(Pager<PlaylistTrack> playlistTrackPager, Response response) {
                        addItemsPreTransform(playlistTrackPager.items);
                    }
                    @Override
                    public void failure(SpotifyError error) {
                        handleError(offset, limit, error, "songs");
                    }
                });
    }

    @Override
    protected void onItemClick(
            MusicPickerList.MusicPickerListItemHolder holder,
            int position,
            MusicPickerList.Item item) {
        PlaylistTrack track = (PlaylistTrack) item.data;
        onTrackClick(track.track, item);
    }
}
