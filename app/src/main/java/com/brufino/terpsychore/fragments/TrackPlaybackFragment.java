package com.brufino.terpsychore.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.SessionActivity;
import com.brufino.terpsychore.view.trackview.TrackProgressBar;
import com.brufino.terpsychore.view.trackview.graph.GraphTrackView;
import com.brufino.terpsychore.view.trackview.graph.TrackCurve;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.PlayerStateCallback;

import java.util.Locale;

public class TrackPlaybackFragment extends Fragment implements SessionActivity.TrackUpdateListener {

    private static final String SAVED_STATE_KEY_TRACK_CURVE = "trackCurve";
    private static final String SAVED_STATE_KEY_CURRENT_POSITION = "currentTrackTimeText";
    private static final String SAVED_STATE_KEY_DURATION = "totalTrackTimeText";
    private static final int REPLAY_TIME_WINDOW_IN_MS = 10_000;

    private GraphTrackView vGraphTrackView;
    private TrackProgressBar vTrackProgressBar;
    private TextView vDisplayCurrentTrackTime;
    private TextView vDisplayTotalTrackTime;
    private ImageButton vReplayButton;
    private ImageButton vPlayButton;
    private ImageButton vNextButton;

    private Player mPlayer;
    private TrackCurve mTrackCurve;
    private boolean mPlaying = false;
    private int mCurrentPosition;
    private int mDuration;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_playback, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        vGraphTrackView = (GraphTrackView) getView().findViewById(R.id.graph_track_view);
        vTrackProgressBar = (TrackProgressBar) getView().findViewById(R.id.track_progress_bar);
        vDisplayCurrentTrackTime = (TextView) getView().findViewById(R.id.display_current_track_time);
        vDisplayCurrentTrackTime.setText(formatTrackTime(0));
        vDisplayTotalTrackTime = (TextView) getView().findViewById(R.id.display_total_track_time);
        vDisplayTotalTrackTime.setText(formatTrackTime(0));
        vReplayButton = (ImageButton) getView().findViewById(R.id.playback_control_replay);
        vReplayButton.setOnClickListener(mOnReplayButtonClickListener);
        vPlayButton = (ImageButton) getView().findViewById(R.id.playback_control_play);
        vPlayButton.setOnClickListener(mOnPlayButtonClickListener);
        vNextButton = (ImageButton) getView().findViewById(R.id.playback_control_next);
        vNextButton.setOnClickListener(mOnNextButtonClickListener);

        TrackCurve.Style trackCurveStyle = new TrackCurve.Style()
                .setFillColor(ContextCompat.getColor(getContext(), R.color.graphForeground))
                .setStroke(ContextCompat.getColor(getContext(), R.color.graphStroke), 8)
                .setFillColorTop(ContextCompat.getColor(getContext(), R.color.graphForegroundTop))
                .setStrokeTop(ContextCompat.getColor(getContext(), R.color.graphStrokeTop), 8);

        if (savedInstanceState != null) {
            mTrackCurve = savedInstanceState.getParcelable(SAVED_STATE_KEY_TRACK_CURVE);
            mCurrentPosition = savedInstanceState.getInt(SAVED_STATE_KEY_CURRENT_POSITION);
            mDuration = savedInstanceState.getInt(SAVED_STATE_KEY_DURATION);
        } else {
            mTrackCurve = TrackCurve.random();
            mCurrentPosition = 0;
            mDuration = 0;
        }
        vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
        /* TODO: Study why progress bar is resetting immediately after rotations */
        onTrackUpdate(false, mCurrentPosition, mDuration, null);
    }

    public void setPlayer(Player player) {
        mPlayer = player;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_STATE_KEY_TRACK_CURVE, mTrackCurve);
        outState.putInt(SAVED_STATE_KEY_CURRENT_POSITION, mCurrentPosition);
        outState.putInt(SAVED_STATE_KEY_DURATION, mDuration);
    }

    @Override
    public void onTrackUpdate(boolean playing, int currentPositionInMs, int durationInMs, @Nullable Player _) {
        mCurrentPosition = currentPositionInMs;
        mDuration = durationInMs;
        double position = (durationInMs > 0) ? (double) currentPositionInMs / durationInMs : 0;

        if (mPlaying && !playing) {
            vPlayButton.setImageResource(R.drawable.ic_play_arrow_white_36dp);
        } else if (!mPlaying && playing) {
            vPlayButton.setImageResource(R.drawable.ic_pause_white_36dp);
        }
        mPlaying = playing;

        mTrackCurve.seekPosition(position);
        vGraphTrackView.postInvalidate();

        vDisplayCurrentTrackTime.setText(formatTrackTime(currentPositionInMs));
        vDisplayTotalTrackTime.setText(formatTrackTime(durationInMs));
        Log.d("VFY", "setting progress to " + position);
        vTrackProgressBar.setProgress(position);
    }

    /* TODO: Refactor this mess of listeners attached to playback controls registering other listeners. Unify? */
    /* TODO: Check if mPlayer is available */

    private View.OnClickListener mOnReplayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlayer.getPlayerState(mOnReplayButtonClickRetrievePlayerState);
        }
    };

    private PlayerStateCallback mOnReplayButtonClickRetrievePlayerState = new PlayerStateCallback() {
        @Override
        public void onPlayerState(PlayerState playerState) {
            int newPosition = Math.max(0, playerState.positionInMs - REPLAY_TIME_WINDOW_IN_MS);
            mPlayer.seekToPosition(newPosition);
        }
    };

    private View.OnClickListener mOnPlayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mPlayer.getPlayerState(mOnPlayButtonClickRetrievePlayerState);
        }
    };

    private PlayerStateCallback mOnPlayButtonClickRetrievePlayerState = new PlayerStateCallback() {
        @Override
        public void onPlayerState(PlayerState playerState) {
            // Button image is controlled in TrackUpdater.onPlayerState()
            if (playerState.playing) {
                mPlayer.pause();
            } else {
                mPlayer.resume();
            }
        }
    };

    private View.OnClickListener mOnNextButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getContext(), "TODO: Implement!", Toast.LENGTH_SHORT).show();
        }
    };

    private static String formatTrackTime(int timeInMs) {
        int secs = (int) (timeInMs / 1000.0 + 0.5);
        int mins = secs / 60;
        secs = secs % 60;
        int hours = mins / 60;
        mins = mins % 60;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, mins, secs);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
        }
    }
}
