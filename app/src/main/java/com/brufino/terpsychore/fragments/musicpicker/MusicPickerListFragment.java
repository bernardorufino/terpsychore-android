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
import com.brufino.terpsychore.fragments.musicpicker.adapters.*;
import com.brufino.terpsychore.view.trackview.MusicPickerList;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/* TODO: Separate in different instances of fragments to support backstack (maybe just call new Fragment in activity) */
public class MusicPickerListFragment extends Fragment {

    public static final String PARAM_CONTENT_TYPE = "itemType";
    public static final String PARAM_USER_ID = "userId";
    public static final String PARAM_PLAYLIST_ID = "playlistId";
    public static final String PARAM_ALBUM_ID = "albumId";
    public static final String PARAM_ALBUM = "album";
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
        mAdapters.put(ContentType.PLAYLIST_SONGS, new PlaylistsSongsAdapter());
        mAdapters.put(ContentType.ALBUM_SONGS, new AlbumSongsAdapter());
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
        mContentType = (ContentType) mParameters.getSerializable(PARAM_CONTENT_TYPE);
        mAdapter = mAdapters.get(mContentType);
        checkNotNull(mAdapter, "No adapter for content type provided");
        if (vMusicList != null) {
            initializeAdapter();
        }
    }

    private void initializeAdapter() {
        vMusicList.setAdapter(mAdapter);
        mAdapter.setParameters(mParameters);
        if (mContentType.resetAdapter) {
            mAdapter.reset();
        }
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

    /* TODO: Or just create a new fragment and handle cache properly */
    public static enum ContentType {
        PLAYLISTS(false),
        SONGS(false),
        ALBUMS(false),
        PLAYLIST_SONGS(true),
        ALBUM_SONGS(true);

        public boolean resetAdapter;

        ContentType(boolean resetAdapter) {
            this.resetAdapter = resetAdapter;
        }
    }
}
