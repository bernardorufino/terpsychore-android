package com.brufino.terpsychore.view.trackview.graph;

import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

public class TrackCurve implements Parcelable {

    public static final Creator<TrackCurve> CREATOR = new Creator<TrackCurve>() {
        @Override
        public TrackCurve createFromParcel(Parcel in) {
            return new TrackCurve(in);
        }

        @Override
        public TrackCurve[] newArray(int size) {
            return new TrackCurve[size];
        }
    };

    private static final Random RANDOM = new Random(System.currentTimeMillis());

    /* TODO: Remove */
    public static TrackCurve sample(Function<Double, Double> f) {
        TrackCurve trackCurve = new TrackCurve();
        for (int i = 0, n = 100; i <= n; i++) {
            double x = (double) i / n;
            double y = f.apply(x);
            Point p = new Point(x, y);
            trackCurve.addControlPoint(p);
        }
        return trackCurve;
    }

    public static TrackCurve random() {
        return sample(new Function<Double, Double>() {
            private double y = 0.25;
            @Override
            public Double apply(Double x) {
                double dy;
                if (x < 0.3 || x > 0.6) {
                    dy = RANDOM.nextGaussian() * 0.01;
                } else if (x < 0.45) {
                    dy = 0.05 + RANDOM.nextGaussian() * 0.1;
                } else {
                    dy = -0.05 + RANDOM.nextGaussian() * 0.1;
                }
                y = Math.min(1, Math.max(0, y + dy));
                return y;
            }
        });
    }

    private List<UpdateListener> mUpdateListeners = new ArrayList<>();
    private List<Point> mControlPoints = new LinkedList<>();

    public TrackCurve() {
    }

    protected TrackCurve(Parcel in) {
        mControlPoints = in.createTypedArrayList(Point.CREATOR);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mControlPoints);
    }

    public List<Point> getControlPoints() {
        return ImmutableList.copyOf(mControlPoints);
    }

    public void addControlPoint(Point point) {
        validatePoint(point);
        mControlPoints.add(point);
        for (UpdateListener updateListener : mUpdateListeners) {
            updateListener.onTrackCurveUpdated(this);
        }
    }

    public void addUpdateListener(UpdateListener updateListener) {
        mUpdateListeners.add(updateListener);
    }

    private void validatePoint(Point point) {
        checkArgument(point.x >= 0, "Point's x coordinate must >= 0");
        checkArgument(point.x <= 1, "Point's x coordinate must <= 1");
        checkArgument(point.y >= 0, "Point's y coordinate must >= 0");
        checkArgument(point.y <= 1, "Point's y coordinate must <= 1");
    }

    public static interface UpdateListener {
        public void onTrackCurveUpdated(TrackCurve trackCurve);
    }

    public static class Point implements Parcelable {

        public static final Creator<Point> CREATOR = new Creator<Point>() {
            @Override
            public Point createFromParcel(Parcel in) {
                return new Point(in);
            }

            @Override
            public Point[] newArray(int size) {
                return new Point[size];
            }
        };

        public double x;
        public double y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        protected Point(Parcel in) {
            x = in.readDouble();
            y = in.readDouble();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeDouble(x);
            dest.writeDouble(y);
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
