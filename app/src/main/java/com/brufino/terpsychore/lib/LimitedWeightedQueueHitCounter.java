package com.brufino.terpsychore.lib;

import com.google.common.base.Function;

import java.util.*;

public class LimitedWeightedQueueHitCounter<T> {

    private Function<Integer, Double> mIndexToWeightFunction;
    private Queue<T> mQueue;

    public LimitedWeightedQueueHitCounter(int queueSize, Function<Integer, Double> indexToWeightFunction) {
        mQueue =  new LinkedList<>();
        for (int i = 0; i < queueSize; i++) {
            mQueue.add(null);
        }
        mIndexToWeightFunction = indexToWeightFunction;
    }

    public LimitedWeightedQueueHitCounter reset() {
        for (int i = 0; i < mQueue.size(); i++) {
            hit(null);
        }
        return this;
    }

    public LimitedWeightedQueueHitCounter hit(T object) {
        mQueue.add(object);
        mQueue.remove();
        return this;
    }

    public List<T> getTop(int k) {
        final Map<T, Double> mHitCount = new HashMap<>();
        PriorityQueue<T> mHeap = new PriorityQueue<>(11, new MapValueComparator<>(mHitCount));
        int i = 1;
        for (T object : mQueue) {
            if (object == null) {
                i++;
                continue;
            }
            double value = mHitCount.containsKey(object) ? -mHitCount.get(object) : 0;
            double add = mIndexToWeightFunction.apply(mQueue.size() - i);
            mHitCount.put(object, -(value + add));
            mHeap.remove(object);
            mHeap.add(object);
            i++;
        }
        List<T> top = new ArrayList<>(k);
        for (i = 0; i < k; i++) {
            top.add(mHeap.poll());
        }
        return top;
    }

    public Queue<T> getQueue() {
        return mQueue;
    }
}
