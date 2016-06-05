package com.brufino.terpsychore.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.brufino.terpsychore.R;
import com.brufino.terpsychore.activities.SessionActivity;
import com.brufino.terpsychore.view.trackview.graph.GraphTrackView;
import com.brufino.terpsychore.view.trackview.graph.TrackCurve;
import com.spotify.sdk.android.player.Player;

public class GraphTrackFragment extends Fragment implements SessionActivity.TrackUpdateListener {

    public static final String TRACK_CURVE_SAVED_STATE_KEY = "trackCurve";

    private GraphTrackView vGraphTrackView;
    private TrackCurve mTrackCurve;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_graph_track, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        vGraphTrackView = (GraphTrackView) getView().findViewById(R.id.graph_track_view);

        TrackCurve.Style trackCurveStyle = new TrackCurve.Style()
                .setFillColor(ContextCompat.getColor(getContext(), R.color.graphForeground))
                .setStroke(ContextCompat.getColor(getContext(), R.color.graphStroke), 6)
                .setFillColorTop(ContextCompat.getColor(getContext(), R.color.graphForegroundTop))
                .setStrokeTop(ContextCompat.getColor(getContext(), R.color.graphStrokeTop), 6);

        if (savedInstanceState != null) {
            mTrackCurve = savedInstanceState.getParcelable(TRACK_CURVE_SAVED_STATE_KEY);
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
        } else {
            mTrackCurve = TrackCurve.random();
            vGraphTrackView.addTrackCurve(mTrackCurve, trackCurveStyle);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(TRACK_CURVE_SAVED_STATE_KEY, mTrackCurve);
    }

    @Override
    public void onTrackUpdate(double currentPosition, int durationInMs, Player player) {
        mTrackCurve.seekPosition(currentPosition);
        vGraphTrackView.postInvalidate();
    }
}
