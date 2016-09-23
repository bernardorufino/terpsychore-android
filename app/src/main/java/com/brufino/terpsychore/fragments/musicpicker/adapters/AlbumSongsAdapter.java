package com.brufino.terpsychore.fragments.musicpicker.adapters;

import com.brufino.terpsychore.view.trackview.MusicPickerList;

public class AlbumSongsAdapter extends SpotifyRemoteAdapter<Object> {

    @Override
    public MusicPickerList.Item transform(Object item) {
        return null;
    }

    @Override
    protected void onItemClickListener(
            MusicPickerList.MusicPickerListItemHolder holder,
            int position,
            MusicPickerList.Item item) {

    }
}