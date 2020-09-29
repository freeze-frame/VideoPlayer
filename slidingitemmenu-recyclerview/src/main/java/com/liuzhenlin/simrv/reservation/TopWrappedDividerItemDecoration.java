package com.liuzhenlin.simrv.reservation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TopWrappedDividerItemDecoration extends RecyclerView.ItemDecoration {
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;
    public static final int VERTICAL = LinearLayout.VERTICAL;

    private static final String TAG = "TopWrappedDividerItemDecoration";
    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private Drawable mDivider;

    /**
     * Current orientation. Either {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    private int mOrientation;

    private final Rect mBounds = new Rect();

    /**
     * Creates a divider {@link RecyclerView.ItemDecoration} that can be used with a
     * {@link LinearLayoutManager}.
     *
     * @param context     Current context, it will be used to access resources.
     * @param orientation Divider orientation. Should be {@link #HORIZONTAL} or {@link #VERTICAL}.
     */
    @SuppressLint("LongLogTag")
    public TopWrappedDividerItemDecoration(Context context, int orientation) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        mDivider = a.getDrawable(0);
        if (mDivider == null) {
            Log.w(TAG, "@android:attr/listDivider was not set in the theme used for this "
                    + "DividerItemDecoration. Please set that attribute all call setDivider()");
        }
        a.recycle();
        setOrientation(orientation);
    }

    /**
     * @return the orientation of this divider, either {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * Sets the orientation for this divider. This should be called if
     * {@link RecyclerView.LayoutManager} changes orientation.
     *
     * @param orientation {@link #HORIZONTAL} or {@link #VERTICAL}
     */
    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException(
                    "Invalid orientation. It should be either HORIZONTAL or VERTICAL");
        }
        mOrientation = orientation;
    }

    /**
     * @return the Drawable for this divider
     */
    @NonNull
    public Drawable getDivider() {
        return mDivider;
    }

    /**
     * Sets the {@link Drawable} for this divider.
     *
     * @param divider Drawable that should be used as a divider.
     */
    public void setDivider(@NonNull Drawable divider) {
        mDivider = divider;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (parent.getLayoutManager() == null || mDivider == null)
            return;
        if (mOrientation == VERTICAL)
            drawVertical(c, parent);
        else
            drawHorizontal(c, parent);
    }

    private void drawVertical(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int left, right;
        if (parent.getClipToPadding()) {
            left = parent.getPaddingLeft();
            right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(left, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            left = 0;
            right = parent.getWidth();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int bottom = mBounds.bottom + Math.round(child.getTranslationY());
            final int top = bottom - mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
            // Draw the divider for RecyclerView's top edge
            if (i == 0) {
                mDivider.setBounds(left, parent.getPaddingTop(), right,
                        parent.getPaddingTop() + mDivider.getIntrinsicHeight());
                mDivider.draw(canvas);
            }
        }
        canvas.restore();
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent) {
        canvas.save();
        final int top, bottom;
        if (parent.getClipToPadding()) {
            top = parent.getPaddingTop();
            bottom = parent.getHeight() - parent.getPaddingBottom();
            canvas.clipRect(parent.getPaddingLeft(), top,
                    parent.getWidth() - parent.getPaddingRight(), bottom);
        } else {
            top = 0;
            bottom = parent.getHeight();
        }

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, mBounds);
            final int right = mBounds.right + Math.round(child.getTranslationX());
            final int left = right - mDivider.getIntrinsicWidth();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(canvas);
            // Draw the divider for RecyclerView's horizontal start edge
            if (i == 0) {
                mDivider.setBounds(parent.getPaddingLeft(), top,
                        parent.getPaddingLeft() + mDivider.getIntrinsicWidth(), bottom);
                mDivider.draw(canvas);
            }
        }
        canvas.restore();
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mDivider == null) {
            outRect.set(0, 0, 0, 0);
            return;
        }
        if (mOrientation == VERTICAL) {
            final int dividerHeight = mDivider.getIntrinsicHeight();
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.set(0, dividerHeight, 0, dividerHeight);
            } else {
                outRect.set(0, 0, 0, dividerHeight);
            }
        } else {
            final int dividerWidth = mDivider.getIntrinsicWidth();
            if (parent.getChildAdapterPosition(view) == 0) {
                outRect.set(dividerWidth, 0, dividerWidth, 0);
            } else {
                outRect.set(0, 0, dividerWidth, 0);
            }
        }
    }
}