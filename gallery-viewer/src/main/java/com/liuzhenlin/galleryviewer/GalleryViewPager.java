package com.liuzhenlin.galleryviewer;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.widget.ViewDragHelper;
import androidx.viewpager.widget.ViewPager;

/**
 * @author 刘振林
 */
public class GalleryViewPager extends ViewPager {
    private static final String TAG = "GalleryViewPager";

    protected final int mTouchSlop;

    private int mActivePointerId = ViewDragHelper.INVALID_POINTER;
    private float mDownX;
    private float mDownY;

    private VelocityTracker mVelocityTracker;

    /**
     * The minimum velocity to fling this view when the image in the current page is magnified
     *
     * @see GestureImageView
     */
    private final float mMinimumFlingVelocityOnCurrImageMagnified; // 400 dp/s

    /** Position of the last selected page */
    /*synthetic*/ int mLastSelectedPageIndex;

    private final OnPageChangeListener mInternalOnPageChangeListener = new SimpleOnPageChangeListener() {
        @Override
        public void onPageSelected(int position) {
            if (mLastSelectedPageIndex != position && mItemCallback != null) {
                Object lastItem = mItemCallback.getItemAt(mLastSelectedPageIndex);
                if (lastItem instanceof GestureImageView) {
                    ((GestureImageView) lastItem).reinitializeImage();
                }

//                Object currentItem = mItemCallback.getItemAt(position);
//                if (currentItem instanceof GestureImageView) {
//                    GestureImageView image = (GestureImageView) currentItem;
//                    image.startImageOverScrollAndSpringBack(0, 0,
//                            position > mLastSelectedPageIndex ?
//                                    -image.mImageOverTranslation : image.mImageOverTranslation,
//                            0);
//                }
            }
            mLastSelectedPageIndex = position;
        }
    };

    public GalleryViewPager(@NonNull Context context) {
        this(context, null);
    }

    public GalleryViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMinimumFlingVelocityOnCurrImageMagnified = 400f * getResources().getDisplayMetrics().density;
        addOnPageChangeListener(mInternalOnPageChangeListener);
    }

    @Override
    public void clearOnPageChangeListeners() {
        super.clearOnPageChangeListeners();
        addOnPageChangeListener(mInternalOnPageChangeListener);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int actionMasked = ev.getAction() & MotionEvent.ACTION_MASK;

        if (actionMasked == MotionEvent.ACTION_DOWN) {
            initOrClearVelocityTracker();
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.addMovement(ev);
        }

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                onPointerDown(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(TAG, "Error processing scroll; pointer index for id "
                            + mActivePointerId + " not found. Did any MotionEvents get skipped?");
                    return false;
                }

                if (mItemCallback == null) break;
                Object item = mItemCallback.getItemAt(getCurrentItem());
                if (item instanceof GestureImageView) {
                    GestureImageView image = (GestureImageView) item;

                    if (ev.getPointerCount() != 1) {
                        return false;
                    }

                    final float absDx = Math.abs(ev.getX() - mDownX);
                    boolean intercept = absDx > mTouchSlop && absDx > Math.abs(ev.getY() - mDownY);
                    if (!intercept) return false;

                    RectF imgBounds = image.mImageBounds;
                    image.getImageBounds(imgBounds);
                    if (imgBounds.isEmpty()) return true;

                    final int imgAvailableWidth =
                            image.getWidth() - image.getPaddingLeft() - image.getPaddingRight();
                    if (imgBounds.width() > imgAvailableWidth) {
                        mVelocityTracker.computeCurrentVelocity(1000);
                        final float vx = mVelocityTracker.getXVelocity(mActivePointerId);

                        // Account for very small floating-point error (+/- 0.01f)
                        final boolean canScrollPageRight = imgBounds.left >= -0.01f
                                && vx >= mMinimumFlingVelocityOnCurrImageMagnified;
                        final boolean canScrollPageLeft = imgBounds.right <= imgAvailableWidth + 0.01f
                                && vx <= -mMinimumFlingVelocityOnCurrImageMagnified;

                        intercept = canScrollPageLeft || canScrollPageRight;
                    }

                    if (intercept) {
                        mActivePointerId = ViewDragHelper.INVALID_POINTER;
                        recycleVelocityTracker();

                        ViewParent parent = getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    return intercept;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = ViewDragHelper.INVALID_POINTER;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private void onPointerDown(MotionEvent e) {
        final int actionIndex = e.getActionIndex();
        mActivePointerId = e.getPointerId(actionIndex);
        mDownX = e.getX(actionIndex);
        mDownY = e.getY(actionIndex);
    }

    private void onSecondaryPointerUp(MotionEvent e) {
        final int pointerIndex = e.getActionIndex();
        final int pointerId = e.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up.
            // Choose a new active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = e.getPointerId(newPointerIndex);
            mDownX = e.getX(newPointerIndex);
            mDownY = e.getY(newPointerIndex);
        }
    }

    private void initOrClearVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /*synthetic*/ ItemCallback mItemCallback;

    public void setItemCallback(@Nullable ItemCallback callback) {
        mItemCallback = callback;
    }

    public interface ItemCallback {
        /**
         * @param position the <strong>adapter position</strong> of the item that you want to get
         * @return the item at the specified position
         */
        Object getItemAt(int position);
    }
}
