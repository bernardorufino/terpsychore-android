package com.brufino.terpsychore.lib;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.brufino.terpsychore.util.CoreUtils;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.util.List;

public class EmoticonsHitCounter extends LimitedWeightedQueueHitCounter<String> {

    public static final List<String> INITIAL_EMOTICONS = ImmutableList.of(
            String.valueOf(Character.toChars(0x1F621)),
            String.valueOf(Character.toChars(0x1F630)),
            String.valueOf(Character.toChars(0x1F62E)),
            String.valueOf(Character.toChars(0x2764)));

    public static final int QUEUE_LENGTH = 10;

    public static final Function<Integer, Double> INDEX_TO_WEIGHT_FUNCTION = new Function<Integer, Double>() {
        @Override
        public Double apply(Integer i) {
            return Math.pow(QUEUE_LENGTH - i, 2);
        }
    };

    private static EmoticonsHitCounter sInstance = null;
    private static final JsonParser JSON_PARSER = new JsonParser();

    /* Only run from the UI thread! */
    public static EmoticonsHitCounter load(Context context) {
        if (sInstance == null) {
            String serializedString = context.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                    .getString(SharedPreferencesDefs.Main.KEY_EMOTICON_HISTORY, null);
            sInstance = (serializedString != null)
                    ? fromSerializedString(context, serializedString)
                    : new EmoticonsHitCounter(context);
        }
        return sInstance;
    }

    private static EmoticonsHitCounter fromSerializedString(Context context, String serializedString) {
        JsonArray jsonArray = JSON_PARSER.parse(serializedString).getAsJsonArray();
        EmoticonsHitCounter instance = new EmoticonsHitCounter(context);
        for (String emoticon : CoreUtils.jsonArrayToNullableStringList(jsonArray)) {
            instance.hit(emoticon);
        }
        return instance;
    }

    private final Context mContext;

    private EmoticonsHitCounter(Context context) {
        super(QUEUE_LENGTH, INDEX_TO_WEIGHT_FUNCTION);
        mContext = context;
        for (String emoticon : INITIAL_EMOTICONS) {
            hit(emoticon);
        }
    }

    @Override
    public EmoticonsHitCounter reset() {
        super.reset();
        for (String emoticon : INITIAL_EMOTICONS) {
            hit(emoticon);
        }
        return this;
    }

    public List<String> getTopDefaultSize() {
        return super.getTop(INITIAL_EMOTICONS.size());
    }

    public EmoticonsHitCounter save() {
        // dumpQueue();
        mContext.getSharedPreferences(SharedPreferencesDefs.Main.FILE, Context.MODE_PRIVATE)
                .edit()
                .putString(SharedPreferencesDefs.Main.KEY_EMOTICON_HISTORY, toSerializedString())
                .commit();
        return this;
    }

    public EmoticonsHitCounter saveInBackground() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                save();
                return null;
            }
        }.execute();
        return this;
    }

    private String toSerializedString() {
        return CoreUtils.nullableStringListToJsonArray(Lists.newArrayList(getQueue())).toString();
    }

    private void dumpQueue() {
        StringBuilder string = new StringBuilder();
        for (String emoticon : getQueue()) {
            string.append(emoticon == null ? "-" : emoticon.codePointAt(0) % 100).append(", ");
        }
        Log.d("VFY", "EMOTICON QUEUE DUMP: " + string.toString());
        string = new StringBuilder();
        for (String emoticon : getTop(getQueue().size())) {
            string.append(emoticon == null ? "-" : emoticon.codePointAt(0) % 100).append(", ");
        }
        Log.d("VFY", "EMOTICON QUEUE TOP: " + string.toString());
    }
}
