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
import android.widget.ImageView;
import android.widget.TextView;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment.ContentType;
import com.brufino.terpsychore.fragments.musicpicker.SearchFragment;
import com.brufino.terpsychore.lib.CircleTransformation;
import com.brufino.terpsychore.util.ActivityUtils;
import com.google.common.collect.Lists;
import com.squareup.picasso.Picasso;
import kaaes.spotify.webapi.android.models.Track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MusicPickerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String RESULT_TRACK_URIS = "trackUris";

    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;
    private Toolbar vToolbar;
    private TextView vHeaderUserName;
    private TextView vSelectionStatus;
    private ViewGroup vSelection;
    private FrameLayout vSelectionDone;
    private FrameLayout vMusicContent;
    private ImageView vHeaderImage;

    private Map<String, Track> mSelectedTrackUris = new LinkedHashMap<>();
    private SearchFragment mSearchFragment;
    private MusicPickerListFragment mPlaylistsFragment;
    private MusicPickerListFragment mSongsFragment;
    private MusicPickerListFragment mAlbumsFragment;

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
        vHeaderImage = (ImageView) vNavigationView.getHeaderView(0).findViewById(R.id.music_picker_nav_header_image);
        vHeaderUserName = (TextView) vNavigationView.getHeaderView(0).findViewById(R.id.music_picker_nav_header_user_name);
        vHeaderUserName.setText(ActivityUtils.getDisplayName(this));
        vMusicContent = (FrameLayout) findViewById(R.id.music_picker_content);
        vSelectionStatus = (TextView) findViewById(R.id.picker_selection_status);
        vSelection = (ViewGroup) findViewById(R.id.picker_selection);
        vSelectionDone = (FrameLayout) findViewById(R.id.picker_selection_done);
        vSelectionDone.setOnClickListener(mOnSelectionDoneClickListener);

        Picasso.with(this)
                .load(ActivityUtils.getImageUrl(this))
                .transform(new CircleTransformation())
                .placeholder(R.drawable.ic_account_circle_no_padding_white_48dp)
                .into(vHeaderImage);

        mSearchFragment = new SearchFragment();
        mPlaylistsFragment = MusicPickerListFragment.create(ContentType.PLAYLISTS);
        mSongsFragment = MusicPickerListFragment.create(ContentType.SONGS);
        mAlbumsFragment = MusicPickerListFragment.create(ContentType.ALBUMS);

        // Selecting default item
        vNavigationView.setCheckedItem(R.id.music_picker_item_playlists);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.music_picker_content, mPlaylistsFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (vDrawer.isDrawerOpen(GravityCompat.START)) {
            vDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_music_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment;
        int id = item.getItemId();
        switch (id) {
            case R.id.music_picker_item_search:
                fragment = mSearchFragment;
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
        if (mSelectedTrackUris.isEmpty()) {
            vSelection.setVisibility(View.GONE);
        } else {
            vSelection.setVisibility(View.VISIBLE);
            vSelectionStatus.setText(mSelectedTrackUris.size() + " tracks selected");
        }
    }

    private View.OnClickListener mOnSelectionDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mSelectedTrackUris.isEmpty()) {
                setResult(RESULT_CANCELED);
            } else {
                Intent result = new Intent();
                ArrayList<String> trackUris = Lists.newArrayList(mSelectedTrackUris.keySet());
                result.putStringArrayListExtra(RESULT_TRACK_URIS, trackUris);
                setResult(RESULT_OK, result);
            }
            finish();
        }
    };
}
