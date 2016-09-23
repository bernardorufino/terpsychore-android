package com.brufino.terpsychore.activities;

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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.musicpicker.MusicPickerListFragment;
import com.brufino.terpsychore.fragments.musicpicker.SearchFragment;

public class MusicPickerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;
    private Toolbar vToolbar;
    private TextView vHeaderUserName;
    private FrameLayout vMusicContent;
    private MusicPickerListFragment mMusicPickerListFragment;

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

        mMusicPickerListFragment = new MusicPickerListFragment();
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

        MusicPickerListFragment musicPicker;
        Fragment fragment;
        int id = item.getItemId();
        switch (id) {
            case R.id.music_picker_item_search:
                Toast.makeText(MusicPickerActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                fragment = new SearchFragment();
                break;
            case R.id.music_picker_item_playlists:
                mMusicPickerListFragment.setItemType(MusicPickerListFragment.ItemType.PLAYLIST);
                fragment = mMusicPickerListFragment;
                break;
            case R.id.music_picker_item_songs:
                mMusicPickerListFragment.setItemType(MusicPickerListFragment.ItemType.SONG);
                fragment = mMusicPickerListFragment;
                break;
            case R.id.music_picker_item_albums:
                mMusicPickerListFragment.setItemType(MusicPickerListFragment.ItemType.ALBUM);
                fragment = mMusicPickerListFragment;
                break;
            default:
                throw new AssertionError("Unknown id");
        }


         getSupportFragmentManager()
                 .beginTransaction()
                 .replace(R.id.music_picker_content, fragment)
                 .commit();

        vToolbar.setTitle(item.getTitle());

        vDrawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
