package com.brufino.terpsychore.fragments.musicpicker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.MusicPickerActivity;
import com.brufino.terpsychore.fragments.musicpicker.adapters.AlbumsAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.SpotifyRemoteAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.PlaylistsAdapter;
import com.brufino.terpsychore.fragments.musicpicker.adapters.SongsAdapter;
import com.brufino.terpsychore.view.trackview.MusicPickerList;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class MusicPickerListFragment extends Fragment {

    public static final String PARAM_ITEM_TYPE = "itemType";
    private static final String SAVED_STATE_KEY_PARAMETERS = "parameters";

    private MusicPickerList vMusicList;

    private Map<ContentType, SpotifyRemoteAdapter<?>> mAdapters = new HashMap<>();
    private ContentType mContentType;
    private SpotifyRemoteAdapter<?> mAdapter;
    private Bundle mParameters;

    public MusicPickerListFragment() {
        mAdapters.put(ContentType.PLAYLISTS, new PlaylistsAdapter());
        mAdapters.put(ContentType.SONGS, new SongsAdapter());
        mAdapters.put(ContentType.ALBUMS, new AlbumsAdapter());
        for (SpotifyRemoteAdapter<?> adapter : mAdapters.values()) {
            adapter.setFragment(this);
        }
    }

    public MusicPickerActivity getMusicPickerActivity() {
        /* TODO: Don't cast, create interface, implement it in the activity and use it */
        return (MusicPickerActivity) getActivity();
    }

    /**
     * Because we want to reuse this fragment
     */
    public void setParameters(Bundle parameters) {
        mParameters = parameters;
    }

    public void refresh() {
        ContentType contentType = (ContentType) mParameters.getSerializable(PARAM_ITEM_TYPE);
        mAdapter = mAdapters.get(contentType);
        if (vMusicList != null) {
            initializeAdapter();
        }
    }

    private void initializeAdapter() {
        vMusicList.setAdapter(mAdapter);
        mAdapter.setParameters(mParameters);
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

        if (savedInstanceState != null) {
            Bundle parameters = savedInstanceState.getBundle(SAVED_STATE_KEY_PARAMETERS);
            setParameters(parameters);
            refresh();
        } else {
            checkNotNull(mAdapter, "Should call setParameters() before attaching this fragment");
            initializeAdapter();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(SAVED_STATE_KEY_PARAMETERS, mParameters);
    }

    public static enum ContentType {
        PLAYLISTS, SONGS, ALBUMS
    }
}
