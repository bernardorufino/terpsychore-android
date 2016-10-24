package com.brufino.terpsychore.util;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.MetricAffectingSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.TextView;
import com.brufino.terpsychore.lib.EmojisIndex;
import com.brufino.terpsychore.lib.ExternalTypefaceSpan;
import com.google.common.collect.ImmutableList;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class FontUtils {

    public static final Font DEFAULT_EMOJI_FONT = Font.TWITTER;

    public static void applyFontToEmojis(Context context, Spannable spannable, Font font) {
        for (ExternalTypefaceSpan span : spannable.getSpans(0, spannable.length(), ExternalTypefaceSpan.class)) {
            spannable.removeSpan(span);
        }
        if (font == null) {
            return;
        }
        for (int i = 0; i < spannable.length(); ) {
            int codePoint = Character.codePointAt(spannable, i);
            int di = Character.charCount(codePoint);
            if (EmojisIndex.isEmoji(codePoint)) {
                ExternalTypefaceSpan span = font.getExternalTypefaceSpan(context);
                spannable.setSpan(span, i, i + di, 0);
            }
            i += di;
        }
    }

    public static Spannable getTextWithFontForEmojis(Context context, CharSequence text, Font font) {
        Spannable spannable = new SpannableStringBuilder(text);
        applyFontToEmojis(context, spannable, font);
        return spannable;
    }

    public static Spannable getTextWithDefaultFontForEmojis(Context context, CharSequence text) {
        return getTextWithFontForEmojis(context, text, DEFAULT_EMOJI_FONT);
    }

    public static void setTextWithEmojis(TextView textView, CharSequence text) {
        CharSequence textWithEmojis = getTextWithFontForEmojis(textView.getContext(), text, DEFAULT_EMOJI_FONT);
        textView.setText(textWithEmojis, TextView.BufferType.SPANNABLE);
    }

    private static void spanDump(CharSequence text, Spannable spannable) {
        Log.v("VFY", "/==== SPAN DUMP ====\\");
        for (Class<? extends MetricAffectingSpan> spanType : ImmutableList.of(CalligraphyTypefaceSpan.class, RelativeSizeSpan.class)) {
            for (MetricAffectingSpan span : spannable.getSpans(0, text.length(), spanType)) {
                int start = spannable.getSpanStart(span);
                int end = spannable.getSpanEnd(span);
                String view = "<" + text.subSequence(0, start) + "[" + text.subSequence(start, end) + "]" + text.subSequence(end, text.length()) + ">";
                Log.v("VFY", span.getClass().getSimpleName() + ": " + view + " (" + start + " -> " + end + ")");
            }
        }
        Log.v("VFY", "\\==== SPAN DUMP ====/");
    }

    public static enum Font {
        //ANDROID_N("fonts/androidn.ttf", 1.0f, 0.0f),
        //EMOJIONE("fonts/emojione.ttf", 1.0f, 0.0f),
        //IOS("fonts/ios4.ttf", 1.1f, -0.08f),
        TWITTER("fonts/twitter2.ttf", 1.1f, -0.1f);


        public String filename;
        public float relativeSize;
        public float baselineShiftProportion;
        private Typeface mTypeface = null;

        Font(String filename, float relativeSize, float baselineShiftProportion) {
            this.filename = filename;
            this.relativeSize = relativeSize;
            this.baselineShiftProportion = baselineShiftProportion;
        }

        public ExternalTypefaceSpan getExternalTypefaceSpan(Context context) {
            if (mTypeface == null) {
                mTypeface = TypefaceUtils.load(context.getAssets(), filename);
            }
            return new ExternalTypefaceSpan(mTypeface, relativeSize, baselineShiftProportion);
        }
    }
}
