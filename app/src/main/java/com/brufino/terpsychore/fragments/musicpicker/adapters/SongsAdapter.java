package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.util.Log;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import kaaes.spotify.webapi.android.SpotifyCallback;
import kaaes.spotify.webapi.android.SpotifyError;
import kaaes.spotify.webapi.android.models.*;
import retrofit.client.Response;

import java.util.List;

public class SongsAdapter extends SpotifyRemoteAdapter<SavedTrack> {

    public static MusicPickerList.Item transformTrack(Track track, AlbumSimple album) {
        String title = track.name;
        StringBuilder artists = new StringBuilder(track.artists.get(0).name);
        for (ArtistSimple artist : Iterables.skip(track.artists, 1)) {
            artists.append(", ").append(artist.name);
        }
        String description = artists.toString();
        List<Image> images = (album != null) ? album.images : null;
        String imageUrl = (images != null && images.size() > 0) ? images.get(0).url : null;
        return new MusicPickerList.Item(title, description, imageUrl);
    }

    @Override
    public MusicPickerList.Item transform(SavedTrack item) {
        MusicPickerList.Item musicPickerItem = transformTrack(item.track, item.track.album);
        musicPickerItem.type = MusicPickerListFragment.ContentType.SONGS;
        musicPickerItem.data = item;
        musicPickerItem.selected = getActivity().isTrackSelected(item.track.uri);
        return musicPickerItem;
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

    @Override
    protected void onItemClickListener(
            MusicPickerList.MusicPickerListItemHolder holder,
            int position,
            MusicPickerList.Item item) {
        SavedTrack savedTrack = (SavedTrack) item.data;
        onTrackClick(savedTrack.track, item);
    }
}
