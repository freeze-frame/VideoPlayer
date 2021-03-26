/*
 * Created on 2020-11-11 10:24:29 AM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.adapter;

import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liuzhenlin.common.utils.Synthetic;

/**
 * @author 刘振林
 */
public abstract class ImageLoadingListAdapter<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> {

    private RecyclerView mRecyclerView;
    private final RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            loadAllItemImagesIfNotScrolling(recyclerView);
        }
    };

    @Synthetic void loadAllItemImagesIfNotScrolling(RecyclerView recyclerView) {
        if (recyclerView.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            for (int i = recyclerView.getChildCount() - 1; i >= 0; i--) {
                View child = recyclerView.getChildAt(i);
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(child);
                if (adapter instanceof HeaderAndFooterWrapper) {
                    HeaderAndFooterWrapper<?> wrapper = (HeaderAndFooterWrapper<?>) adapter;
                    int position = holder.getAdapterPosition();
                    if (wrapper.isHeaderOrFooterPos(position)) {
                        continue;
                    }
                }
                //noinspection unchecked
                loadItemImages((VH) holder);
            }
        }
    }

    @CallSuper
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        recyclerView.addOnScrollListener(mScrollListener);
    }

    @CallSuper
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        mRecyclerView = null;
        recyclerView.removeOnScrollListener(mScrollListener);
    }

    @CallSuper
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        loadItemImagesIfNotScrolling(holder);
    }

    public void loadItemImagesIfNotScrolling(@NonNull VH holder) {
        RecyclerView parent = mRecyclerView;
        if (parent != null && parent.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
            loadItemImages(holder);
        }
    }

    @CallSuper
    @Override
    public void onViewDetachedFromWindow(@NonNull VH holder) {
        cancelLoadingItemImages(holder);
    }

    public abstract void loadItemImages(@NonNull VH holder);
    public abstract void cancelLoadingItemImages(@NonNull VH holder);
}
