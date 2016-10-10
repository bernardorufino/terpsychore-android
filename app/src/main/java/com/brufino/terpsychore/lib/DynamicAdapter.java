package com.brufino.terpsychore.lib;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.*;

/**
 * LayoutManager of the RecyclerView must be a LinearLayoutManager
 */
public abstract class DynamicAdapter<I, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected List<I> mList = new ArrayList<>();
    private int mItemsPerRequest;
    private int mTriggerMargin;
    private boolean mComplete = false;
    private boolean mLoading = false;
    private RecyclerView mRecyclerView;

    public DynamicAdapter(int itemsPerRequest, int triggerMargin) {
        mItemsPerRequest = itemsPerRequest;
        mTriggerMargin = triggerMargin;
    }

    public boolean isLoading() {
        return mLoading;
    }

    public void reset() {
        mList.clear();
        mComplete = false;
        mLoading = false;
    }

    public void firstLoad() {
        mLoading = true;
        loadItems(0, mItemsPerRequest);
    }

    protected void addItems(Collection<? extends I> items) {
        checkArgument(items.size() <= mItemsPerRequest, "Can't load more items per requested than the one provided");
        if (items.size() < mItemsPerRequest) {
            mComplete = true;
            mRecyclerView.removeOnScrollListener(mOnScrollListener);
        }
        mList.addAll(items);
        notifyDataSetChanged();
    }

    /**
     * Should implement this to load a subset of the items. After the load is complete should call {@link #addItems}.
     *
     * @param offset The index of the first item to be loaded from this window.
     * @param limit The number of items in the window.
     */
    protected abstract void loadItems(int offset, int limit);

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.addOnScrollListener(mOnScrollListener);
        Log.d("VFY", "onAttachedToRecyclerView()");
    }

    private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

        private int mPreviousTotal = 0;

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (mComplete || recyclerView.getAdapter() != DynamicAdapter.this) {
                mRecyclerView.removeOnScrollListener(this);
                return;
            }

            RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
            checkState(layoutManager instanceof LinearLayoutManager, "RecyclerView's LayoutManager should be LinearLayoutManager");
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;

            int visibleItemCount = mRecyclerView.getChildCount();
            int totalItemCount = linearLayoutManager.getItemCount();
            int firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition();

            if (mLoading && totalItemCount != mPreviousTotal) {
                /* TODO: What if empty result or error? */
                mLoading = false;
                mPreviousTotal = totalItemCount;
            }

            if (!mLoading && (firstVisibleItem + visibleItemCount) >= (totalItemCount - mTriggerMargin)) {
                mLoading = true;
                loadItems(mList.size(), mItemsPerRequest);
            }
        }
    };

    protected void reportError() {
        mLoading = false;
    }

    public I getItem(int position) {
        return mList.get(position);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        I item = mList.get(position);
        onBindViewHolder(holder, position, item);
    }

    public abstract void onBindViewHolder(VH holder, int position, I item);

    @Override
    public int getItemCount() {
        return mList.size();
    }

}
