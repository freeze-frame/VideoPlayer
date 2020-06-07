package com.liuzhenlin.swipeback;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.List;

public class SwipeBackFragment extends Fragment implements ISwipeBackFragment {

    private SwipeBackLayout mSwipeBackLayout;
    private static Animation sNoTransition;

    private boolean mTransitionEnabled = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSwipeBackLayout = (SwipeBackLayout) View.inflate(getContext(), R.layout.swipeback, null);
    }

    protected final SwipeBackLayout attachViewToSwipeBackLayout(@NonNull View view) {
        mSwipeBackLayout.attachFragmentView(this, view);
        return mSwipeBackLayout;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSwipeBackLayout = null;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        if (mTransitionEnabled) {
            return super.onCreateAnimation(transit, enter, nextAnim);
        } else {
            if (sNoTransition == null)
                sNoTransition = AnimationUtils.loadAnimation(getContext(), R.anim.no_anim);
            return sNoTransition;
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            // On this fragment hidden, we should set the previous fragment to be gone
            // since we may have made it visible during our dragging
            Fragment prefragment = (Fragment) getPreviousFragment();
            if (prefragment != null && prefragment.getView() != null) {
                prefragment.getView().setVisibility(View.GONE);
            }
        }
    }

    @Override
    public final SwipeBackLayout getSwipeBackLayout() {
        return mSwipeBackLayout;
    }

    @Override
    public final boolean isSwipeBackEnabled() {
        return mSwipeBackLayout.isGestureEnabled();
    }

    @Override
    public final void setSwipeBackEnabled(boolean enabled) {
        mSwipeBackLayout.setGestureEnabled(enabled);
    }

    @Override
    public final boolean isTransitionEnabled() {
        return mTransitionEnabled;
    }

    @Override
    public final void setTransitionEnabled(boolean enabled) {
        mTransitionEnabled = enabled;
    }

    @Nullable
    @Override
    public ISwipeBackFragment getPreviousFragment() {
        FragmentManager fm = getFragmentManager();
        if (fm == null) return null;

        List<Fragment> fragments = fm.getFragments();
        if (fragments.size() > 1) {
            final int index = fragments.indexOf(this);
            for (int i = index - 1; i >= 0; i--) {
                Fragment fragment = fragments.get(i);
                if (fragment != null && fragment.getView() != null) {
                    return (ISwipeBackFragment) fragment;
                }
            }
        }
        return null;
    }
}
