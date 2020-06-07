package com.liuzhenlin.swipeback;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SwipeBackActivity extends AppCompatActivity implements ISwipeBackActivity {

    private SwipeBackLayout mSwipeBackLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setWindowAnimations(R.style.WindowAnimations_SwipeBackActivity);

        mSwipeBackLayout = (SwipeBackLayout) View.inflate(this, R.layout.swipeback, null);
        mSwipeBackLayout.attachToActivity(this);
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
    public boolean canSwipeBackToFinish() {
        return getSupportFragmentManager().getFragments().size() <= 1;
    }

    /*
     * This default implementation just returns 'null'. Subclasses are encouraged to override this
     * to provide simultaneous scrolling of the previous and the current activities's content views
     * while the current one is involved in a swipe-back.
     */
    @Nullable
    @Override
    public Activity getPreviousActivity() {
        return null;
    }
}
