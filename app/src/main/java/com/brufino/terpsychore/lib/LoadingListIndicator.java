package com.brufino.terpsychore.lib;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public class LoadingListIndicator<VH extends RecyclerView.ViewHolder & LoadingListIndicator.Item> {

    private View vLoading = null;
    private RecyclerView vList;

    private Set<Item> mLoadingViewHolders = new HashSet<>();
    private boolean mLoading;

    public LoadingListIndicator(RecyclerView listView) {
        this.vList = listView;
    }

    public LoadingListIndicator(RecyclerView listView, View loadingView) {
        vList = listView;
        vLoading = loadingView;
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
        if (loading) {
            if (vList.getChildCount() == 0 && vLoading != null) {
                vLoading.setVisibility(View.VISIBLE);
            } // else it's handled in mAdapter.onBindViewHolder()
        } else {
            if (vLoading != null) {
                vLoading.setVisibility(View.GONE);
            }
            for (Item viewHolder : mLoadingViewHolders) {
                viewHolder.setLoading(false);
            }
            mLoadingViewHolders.clear();
        }
    }

    public void onBindViewHolder(Item holder, int position, RecyclerView.Adapter<VH> adapter) {
        holder.setLoading(mLoading && position == adapter.getItemCount() - 1);
        mLoadingViewHolders.add(holder);
    }

    public static interface Item {
        public void setLoading(boolean loading);
    }
}
