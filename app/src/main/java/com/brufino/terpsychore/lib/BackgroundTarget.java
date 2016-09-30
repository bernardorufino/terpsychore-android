package com.brufino.terpsychore.lib;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Use for setting the background image of views. Note that this will set/overwrite the TAG of the view provided.
 */
public class BackgroundTarget implements Target {

    public static BackgroundTarget of(View view) {
        return new BackgroundTarget(view);
    }

    private View mTarget;

    private BackgroundTarget(View target) {
        mTarget = target;
        mTarget.setTag(this);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        BitmapDrawable bitmapDrawable = new BitmapDrawable(mTarget.getResources(), bitmap);
        mTarget.setBackground(bitmapDrawable);
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        if (errorDrawable != null) {
            mTarget.setBackground(errorDrawable);
        }
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
        if (placeHolderDrawable != null) {
            mTarget.setBackground(placeHolderDrawable);
        }
    }
}
