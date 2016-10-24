package com.brufino.terpsychore.lib;

import android.content.Context;
import android.text.Editable;
import com.brufino.terpsychore.util.FontUtils;

public class EmojiFontTextWatcher extends SimpleTextWatcher {

    private Context mContext;
    private boolean mCanChangeInput = true;

    public EmojiFontTextWatcher(Context context) {
        mContext = context;
    }

    public EmojiFontTextWatcher() {
    }

    protected Context getContext() {
        return mContext;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (mCanChangeInput) {
            mCanChangeInput = false;
            FontUtils.applyFontToEmojis(getContext(), s, FontUtils.DEFAULT_EMOJI_FONT);
            mCanChangeInput = true;
        }
    }
}
