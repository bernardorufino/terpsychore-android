package com.brufino.terpsychore.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.brufino.terpsychore.R;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST_CODE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (true) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST_CODE);
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
        switch (requestCode) {
            case LOGIN_REQUEST_CODE:
                Toast.makeText(MainActivity.this, "Logged in", Toast.LENGTH_LONG).show();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(MainActivity.this, SessionActivity.class);
                        intent.putExtra(SessionActivity.SESSION_ID_EXTRA_KEY, "12");
                        intent.putExtra(SessionActivity.TRACK_ID_EXTRA_KEY, "spotify:track:3Gaj5GBeZ8aynvtPkxrr9A");
                        intent.putExtra(SessionActivity.TRACK_NAME_EXTRA_KEY, "Paradise");
                        intent.putExtra(SessionActivity.TRACK_ARTIST_EXTRA_KEY, "Tiësto");
                        startActivity(intent);
                    }
                }, 1000);
        }
    }
}
