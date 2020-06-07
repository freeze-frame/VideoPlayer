package com.liuzhenlin.floatingmenu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FloatingMenu extends PopupWindow {

    /** Menu tag name in XML. */
    private static final String XML_TAG_MENU = "menu";

    /** Group tag name in XML. */
    private static final String XML_TAG_GROUP = "group";

    /** Item tag name in XML. */
    private static final String XML_TAG_ITEM = "item";

    private static final int GRAVITY = Gravity.TOP | Gravity.START;

    private final Context mContext;

    private final View mAnchorView;

    private final int mScreenWidth;
    private final int mScreenHeight;

    private final List<MenuItem> mMenuItems = new ArrayList<>();

    private LinearLayout mMenuLayout;

    // Match the width of the contentView of this menu
    private static final int DEFAULT_ITEM_WIDTH = ViewGroup.LayoutParams.MATCH_PARENT;

    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    private View.OnClickListener mOnClickListener;
    private View.OnLongClickListener mOnLongClickListener;

    private int mDownX;
    private int mDownY;

    public FloatingMenu(@NonNull View anchor) {
        super(anchor.getContext());
        setFocusable(true);
        setBackgroundDrawable(new BitmapDrawable());

        mContext = anchor.getContext();
        mAnchorView = anchor;
        mAnchorView.setOnTouchListener(new OnMenuTouchListener(this));
        mScreenWidth = DensityUtils.getScreenWidth(mContext);
        mScreenHeight = DensityUtils.getScreenHeight(mContext);
    }

    public void inflate(@MenuRes int menuRes) {
        inflate(menuRes, DEFAULT_ITEM_WIDTH);
    }

    public void inflate(@MenuRes int menuRes, int itemWidth) {
        @SuppressLint("ResourceType")
        XmlResourceParser parser = mContext.getResources().getLayout(menuRes);
        AttributeSet attrs = Xml.asAttributeSet(parser);
        try {
            parseMenu(parser, attrs);
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        } finally {
            parser.close();
        }
        generateLayout(itemWidth);
    }

    private void parseMenu(XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        int eventType = parser.getEventType();
        String tagName;
        // This loop will skip to the menu start tag
        do {
            if (eventType == XmlPullParser.START_TAG) {
                tagName = parser.getName();
                if (XML_TAG_MENU.equals(tagName)) {
                    // Go to next tag
                    eventType = parser.next();
                    break;
                }

                throw new RuntimeException("Expecting menu, got " + tagName);
            }

            eventType = parser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);

        boolean reachedEndOfMenu = false;
        boolean lookingForEndOfUnknownTag = false;
        String unknownTagName = null;
        while (!reachedEndOfMenu) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (lookingForEndOfUnknownTag) {
                        break;
                    }

                    tagName = parser.getName();
                    if (XML_TAG_ITEM.equals(tagName)) {
                        readItem(attrs);
                    } else {
                        lookingForEndOfUnknownTag = true;
                        unknownTagName = tagName;
                    }
                    break;

                case XmlPullParser.END_TAG:
                    tagName = parser.getName();
                    if (lookingForEndOfUnknownTag && tagName.equals(unknownTagName)) {
                        lookingForEndOfUnknownTag = false;
                        unknownTagName = null;
                    } else if (XML_TAG_MENU.equals(tagName)) {
                        reachedEndOfMenu = true;
                    }
                    break;

                case XmlPullParser.END_DOCUMENT:
                    throw new RuntimeException("Unexpected end of document!");
            }

            eventType = parser.next();
        }
    }

    private void readItem(AttributeSet attrs) {
        TypedArray ta = mContext.obtainStyledAttributes(attrs, R.styleable.MenuItem);
        final int iconResId = ta.getResourceId(R.styleable.MenuItem_icon, 0);
        final String text = ta.getText(R.styleable.MenuItem_text).toString();
        ta.recycle();

        MenuItem item = new MenuItem();
        item.setIconResId(iconResId);
        item.setText(text);

        mMenuItems.add(item);
    }

    public void items(@Nullable String... items) {
        items(DEFAULT_ITEM_WIDTH, items);
    }

    public void items(int itemWidth, @Nullable String... items) {
        mMenuItems.clear();
        if (items != null) {
            for (String item : items) {
                mMenuItems.add(new MenuItem(item));
            }
        }
        generateLayout(itemWidth);
    }

    public <T extends MenuItem> void items(@Nullable List<T> items) {
        items(items, DEFAULT_ITEM_WIDTH);
    }

    public <T extends MenuItem> void items(@Nullable List<T> items, int itemWidth) {
        mMenuItems.clear();
        if (items != null) {
            mMenuItems.addAll(items);
        }
        generateLayout(itemWidth);
    }

    private void generateLayout(int itemWidth) {
        mMenuLayout = new LinearLayout(mContext);
        mMenuLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        mMenuLayout.setBackgroundDrawable(ContextCompat.getDrawable(mContext, R.drawable.bg_shadow));
        mMenuLayout.setOrientation(LinearLayout.VERTICAL);

        final int padding = DensityUtils.dp2px(mContext, 12);
        for (int i = 0, itemCount = mMenuItems.size(); i < itemCount; i++) {
            MenuItem menuItem = mMenuItems.get(i);

            TextView textView = new TextView(mContext);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    itemWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            ViewCompat.setPaddingRelative(textView, padding, padding, padding * 2, padding);
            textView.setBackgroundDrawable(ContextCompat.getDrawable(mContext, R.drawable.selector_item));
            textView.setFocusable(true);
            textView.setClickable(true);
            textView.setText(menuItem.getText());
            textView.setTextSize(15);
            textView.setTextColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                textView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            if (menuItem.getIconResId() != 0) {
                Drawable icon = AppCompatResources.getDrawable(mContext, menuItem.getIconResId());
                if (icon != null) {
                    textView.setCompoundDrawablePadding(padding);
                    if (ViewCompat.getLayoutDirection(mAnchorView) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                        textView.setCompoundDrawablesWithIntrinsicBounds(
                                icon, null, null, null);
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(
                                null, null, icon, null);
                    }
                }
            }

            mMenuLayout.addView(textView);
        }

        setOnItemClickListener(mOnItemClickListener);
        setOnItemLongClickListener(mOnItemLongClickListener);

        mMenuLayout.measure(
                View.MeasureSpec.makeMeasureSpec(mScreenWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(mScreenHeight, View.MeasureSpec.AT_MOST));
        setWidth(mMenuLayout.getMeasuredWidth());
        setHeight(mMenuLayout.getMeasuredHeight());
        setContentView(mMenuLayout);
    }

    public void show(int x, int y) {
        mDownX = x;
        mDownY = y;
        show();
    }

    public void show() {
        if (isShowing()) {
            return;
        }
//        // It is must, otherwise 'setFocusable' will not work below Android 6.0
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            setBackgroundDrawable(new BitmapDrawable());
//        }

        final int height = getHeight();
        if (mDownX <= mScreenWidth / 2) {
            if (mDownY + height < mScreenHeight) {
                setAnimationStyle(R.style.Animation_top_left);
                showAtLocation(mAnchorView, GRAVITY, mDownX, mDownY);
            } else {
                setAnimationStyle(R.style.Animation_bottom_left);
                showAtLocation(mAnchorView, GRAVITY, mDownX, mDownY - height);
            }
        } else {
            if (mDownY + height < mScreenHeight) {
                setAnimationStyle(R.style.Animation_top_right);
                showAtLocation(mAnchorView, GRAVITY, mDownX - getWidth(), mDownY);
            } else {
                setAnimationStyle(R.style.Animation_bottom_right);
                showAtLocation(mAnchorView, GRAVITY, mDownX - getWidth(), mDownY - height);
            }
        }
    }

    public void setOnItemClickListener(@Nullable OnItemClickListener listener) {
        mOnItemClickListener = listener;
        if (listener == null) {
            mOnClickListener = null;

        } else if (mOnClickListener == null) {
            mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int position = mMenuLayout.indexOfChild(v);
                    if (position != -1) {
                        dismiss();
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onClick(mMenuItems.get(position), position);
                        }
                    }
                }
            };
        }
        for (int i = mMenuLayout.getChildCount() - 1; i >= 0; i--) {
            mMenuLayout.getChildAt(i).setOnClickListener(mOnClickListener);
        }
    }

    public void setOnItemLongClickListener(@Nullable OnItemLongClickListener listener) {
        mOnItemLongClickListener = listener;
        if (listener == null) {
            mOnLongClickListener = null;

        } else if (mOnLongClickListener == null) {
            mOnLongClickListener = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final int position = mMenuLayout.indexOfChild(v);
                    if (position != -1) {
//                        dismiss();
                        if (mOnItemLongClickListener != null) {
                            return mOnItemLongClickListener.onLongClick(
                                    mMenuItems.get(position), position);
                        }
                    }
                    return false;
                }
            };
        }
        for (int i = mMenuLayout.getChildCount() - 1; i >= 0; i--) {
            mMenuLayout.getChildAt(i).setOnLongClickListener(mOnLongClickListener);
        }
    }

    public interface OnItemClickListener {
        void onClick(@NonNull MenuItem menuItem, int position);
    }

    public interface OnItemLongClickListener {
        boolean onLongClick(@NonNull MenuItem menuItem, int position);
    }

    private static final class OnMenuTouchListener implements View.OnTouchListener {
        final WeakReference<FloatingMenu> menuRef;

        OnMenuTouchListener(FloatingMenu menu) {
            menuRef = new WeakReference<>(menu);
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            FloatingMenu menu = menuRef.get();
            if (menu != null) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    menu.mDownX = (int) event.getRawX();
                    menu.mDownY = (int) event.getRawY();
                }
            }
            return false;
        }
    }
}
