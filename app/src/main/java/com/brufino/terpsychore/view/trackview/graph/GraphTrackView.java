package com.brufino.terpsychore.view.trackview.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.brufino.terpsychore.view.trackview.TrackView;

import java.util.ArrayList;
import java.util.List;

public class GraphTrackView extends View implements TrackView {

    private List<Pair<TrackCurve, TrackCurve.Style>> mStaticTrackCurves = new ArrayList<>();

    public GraphTrackView(Context context) {
        super(context);
        initializeView();
    }

    public GraphTrackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public GraphTrackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
    }

    private void initializeView() {

    }

    public void addTrackCurve(TrackCurve trackCurve, TrackCurve.Style trackCurveStyle) {
        mStaticTrackCurves.add(Pair.create(trackCurve, trackCurveStyle));
        trackCurveStyle.preCalculatePaints();
        invalidate();
    }

    public void addLiveTrackCurve(LiveTrackCurve liveTrackCurve, TrackCurve.Style trackCurveStyle) {
        /* TODO: Subscrive to callbacks and etc. */
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d("VFY", "onSizeChanged()");
        Log.d("VFY", "  w:" + oldw + " -> " + w);
        Log.d("VFY", "  h:" + oldh + " -> " + h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("VFY", "GraphTrackView.onDraw()");
        super.onDraw(canvas);

        int height = canvas.getHeight();
        int width = canvas.getWidth();

        for (Pair<TrackCurve, TrackCurve.Style> pair : mStaticTrackCurves) {
            TrackCurve curve = pair.first;
            TrackCurve.Style style = pair.second;

            Paint strokePaint = style.getStrokePaint();

            List<TrackCurve.Point> controlPoints = curve.getControlPoints();
            for (int i = 0; i < controlPoints.size() - 1; i++) {
                TrackCurve.Point a = controlPoints.get(i);
                TrackCurve.Point b = controlPoints.get(i + 1);
                // (1 - y) bc y is backwards!
                canvas.drawLine(
                        (float) (a.x * width),
                        (float) ((1 - a.y) * height),
                        (float) (b.x * width),
                        (float) ((1 - b.y) * height),
                        strokePaint);
            }
        }
    }
}
