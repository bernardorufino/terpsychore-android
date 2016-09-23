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

/* TODO: Separate in different instances of fragments to support backstack (maybe just call new Fragment in activity) */
public class MusicPickerListFragment extends Fragment {

    public static final String ARG_CONTENT_TYPE = "itemType";
    public static final String ARG_USER_ID = "userId";
    public static final String ARG_PLAYLIST_ID = "playlistId";
    public static final String ARG_ALBUM_ID = "albumId";
    public static final String ARG_ALBUM = "album";
    public static final String ARG_TITLE = "title";
    private static final String SAVED_STATE_KEY_PARAMETERS = "parameters";

    private MusicPickerList vMusicList;
    private SpotifyRemoteAdapter<?> mAdapter;

    public static MusicPickerListFragment create(ContentType contentType) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_CONTENT_TYPE, contentType);
        MusicPickerListFragment fragment = new MusicPickerListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_music_picker_list, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentType contentType = (ContentType) getArguments().getSerializable(ARG_CONTENT_TYPE);
        mAdapter = selectAdapter(contentType);
        mAdapter.setFragment(this);
        mAdapter.setArguments(getArguments());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("VFY", "MusicPickerListFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
        //noinspection unchecked
        vMusicList = (MusicPickerList) getView().findViewById(R.id.music_picker_list);

        ContentType contentType = (ContentType) getArguments().getSerializable(ARG_CONTENT_TYPE);
        vMusicList.setAdapter(mAdapter);
        if (contentType.resetAdapter) {
            mAdapter.reset();
        }
        if (mAdapter.getItemCount() == 0) {
            mAdapter.firstLoad();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getTitle());
    }

    private String getTitle() {
        /* TODO: String resources! */
        ContentType contentType = (ContentType) getArguments().getSerializable(ARG_CONTENT_TYPE);
        switch (contentType) {
            case ALBUMS:
                return "Albums";
            case SONGS:
                return "Songs";
            case PLAYLISTS:
                return "Playlists";
            case ALBUM_SONGS:
            case PLAYLIST_SONGS:
                return getArguments().getString(ARG_TITLE);
            default:
                throw new AssertionError("Unknown content type");
        }
    }

    public MusicPickerActivity getMusicPickerActivity() {
        /* TODO: Don't cast, create interface, implement it in the activity and use it */
        return (MusicPickerActivity) getActivity();
    }

    public SpotifyRemoteAdapter<?> selectAdapter(ContentType contentType) {
        switch (contentType) {
            case PLAYLISTS: return new PlaylistsAdapter();
            case SONGS: return new SongsAdapter();
            case ALBUMS: return new AlbumsAdapter();
            case PLAYLIST_SONGS: return new PlaylistsSongsAdapter();
            case ALBUM_SONGS: return new AlbumSongsAdapter();
            default: throw new AssertionError("Unknown content type");
        }
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
