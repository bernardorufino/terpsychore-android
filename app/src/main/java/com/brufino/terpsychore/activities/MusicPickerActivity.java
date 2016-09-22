package com.brufino.terpsychore.activities;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
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
import com.brufino.terpsychore.fragments.musicpicker.*;
import com.brufino.terpsychore.view.trackview.MusicPickerList;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

public class MusicPickerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;
    private Toolbar vToolbar;
    private TextView vHeaderUserName;
    private FrameLayout vMusicContent;
    private MusicPickerList<JsonObject> vMusicList;

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
        //vMusicContent = (FrameLayout) findViewById(R.id.music_picker_content);
        //noinspection unchecked
        vMusicList = (MusicPickerList<JsonObject>) findViewById(R.id.music_picker_list);
        vMusicList.setTransform(mPlaylistsTransform);
    }

    private Function<JsonObject, MusicPickerList.Item> mPlaylistsTransform =
            new Function<JsonObject, MusicPickerList.Item>() {
        @Override
        public MusicPickerList.Item apply(JsonObject input) {
            return new MusicPickerList.Item("Discover Weekly", "by Spotify", null);
        }
    };

    private Function<JsonObject, MusicPickerList.Item> mSongsTransform =
            new Function<JsonObject, MusicPickerList.Item>() {
        @Override
        public MusicPickerList.Item apply(JsonObject input) {
            return new MusicPickerList.Item("Concrete Angel", "Gareth Emery", null);
        }
    };

    private Function<JsonObject, MusicPickerList.Item> mAlbumsTransform =
            new Function<JsonObject, MusicPickerList.Item>() {
        @Override
        public MusicPickerList.Item apply(JsonObject input) {
            return new MusicPickerList.Item("Embrace", "Armin van Buuren", null);
        }
    };

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

        MusicPickerFragment fragment;
        int id = item.getItemId();
        switch (id) {
            case R.id.music_picker_item_search:
                Toast.makeText(MusicPickerActivity.this, "TODO: Implement!", Toast.LENGTH_SHORT).show();
                break;
            case R.id.music_picker_item_playlists:
                vMusicList.setTransform(mPlaylistsTransform);
                break;
            case R.id.music_picker_item_songs:
                vMusicList.setTransform(mSongsTransform);
                break;
            case R.id.music_picker_item_albums:
                vMusicList.setTransform(mAlbumsTransform);
                break;
            default:
                throw new AssertionError("Unknown id");
        }

        vMusicList.setList(ImmutableList.<JsonObject>builder()
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .add(new JsonObject())
                .build());

        // getSupportFragmentManager()
        //         .beginTransaction()
        //         .replace(R.id.music_picker_content, fragment)
        //         .commit();

        vToolbar.setTitle(item.getTitle());

        vDrawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
