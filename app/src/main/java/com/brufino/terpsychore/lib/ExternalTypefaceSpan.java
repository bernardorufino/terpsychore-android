package com.brufino.terpsychore.lib;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

import static com.google.common.base.Preconditions.*;

public class ExternalTypefaceSpan extends MetricAffectingSpan {

    private Typeface mTypeface;
    private float mProportion = 1f;
    private float mBaselineShiftProportion = 0f;

    public ExternalTypefaceSpan(Typeface typeface) {
        checkArgument(typeface != null, "Typeface is null");
        mTypeface = typeface;
    }

    public ExternalTypefaceSpan(Typeface typeface, float proportion, float baselineShiftProportion) {
        checkArgument(typeface != null, "Typeface is null");

        mTypeface = typeface;
        mProportion = proportion;
        mBaselineShiftProportion = baselineShiftProportion;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        apply(textPaint);
    }

    @Override
    public void updateMeasureState(TextPaint textPaint) {
        apply(textPaint);
    }

    private void apply(TextPaint textPaint) {
        Typeface oldTypeface = textPaint.getTypeface();
        int oldStyle = oldTypeface != null ? oldTypeface.getStyle() : 0;
        int fakeStyle = oldStyle & ~mTypeface.getStyle();

        if ((fakeStyle & Typeface.BOLD) != 0) {
            textPaint.setFakeBoldText(true);
        }

        if ((fakeStyle & Typeface.ITALIC) != 0) {
            textPaint.setTextSkewX(-0.25f);
        }

        textPaint.setTypeface(mTypeface);
        textPaint.setAntiAlias(true);
        textPaint.setSubpixelText(true);
        textPaint.setTextSize(textPaint.getTextSize() * mProportion);
        textPaint.baselineShift += (int) (mBaselineShiftProportion * textPaint.ascent());
    }
}
