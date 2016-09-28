package com.brufino.terpsychore.fragments.musicpicker.adapters;

import android.os.Bundle;
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

public class AlbumsAdapter extends SpotifyRemoteAdapter<SavedAlbum> {

    @Override
    public MusicPickerList.Item transform(SavedAlbum item) {
        String title = item.album.name;
        StringBuilder artists = new StringBuilder(item.album.artists.get(0).name);
        for (ArtistSimple artist : Iterables.skip(item.album.artists, 1)) {
            artists.append(", ").append(artist.name);
        }
        String description = artists.toString();
        List<Image> images = item.album.images;
        String imageUrl = (images.size() > 0) ? images.get(0).url : null;
        MusicPickerList.Item musicPickerItem = new MusicPickerList.Item(title, description, imageUrl);
        musicPickerItem.type = MusicPickerListFragment.ContentType.ALBUMS;
        musicPickerItem.data = item;
        return musicPickerItem;
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

    @Override
    protected void onItemClick(
            MusicPickerList.MusicPickerListItemHolder holder,
            int position,
            MusicPickerList.Item item) {
        SavedAlbum album = (SavedAlbum) item.data;
        Bundle args = new Bundle();
        args.putSerializable(
                MusicPickerListFragment.ARG_CONTENT_TYPE,
                MusicPickerListFragment.ContentType.ALBUM_SONGS);
        args.putString(MusicPickerListFragment.ARG_ALBUM_ID, album.album.id);
        args.putParcelable(MusicPickerListFragment.ARG_ALBUM,  album.album);
        args.putString(MusicPickerListFragment.ARG_TITLE, item.title);
        getActivity().showMusicPickerListFragment(args);
    }
}
