package com.brufino.terpsychore.view.trackview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.brufino.terpsychore.R;

import static com.google.common.base.Preconditions.*;

/* TODO: Extract bar bg colors into view attributes */
public class TrackProgressBar extends RelativeLayout {

    private View mBarFg;
    private View mBarBg;

    public TrackProgressBar(Context context) {
        super(context);
        initializeView();
    }

    public TrackProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public TrackProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
    }

    private void initializeView() {
        LayoutInflater.from(getContext()).inflate(R.layout.track_progress_bar, this);
        mBarFg = findViewById(R.id.track_progress_bar_fg);
        mBarBg = findViewById(R.id.track_progress_bar_bg);
    }

    public void setProgress(double progress) {
        checkArgument(0 <= progress && progress <= 1, "progress must be btw 0 and 1");

        ViewGroup.LayoutParams params = mBarFg.getLayoutParams();
        params.width = (int) (progress * mBarBg.getWidth());
        mBarFg.requestLayout();
    }
}
