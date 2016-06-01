package com.brufino.terpsychore.view.trackview.graph;

import android.graphics.Paint;
import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class TrackCurve {

    /* TODO: Remove */
    public static TrackCurve sample() {
        TrackCurve trackCurve = new TrackCurve();
        for (int i = 0, n = 20; i <= n; i++) {
            double x = (double) i / n;
            double y = x * x;
            Point p = new Point(x, y);
            trackCurve.addControlPoint(p);
        }
        return trackCurve;
    }

    private List<Point> mControlPoints = new LinkedList<>();

    public List<Point> getControlPoints() {
        return ImmutableList.copyOf(mControlPoints);
    }

    public void addControlPoint(Point point) {
        validatePoint(point);
        mControlPoints.add(point);
    }

    private void validatePoint(Point point) {
        checkArgument(point.x >= 0, "Point's x coordinate must >= 0");
        checkArgument(point.x <= 1, "Point's x coordinate must <= 1");
        checkArgument(point.y >= 0, "Point's y coordinate must >= 0");
        checkArgument(point.y <= 1, "Point's y coordinate must <= 1");
    }

    public static class Point {
        public double x;
        public double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class Style {

        private Paint mStrokePaint;

        /* TODO: Remove! */
        public static Style sample() {
            return new Style();
        }

        public void preCalculatePaints() {
            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStrokePaint.setColor(0xFF003333);
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(8);
        }

        public Paint getStrokePaint() {
            return mStrokePaint;
        }
    }
}
