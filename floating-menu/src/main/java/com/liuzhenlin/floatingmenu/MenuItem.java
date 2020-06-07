package com.liuzhenlin.floatingmenu;

import androidx.annotation.NonNull;

public class MenuItem {

    private CharSequence text;
    private int iconResId;

    public MenuItem() {
    }

    public MenuItem(String text) {
        this.text = text;
    }

    public MenuItem(String text, int iconResId) {
        this.text = text;
        this.iconResId = iconResId;
    }

    public CharSequence getText() {
        return text;
    }

    public void setText(CharSequence item) {
        this.text = item;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int itemResId) {
        this.iconResId = itemResId;
    }

    @NonNull
    @Override
    public String toString() {
        return "MenuItem{" +
                "text='" + text + '\'' +
                '}';
    }
}
