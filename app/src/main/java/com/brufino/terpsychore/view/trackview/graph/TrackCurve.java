package com.brufino.terpsychore.view.trackview.graph;

import android.graphics.Paint;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class TrackCurve {

    /* TODO: Remove */
    public static TrackCurve sample(Function<Double, Double> f) {
        TrackCurve trackCurve = new TrackCurve();
        for (int i = 0, n = 5; i <= n; i++) {
            double x = (double) i / n;
            double y = f.apply(x);
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

        public static Style create(int strokeColor, float strokeWidth, int fillColor) {
            Style style = new Style();
            style.setStroke(strokeColor, strokeWidth);
            style.setFillColor(fillColor);
            return style;
        }

        private Paint mStrokePaint;
        private Paint mPathPaint;

        public Style() {
            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(8);
            mPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        public void setStroke(int color, float width) {
            mStrokePaint.setColor(color);
            mStrokePaint.setStrokeWidth(width);
        }

        public void setFillColor(int color) {
            mPathPaint.setColor(color);
        }

        /* package private */ Paint getStrokePaint() {
            return mStrokePaint;
        }

        /* package private */ Paint getPathPaint() {
            return mPathPaint;
        }

        /* package private */ boolean shouldDrawUnderCurve() {
            return mPathPaint.getAlpha() > 0;
        }
    }
}
