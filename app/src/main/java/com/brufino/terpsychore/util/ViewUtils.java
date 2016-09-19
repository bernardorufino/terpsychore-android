package com.brufino.terpsychore.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.*;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ViewUtils {

    public static String formatTrackTime(int timeInMs) {
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

    public static Rect getRelativeGlobalVisibleRect(View subject, View reference) {
        Rect referenceRect = new Rect();
        reference.getGlobalVisibleRect(referenceRect);
        Rect subjectRect = new Rect();
        subject.getGlobalVisibleRect(subjectRect);
        subjectRect.offset(-referenceRect.left, -referenceRect.top);
        return subjectRect;
    }

    public static boolean isStrictAncestor(View parent, View child) {
        for (ViewParent p = child.getParent(); p != null; p = p.getParent()) {
            if (p == parent) {
                return true;
            }
        }
        return false;
    }

    /**
     * Will dispose views given in {@param views} with their centers around a circle centered in {@param centerX},
     * {@param centerY} with radius {@params radius}. They will start at {@param arcStart} and end at {@param arcEnd}
     * according to the trigonometric circle (e.g. 0 radians = 3 hours on the clock, pi radians = 9 hours).
     * The width / height of each view is retrieved from its LayoutParams, thus the LayoutParams must explicitly
     * specify the size of the view, instead of using wrap_content / match_parent.
     *
     * @param layout The RelativeLayout in which the views will be added and positioned.
     * @param centerX The x coordinate of the center of the circle (in pixels).
     * @param centerY The y coordinate of the center of the circle (in pixels).
     * @param radius The circle's radius (in pixels).
     * @param arcStart The start point of the arcle (in radians).
     * @param arcEnd The end point of the arcle (in radians).
     * @param views The list of views to be added.
     */
    public static void disposeViewsInArc(
            RelativeLayout layout,
            int centerX, int centerY, int radius,
            double arcStart, double arcEnd,
            List<View> views) {
        int n = views.size();
        double arcInterval = (arcEnd - arcStart) / (n - 1);

        for (int i = 0; i < n; i++) {
            View view = views.get(i);
            // Tried manually measuring but didn't work because each parent (in this case RelativeLayout) has its own
            // method of calling measure() on the child with the correct parameters. So I decided not to try to simulate
            // RelativeLayout logic (see source code for more details).
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
            int h = params.height;
            int w = params.width;

            double arc = arcStart + i * arcInterval;
            double cX = centerX + radius * Math.cos(arc);
            double cY = centerY - radius * Math.sin(arc);
            int left = (int) (cX - w / 2.0 + 0.5);
            int top = (int) (cY - h / 2.0 + 0.5);

            params.setMargins(left, top, 0, 0);
            layout.addView(view, params);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> List<T> inflate(Context context, ViewGroup root, boolean attach, int... resIds) {
        LayoutInflater inflater = LayoutInflater.from(context);
        List<T> views = new ArrayList<>(resIds.length);
        for (int resId : resIds) {
            T view = (T) inflater.inflate(resId, root, attach);
            views.add(view);
        }
        return views;
    }

    // http://stackoverflow.com/questions/7733813/how-can-you-tell-when-a-layout-has-been-drawn
    public static void addOnFirstGlobalLayoutListener(
            final View view,
            final ViewTreeObserver.OnGlobalLayoutListener listener) {
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                listener.onGlobalLayout();
            }
        });
    }

    // Shorter and convert to int
    public static int dpToPx(Resources resources, float dp) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()) + 0.5);
    }

    // Prevents instantiation
    private ViewUtils() {
        throw new AssertionError("Cannot instantiate object from " + this.getClass());
    }
}
