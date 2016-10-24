package com.brufino.terpsychore.lib;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.*;

public class LimitedWeightedQueueHitCounter<T> {

    private Function<Integer, Double> mIndexToWeightFunction;
    private Queue<T> mQueue;

    public LimitedWeightedQueueHitCounter(Queue<T> queue, Function<Integer, Double> indexToWeightFunction) {
        mQueue = queue;
        mIndexToWeightFunction = indexToWeightFunction;
    }

    public LimitedWeightedQueueHitCounter(int queueLength, Function<Integer, Double> indexToWeightFunction) {
        mQueue =  new LinkedList<>();
        for (int i = 0; i < queueLength; i++) {
            mQueue.add(null);
        }
        mIndexToWeightFunction = indexToWeightFunction;
    }

    public void hit(T object) {
        mQueue.add(object);
        mQueue.remove();
    }

    public List<T> getTop(int k) {
        final Map<T, Double> mHitCount = new HashMap<>();
        PriorityQueue<T> mHeap = new PriorityQueue<>(11, new MapValueComparator<>(mHitCount));
        int i = 1;
        for (T object : Iterables.filter(mQueue, Predicates.<T>notNull())) {
            double value = mHitCount.containsKey(object) ? -mHitCount.get(object) : 0;
            double add = mIndexToWeightFunction.apply(mQueue.size() - i);
            mHitCount.put(object, -(value + add));
            mHeap.remove(object);
            mHeap.add(object);
            i++;
        }
        return Lists.newArrayList(Iterables.limit(mHeap, k));
    }

    public Queue<T> getQueue() {
        return mQueue;
    }
}
