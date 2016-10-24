package com.brufino.terpsychore.lib;

import com.google.common.collect.Ordering;

import java.util.Comparator;
import java.util.Map;

public class MapValueComparator<K, V extends Comparable<V>> implements Comparator<K> {

    private final Map<K, V> mMap;

    public MapValueComparator(Map<K, V> map) {
        mMap = map;
    }

    @Override
    public int compare(K a, K b) {
        return Ordering.natural().compare(mMap.get(a), mMap.get(b));
    }
}
