package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.brufino.terpsychore.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (true) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, SessionActivity.class);
            intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, "12");
            intent.putExtra(SessionActivity.TRACK_ID_EXTRA_KEY, "spotify:track:3Gaj5GBeZ8aynvtPkxrr9A");
            intent.putExtra(SessionActivity.TRACK_NAME_EXTRA_KEY, "Paradise");
            intent.putExtra(SessionActivity.TRACK_ARTIST_EXTRA_KEY, "Tiësto");
            startActivity(intent);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }
}
