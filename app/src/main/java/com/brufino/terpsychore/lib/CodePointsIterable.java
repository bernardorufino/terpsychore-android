package com.brufino.terpsychore.lib;

import java.util.Iterator;

public class CodePointsIterable implements Iterable<Integer> {

    private final String mString;

    public CodePointsIterable(String string) {
        mString = string;
    }

    @Override
    public Iterator<Integer> iterator() {
        return new CodePointsIterator(mString);
    }

    private static class CodePointsIterator implements Iterator<Integer> {

        private final String mString;
        private int mIndex;

        public CodePointsIterator(String string) {
            mString = string;
            mIndex = 0;
        }

        @Override
        public boolean hasNext() {
            return mIndex < mString.length();
        }

        @Override
        public Integer next() {
            int codePoint = mString.codePointAt(mIndex);
            mIndex += Character.charCount(codePoint);
            return codePoint;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
