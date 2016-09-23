package com.brufino.terpsychore.fragments.musicpicker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.musicpicker.adapters.AlbumsAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.SpotifyRemoteAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.PlaylistsAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.SongsAdapter;
import com.brufino.terpsychore.view.trackview.MusicPickerList;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MusicPickerListFragment extends Fragment {

    public static final String SAVED_STATE_KEY_ITEM_TYPE = "itemType";

    private MusicPickerList vMusicList;

    private Map<ItemType, SpotifyRemoteAdapter<?>> mAdapters = new HashMap<>();
    private ItemType mItemType;
    private SpotifyRemoteAdapter<?> mAdapter;

    public MusicPickerListFragment() {
        mAdapters.put(ItemType.PLAYLIST, new PlaylistsAdapter());
        mAdapters.put(ItemType.SONG, new SongsAdapter());
        mAdapters.put(ItemType.ALBUM, new AlbumsAdapter());
        for (SpotifyRemoteAdapter<?> adapter : mAdapters.values()) {
            adapter.setFragment(this);
        }
    }

    public void setItemType(ItemType itemType) {
        Log.d("VFY", "MusicPickerListFragment.setItemType()");
        mItemType = itemType;
        mAdapter = mAdapters.get(itemType);
        if (vMusicList != null) {
            initializeAdapter();
        }
    }

    private void initializeAdapter() {
        vMusicList.setAdapter(mAdapter);
        if (mAdapter.getItemCount() == 0) {
            mAdapter.firstLoad();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_picker_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("VFY", "MusicPickerListFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        //noinspection unchecked
        vMusicList = (MusicPickerList) getView().findViewById(R.id.music_picker_list);

        /* TODO: Handler rotation below, basically newly created fragment does not have mAdapter, watch for lifecycle */
        if (savedInstanceState != null) {
            ItemType itemType = (ItemType) savedInstanceState.getSerializable(SAVED_STATE_KEY_ITEM_TYPE);
            setItemType(itemType);
        } else {
            checkNotNull(mAdapter, "Should call setItemType() before attaching this fragment");
            initializeAdapter();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SAVED_STATE_KEY_ITEM_TYPE, mItemType);
    }

    public static enum ItemType {
        PLAYLIST, SONG, ALBUM
    }
}
