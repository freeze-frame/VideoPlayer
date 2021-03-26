/*
 * Created on 2018/09/05.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import androidx.annotation.CallSuper;

/**
 * @author 刘振林
 */
public abstract class BaseAdapter2 extends BaseAdapter {
    private AdapterView<? extends Adapter> mAdapterView;

    @CallSuper
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        mAdapterView = (AdapterView<? extends Adapter>) parent;
        return convertView;
    }

    public void notifyItemChanged(int position) {
        if (mAdapterView == null) return;

        final int headerCount;
        if (mAdapterView instanceof ListView) {
            headerCount = ((ListView) mAdapterView).getHeaderViewsCount();
        } else {
            headerCount = 0;
        }
        final int unwrappedPosition = headerCount + position;

        final int firstVisiblePosition = mAdapterView.getFirstVisiblePosition();
        final int lastVisiblePosition = mAdapterView.getLastVisiblePosition();

        // 在可见范围内的才更新；不可见的在列表滚动后，列表会自动调用getView方法进行更新
        if (unwrappedPosition >= firstVisiblePosition && unwrappedPosition <= lastVisiblePosition) {
            int itemIndex = unwrappedPosition - firstVisiblePosition;
            // 获取并更新指定位置的itemView
            View itemView = mAdapterView.getChildAt(itemIndex);
            getView(position, itemView, mAdapterView);
        }
    }
}
