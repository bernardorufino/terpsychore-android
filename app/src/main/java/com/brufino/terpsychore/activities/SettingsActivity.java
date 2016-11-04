package com.brufino.terpsychore.activities;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.lib.EmoticonsHitCounter;

/**
 * Remember that this activity can also be accessed from the LoginActivity, so test for the presence of user id where
 * need instead of assuming it always exist.
 */
public class SettingsActivity extends AppCompatActivity {

    private Toolbar vToolbar;
    private View vResetEmoticonHistoryButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        vToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(vToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        vResetEmoticonHistoryButton = findViewById(R.id.settings_reset_emoticon_history);
        vResetEmoticonHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this, R.style.AppTheme_AlertDialog)
                        .setMessage("Reset emoticon history?")
                        .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EmoticonsHitCounter.load(SettingsActivity.this)
                                        .reset()
                                        .saveInBackground();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        getFragmentManager()
                .beginTransaction()
                .add(R.id.settings_general_container, new GeneralSettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // TODO: If previous activity does not belong to our app, go to MainActivity
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class GeneralSettingsFragment extends PreferenceFragmentImpl {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_general);
            findPreference(getString(R.string.preference_backend_address))
                    .setOnPreferenceChangeListener(mOnBackendAddressChangeListener);
        }

        private Preference.OnPreferenceChangeListener mOnBackendAddressChangeListener =
                new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Toast.makeText(getActivity(), "Remember to logout, kill the app and then reopen", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
    }

    public static class PreferenceFragmentImpl extends PreferenceFragment {

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            ListView listView = (ListView) getView().findViewById(android.R.id.list);
            if (listView != null) {
                listView.setPadding(0, 0, 0, 0);
                listView.setDividerHeight(0);
            }
        }

        private void initializePreferences(Preference preference) {
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
                    initializePreferences(preferenceGroup.getPreference(i));
                }
            } else {
                updatePreference(preference);
            }
        }

        private void updatePreference(Preference preference) {
            String stringValue = preference.getSharedPreferences().getAll().get(preference.getKey()).toString();
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                preference.setSummary(listPreference.getEntry());
            } else if (preference instanceof EditTextPreference) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                preference.setSummary(editTextPreference.getText());
            } else if (preference instanceof CheckBoxPreference ||
                    preference instanceof SwitchPreference) {
                /* No-op */
            } else {
                preference.setSummary(stringValue);
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            initializePreferences(getPreferenceScreen());
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(mOnPreferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(mOnPreferenceChangeListener);
        }

        private SharedPreferences.OnSharedPreferenceChangeListener mOnPreferenceChangeListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updatePreference(findPreference(key));
            }
        };
    }
}
