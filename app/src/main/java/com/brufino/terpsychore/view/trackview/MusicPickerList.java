package com.brufino.terpsychore.view.trackview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import com.brufino.terpsychore.R;

public class MusicPickerList extends RelativeLayout {

    public MusicPickerList(Context context) {
        super(context);
        initializeView();
    }

    public MusicPickerList(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeView();
    }

    public MusicPickerList(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeView();
    }

    private void initializeView() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_music_picker_list, this);

    }
}
