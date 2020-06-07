package com.liuzhenlin.videos.view.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import com.liuzhenlin.galleryviewer.GalleryViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author 刘振林
 */
public class GalleryPagerAdapter<V extends View> extends PagerAdapter
        implements GalleryViewPager.ItemCallback {

    @NonNull
    public final List<V> views;

    public GalleryPagerAdapter(@Nullable V[] views) {
        if (views == null) {
            this.views = new ArrayList<>(0);
        } else {
            this.views = new ArrayList<>(views.length);
            if (views.length > 0) {
                this.views.addAll(Arrays.asList(views));
            }
        }
    }

    public GalleryPagerAdapter(@Nullable Collection<V> views) {
        this.views = views == null ? new ArrayList<V>(0) : new ArrayList<V>(views);
    }

    @Override
    public int getCount() {
        return views.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = views.get(position);
        if (view.getParent() == null) {
            container.addView(view);
        }
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return POSITION_NONE;
    }

    @Override
    public Object getItemAt(int position) {
        if (position >= 0 && position < views.size()) {
            return views.get(position);
        }
        return null;
    }
}