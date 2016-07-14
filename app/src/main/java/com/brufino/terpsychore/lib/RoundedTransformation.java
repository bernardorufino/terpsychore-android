package com.brufino.terpsychore.lib;

import android.graphics.*;
import com.squareup.picasso.Transformation;

/**
 * From https://gist.github.com/aprock/6213395
 */
public class RoundedTransformation implements Transformation {

    private final int mRadius;
    private final int mMargin;  // In dp

    public RoundedTransformation(final int radius, final int margin) {
        mRadius = radius;
        mMargin = margin;
    }

    @Override
    public Bitmap transform(final Bitmap source) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        canvas.drawRoundRect(new RectF(mMargin, mMargin, source.getWidth() - mMargin, source.getHeight() - mMargin), mRadius, mRadius, paint);

        if (source != output) {
            source.recycle();
        }

        return output;
    }

    @Override
    public String key() {
        return "rounded(radius=" + mRadius + ", margin=" + mMargin + ")";
    }
}
