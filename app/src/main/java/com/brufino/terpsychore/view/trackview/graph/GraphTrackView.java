package com.brufino.terpsychore.view.trackview.graph;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.util.Pair;
import android.util.AttributeSet;
import android.view.View;
import com.brufino.terpsychore.view.trackview.TrackView;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class GraphTrackView extends View implements TrackView, TrackCurve.UpdateListener {

    private static final double FIXED_HEIGHT_OFFSET = 0.25;

    private List<Pair<TrackCurve, TrackCurve.Style>> mStaticTrackCurves = new ArrayList<>();
    private Path mCurveAreaPath = new Path();

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
        invalidate();
    }

    @Override
    public void onTrackCurveUpdated(TrackCurve trackCurve) {
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int height = canvas.getHeight();
        int width = canvas.getWidth();

        for (Pair<TrackCurve, TrackCurve.Style> pair : mStaticTrackCurves) {
            TrackCurve curve = pair.first;
            TrackCurve.Style style = pair.second;

            List<TrackCurve.Point> controlPoints = curve.getControlPoints();
            /* TODO: Do it online inside this for, performance here is critical */
            controlPoints = addInterpolatedPoint(controlPoints, curve.getCurrentPosition());
            for (int i = 0; i < controlPoints.size() - 1; i++) {
                TrackCurve.Point a = controlPoints.get(i);
                TrackCurve.Point b = controlPoints.get(i + 1);

                Paint strokePaint, pathPaint;
                if (curve.getCurrentPosition() <= a.x) {
                    strokePaint = style.getStrokePaint();
                    pathPaint = style.getPathPaint();
                } else {
                    strokePaint = style.getStrokeTopPaint();
                    pathPaint = style.getPathTopPaint();
                }

                // Minimun height
                double aY = FIXED_HEIGHT_OFFSET + a.y * (1 - FIXED_HEIGHT_OFFSET);
                double bY = FIXED_HEIGHT_OFFSET + b.y * (1 - FIXED_HEIGHT_OFFSET);

                // (1 - y) bc y is backwards!
                float leftX = (float) (a.x * width);
                float leftY = (float) ((1 - aY) * height);
                float rightX = (float) (b.x * width);
                float rightY = (float) ((1 - bY) * height);

                if (style.shouldDrawUnderCurve()) {
                    mCurveAreaPath.reset();
                    mCurveAreaPath.moveTo(leftX, leftY);
                    mCurveAreaPath.lineTo(rightX, rightY);
                    mCurveAreaPath.lineTo(rightX, height);
                    mCurveAreaPath.lineTo(leftX, height);
                    mCurveAreaPath.lineTo(leftX, leftY);
                    mCurveAreaPath.close();
                    canvas.drawPath(mCurveAreaPath, pathPaint);
                }
                canvas.drawLine(leftX, leftY, rightX, rightY, strokePaint);
            }
        }
    }

    private List<TrackCurve.Point> addInterpolatedPoint(List<TrackCurve.Point> points, double x) {
        ImmutableList.Builder<TrackCurve.Point> builder = new ImmutableList.Builder<>();
        for (int i = 0, n = points.size(); i < n - 1; i++) {
            TrackCurve.Point a = points.get(i);
            TrackCurve.Point b = points.get(i + 1);
            builder.add(a);
            if (a.x < x && x < b.x) {
                double y = a.y + (x - a.x) / (b.x - a.x) * (b.y - a.y);
                TrackCurve.Point p = new TrackCurve.Point(x, y);
                builder.add(p);
            }
            builder.add(b);
        }
        return builder.build();
    }
}
