package com.brufino.terpsychore.fragments;

import android.content.res.ColorStateList;
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
import com.brufino.terpsychore.activities.PlayerManager;
import com.brufino.terpsychore.network.ApiUtils;
import com.brufino.terpsychore.util.ActivityUtils;
import com.brufino.terpsychore.util.ViewUtils;
import com.brufino.terpsychore.view.trackview.TrackProgressBar;
import com.brufino.terpsychore.view.trackview.graph.GraphTrackView;
import com.brufino.terpsychore.view.trackview.graph.TrackCurve;
import com.google.gson.JsonObject;
import com.spotify.sdk.android.player.Player;

public class TrackPlaybackFragment extends Fragment implements PlayerManager.TrackUpdateListener {

    private static final String SAVED_STATE_KEY_TRACK_CURVE = "trackCurve";
    private static final String SAVED_STATE_KEY_CURRENT_POSITION = "currentTrackTimeText";
    private static final String SAVED_STATE_KEY_DURATION = "totalTrackTimeText";
    private static final int REPLAY_REWIND_TIME_IN_MS = 10_000;

    private GraphTrackView vGraphTrackView;
    private TrackProgressBar vTrackProgressBar;
    private TextView vDisplayCurrentTrackTime;
    private TextView vDisplayTotalTrackTime;
    private ImageButton vReplayButton;
    private ImageButton vPlayButton;
    private ImageButton vNextButton;
    private TextView vTrackTitleName;
    private TextView vTrackTitleArtist;
    private TextView vNextTrackName;
    private TextView vNextTrackArtist;
    private ViewGroup vQueueView;

    private PlayerManager mPlayerManager;
    private TrackCurve mTrackCurve;
    private boolean mPlaying = false;
    private int mCurrentPosition;
    private int mDuration;
    private JsonObject mSession;
    private boolean mCanPlay;
    private boolean mCanReplay;
    private boolean mCanNext;
    private ColorStateList mActivatedColor;
    private ColorStateList mDeactivatedColor;
    private QueueManager mQueueManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_track_playback, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d("VFY", "TrackPlaybackFragment.onActivityCreated()");
        super.onActivityCreated(savedInstanceState);

        vTrackTitleName = (TextView) getView().findViewById(R.id.track_title_name);
        vTrackTitleArtist = (TextView) getView().findViewById(R.id.track_title_artist);
        vQueueView = (ViewGroup) getView().findViewById(R.id.playback_queue_next);
        vQueueView.setOnClickListener(mOnQueueViewClickListener);
        vNextTrackName = (TextView) getView().findViewById(R.id.playback_control_next_track_name);
        vNextTrackArtist = (TextView) getView().findViewById(R.id.playback_control_next_track_artist);
        vGraphTrackView = (GraphTrackView) getView().findViewById(R.id.graph_track_view);
        vTrackProgressBar = (TrackProgressBar) getView().findViewById(R.id.track_progress_bar);
        vDisplayCurrentTrackTime = (TextView) getView().findViewById(R.id.display_current_track_time);
        vDisplayCurrentTrackTime.setText(ViewUtils.formatTrackTime(0));
        vDisplayTotalTrackTime = (TextView) getView().findViewById(R.id.display_total_track_time);
        vDisplayTotalTrackTime.setText(ViewUtils.formatTrackTime(0));
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
        // onTrackUpdate(false, mCurrentPosition, mDuration, null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_STATE_KEY_TRACK_CURVE, mTrackCurve);
        outState.putInt(SAVED_STATE_KEY_CURRENT_POSITION, mCurrentPosition);
        outState.putInt(SAVED_STATE_KEY_DURATION, mDuration);
    }

    public void setPlayerManager(PlayerManager playerManager) {
        mPlayerManager = playerManager;
    }

    public void setQueueManager(QueueManager queueManager) {
        mQueueManager = queueManager;
    }

    private View.OnClickListener mOnQueueViewClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mQueueManager.onOpenQueue(v);
        }
    };

    public void bind(JsonObject session) {
        mSession = session;
        JsonObject queue = session.get("queue").getAsJsonObject();

        JsonObject currentTrack = ApiUtils.getCurrentTrack(queue);
        if (currentTrack != null) {
            String trackName = currentTrack.get("name").getAsString();
            String trackArtist = currentTrack.get("artist").getAsString();
            vTrackTitleName.setText(trackName);
            vTrackTitleArtist.setText(trackArtist);
        }

        JsonObject nextTrack = ApiUtils.getNextTrack(queue);
        vNextTrackName.setVisibility(View.GONE);
        vNextTrackArtist.setVisibility(View.GONE);
        if (nextTrack != null) {
            vNextTrackName.setVisibility(View.VISIBLE);
            vNextTrackArtist.setVisibility(View.VISIBLE);
            String nextTrackName = nextTrack.get("name").getAsString();
            String nextTrackArtist = nextTrack.get("artist").getAsString();
            vNextTrackName.setText(nextTrackName);
            vNextTrackArtist.setText(nextTrackArtist);
        }

        // see onTrackUpdate()
        mCanPlay = false;
        mCanReplay = false;
        mCanNext = (nextTrack != null);
        mActivatedColor = ActivityUtils.getColorList(getContext(), R.color.playbackControlActivated);
        mDeactivatedColor = ActivityUtils.getColorList(getContext(), R.color.playbackControlDeactivated);
        updatePlaybackControlStates();
    }

    private void updatePlaybackControlStates() {
        vPlayButton.setImageTintList((mCanPlay) ? mActivatedColor : mDeactivatedColor);
        vReplayButton.setImageTintList((mCanReplay) ? mActivatedColor : mDeactivatedColor);
        vNextButton.setImageTintList((mCanNext) ? mActivatedColor : mDeactivatedColor);
    }

    @Override
    public void onTrackUpdate(
            boolean playing,
            int currentPositionInMs,
            int durationInMs,
            PlayerManager playerManager,
            @Nullable Player _) {

        mCurrentPosition = currentPositionInMs;
        mDuration = durationInMs;
        double position = (durationInMs > 0) ? Math.min(1, (double) currentPositionInMs / durationInMs) : 0;

        mCanPlay = playerManager.canControl();
        mCanReplay = playerManager.canControl();
        updatePlaybackControlStates();

        if (mPlaying != playing) {
            vPlayButton.setImageResource((playing) ? R.drawable.ic_pause_white_36dp : R.drawable.ic_play_arrow_white_36dp);
            mPlaying = playing;
        }

        mTrackCurve.seekPosition(position);
        vGraphTrackView.postInvalidate();

        vDisplayCurrentTrackTime.setText(ViewUtils.formatTrackTime(currentPositionInMs));
        vDisplayTotalTrackTime.setText(ViewUtils.formatTrackTime(durationInMs));
        vTrackProgressBar.setProgress(position);
    }

    private View.OnClickListener mOnReplayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mCanReplay) {
                return;
            }
            mPlayerManager.replay(REPLAY_REWIND_TIME_IN_MS);
        }
    };

    private View.OnClickListener mOnPlayButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mCanPlay) {
                return;
            }
            mPlayerManager.togglePlay();
        }
    };

    private View.OnClickListener mOnNextButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mCanNext) {
                return;
            }
            Toast.makeText(getContext(), "TODO: Implement!", Toast.LENGTH_SHORT).show();
        }
    };

    public static interface QueueManager {
        public void onOpenQueue(View viewHint);
    }
}
