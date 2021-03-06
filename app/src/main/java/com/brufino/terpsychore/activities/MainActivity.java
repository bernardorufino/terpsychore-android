package com.brufino.terpsychore.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.fragments.main.MainFragment;
import com.brufino.terpsychore.fragments.main.MusicInboxFragment;
import com.brufino.terpsychore.fragments.main.SessionsListFragment;
import com.brufino.terpsychore.lib.CircleTransformation;
import com.brufino.terpsychore.util.ActivityUtils;
import com.squareup.picasso.Picasso;

import static com.google.common.base.Preconditions.*;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_FRAGMENT = "openFragment";

    private static final int LOGIN_REQUEST_CODE = 0;

    private FloatingActionButton vAddSessionButton;
    private ProgressBar vLoading;
    private Toolbar vToolbar;
    private DrawerLayout vDrawer;
    private NavigationView vNavigationView;
    private TextView vHeaderUserName;
    private ImageView vHeaderImage;
    private CoordinatorLayout vMainCoordinatorLayout;
    private FloatingActionButton vFab;

    private SessionsListFragment mSessionListFragment;
    private MusicInboxFragment mMusicInboxFragment;
    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        vDrawer = (DrawerLayout) findViewById(R.id.main_drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, vDrawer, vToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        vDrawer.addDrawerListener(toggle);
        toggle.syncState();
        vNavigationView = (NavigationView) findViewById(R.id.nav_view);
        vHeaderImage = (ImageView) vNavigationView.getHeaderView(0).findViewById(R.id.main_nav_header_image);
        vHeaderUserName = (TextView) vNavigationView.getHeaderView(0).findViewById(R.id.main_nav_header_user_name);
        vNavigationView.setNavigationItemSelectedListener(this);
        vMainCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.main_coordinator_layout);

        // Set default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences_general, false);

        mSessionListFragment = new SessionsListFragment();
        mMusicInboxFragment = new MusicInboxFragment();

        mUserId = ActivityUtils.getUserId(this);
        if (mUserId != null) {
            onCreateFinish();
        } else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
            // onCreateFinish() is called in onActivityResult()
        }
    }

    private void onCreateFinish() {
        checkState(mUserId != null, "mUserId shouldn't be null");

        Picasso.with(this)
                .load(ActivityUtils.getImageUrl(this))
                .transform(new CircleTransformation())
                .placeholder(R.drawable.ic_account_circle_no_padding_white_48dp)
                .into(vHeaderImage);
        vHeaderUserName.setText(ActivityUtils.getDisplayName(this));

        int menuResId = getIntent().getIntExtra(EXTRA_FRAGMENT, R.id.main_drawer_sessions);
        vNavigationView.setCheckedItem(menuResId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_content, checkedGetMainFragment(menuResId))
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case LOGIN_REQUEST_CODE:
                Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
                mUserId = ActivityUtils.getUserId(this);
                onCreateFinish();
        }
    }

    @Override
    public void onBackPressed() {
        if (vDrawer.isDrawerOpen(GravityCompat.START)) {
            vDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private MainFragment checkedGetMainFragment(int menuResId) {
        return checkNotNull(getMainFragment(menuResId), "Unknown fragment for menuResId");
    }

    private MainFragment getMainFragment(int menuResId) {
        switch (menuResId) {
            case R.id.main_drawer_sessions:
                return mSessionListFragment;
            case R.id.main_drawer_music_inbox:
                return mMusicInboxFragment;
            default:
                return null;
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        MainFragment fragment = getMainFragment(item.getItemId());

        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            switch (item.getItemId()) {
                case R.id.main_drawer_settings:
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    break;
                case R.id.main_drawer_logout:
                    new AlertDialog.Builder(this, R.style.AppTheme_AlertDialog)
                            .setMessage(R.string.logout_message)
                            .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    LoginActivity.logout(MainActivity.this);
                                    Log.d("VFY", "after logout: user id = " + ActivityUtils.getUserId(MainActivity.this));
                                    finish();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    break;
                default:
                    throw new AssertionError("Unknown menu id");
            }
        }

        vDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public ViewGroup getFabParent() {
        return vMainCoordinatorLayout;
    }

    public void setFab(FloatingActionButton fab) {
        if (vFab != null && vFab.getParent() == vMainCoordinatorLayout) {
            vMainCoordinatorLayout.removeView(vFab);
        }
        vFab = fab;
        if (vFab != null) {
            CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) vFab.getLayoutParams();
            layoutParams.setAnchorId(R.id.main_content);
            layoutParams.anchorGravity = Gravity.BOTTOM | Gravity.END;
            layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
            vMainCoordinatorLayout.addView(vFab, layoutParams);
        }
    }
}
