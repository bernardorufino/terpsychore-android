package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment.ContentType;
import com.brufino.terpsychore.fragments.musicpicker.SearchFragment;
import kaaes.spotify.webapi.android.models.Track;

import java.util.HashMap;
import java.util.Map;

public class MusicPickerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String RESULT_TRACK_URIS = "trackUris";

    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;
    private Toolbar vToolbar;
    private TextView vHeaderUserName;
    private TextView vSelectionStatus;
    private FrameLayout vMusicContent;
    private ViewGroup vSelection;
    private FrameLayout vDoneButton;

    private MusicPickerListFragment mMusicPickerListFragment;
    private Map<String, Track> mSelectedTrackUris = new HashMap<>();
    private MusicPickerListFragment mSongsFragment;
    private MusicPickerListFragment mAlbumsFragment;
    private MusicPickerListFragment mPlaylistsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_picker);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        vDrawer = (DrawerLayout) findViewById(R.id.music_picker_drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, vDrawer, vToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        vDrawer.addDrawerListener(toggle);
        toggle.syncState();
        vNavigationView = (NavigationView) findViewById(R.id.nav_view);
        vNavigationView.setNavigationItemSelectedListener(this);
        vHeaderUserName = (TextView) findViewById(R.id.music_picker_nav_header_user_name);
        vMusicContent = (FrameLayout) findViewById(R.id.music_picker_content);
        vSelectionStatus = (TextView) findViewById(R.id.music_picker_selection_status);
        vSelection = (ViewGroup) findViewById(R.id.music_picker_selection);
        vDoneButton = (FrameLayout) findViewById(R.id.music_picker_selection_done);
        vDoneButton.setOnClickListener(mOnDoneButtonClickListener);

        mSongsFragment = MusicPickerListFragment.create(ContentType.SONGS);
        mAlbumsFragment = MusicPickerListFragment.create(ContentType.ALBUMS);
        mPlaylistsFragment = MusicPickerListFragment.create(ContentType.PLAYLISTS);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.music_picker_drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.spotify_music_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Bundle params = new Bundle();
        Fragment fragment;
        int id = item.getItemId();
        switch (id) {
            case R.id.music_picker_item_search:
                Toast.makeText(MusicPickerActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                fragment = new SearchFragment();
                break;
            case R.id.music_picker_item_playlists:
                fragment = mPlaylistsFragment;
                break;
            case R.id.music_picker_item_songs:
                fragment = mSongsFragment;
                break;
            case R.id.music_picker_item_albums:
                fragment = mAlbumsFragment;
                break;
            default:
                throw new AssertionError("Unknown id");
        }

         getSupportFragmentManager()
                 .beginTransaction()
                 .replace(R.id.music_picker_content, fragment)
                 .addToBackStack(null)
                 .commit();

        vDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void setTitle(CharSequence title) {
        vToolbar.setTitle(title);
    }

    public void showMusicPickerListFragment(Bundle args) {
        MusicPickerListFragment fragment = new MusicPickerListFragment();
        fragment.setArguments(args);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.music_picker_content, fragment)
                .addToBackStack(null)
                .commit();
    }

    public boolean isTrackSelected(String trackUri) {
        return mSelectedTrackUris.containsKey(trackUri);
    }

    public void setTrackSelected(String trackUri, Track track, boolean selected) {
        if (selected) {
            mSelectedTrackUris.put(trackUri, track);
        } else {
            mSelectedTrackUris.remove(trackUri);
        }
        if (mSelectedTrackUris.size() == 0) {
            vSelection.setVisibility(View.GONE);
        } else {
            vSelection.setVisibility(View.VISIBLE);
            vSelectionStatus.setText(mSelectedTrackUris.size() + " tracks selected");
        }
    }

    private View.OnClickListener mOnDoneButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSelectedTrackUris.isEmpty()) {
                setResult(RESULT_CANCELED);
            } else {
                Intent result = new Intent();
                String[] trackUris = mSelectedTrackUris.keySet().toArray(new String[mSelectedTrackUris.size()]);
                result.putExtra(RESULT_TRACK_URIS, trackUris);
                setResult(RESULT_OK, result);
            }
            finish();
        }
    };
}
