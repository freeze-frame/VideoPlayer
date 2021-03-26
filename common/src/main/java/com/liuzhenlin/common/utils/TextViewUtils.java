/*
 * Created on 2017/11/09.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * @author 刘振林
 */
public class TextViewUtils {
    private TextViewUtils() {
    }

    public static int getTextWidth(@NonNull TextView tv) {
//        CharSequence text = tv.getText();
        String text = tv.getText().toString();
        Rect bounds = new Rect();
        TextPaint paint = tv.getPaint();
        paint.getTextBounds(text, 0, text.length(), bounds);
        return bounds.width();
    }

    public static int getTextHeight(@NonNull TextView tv) {
        TextPaint paint = tv.getPaint();
        paint.setTextSize(tv.getTextSize());
        Paint.FontMetrics fm = paint.getFontMetrics();
        return Utils.roundFloat(fm.descent - fm.ascent);
    }

    public static int getLineHeight(@NonNull TextView tv) {
        TextPaint paint = tv.getPaint();
        paint.setTextSize(tv.getTextSize());
        Paint.FontMetrics fm = paint.getFontMetrics();
        return Utils.roundFloat(fm.bottom - fm.top);
    }

    public static int getLineSpacingHeight(@NonNull TextView tv) {
        TextPaint paint = tv.getPaint();
        paint.setTextSize(tv.getTextSize());
        Paint.FontMetrics fm = paint.getFontMetrics();
        return Utils.roundFloat(fm.leading);
    }

    public static void setHangingIndents(@NonNull TextView tv, int indentSpaceCount) {
        if (indentSpaceCount <= 0) {
            return;
        }
        final String text = tv.getText().toString();
        if (text.isEmpty()) {
            return;
        }
        if (tv.getMeasuredWidth() == 0) {
            Log.e("TextViewUtils", "Failed to set hanging intents for the TextView " + tv
                    + " because its width has not been determined yet."
                    + " Probably you need to call this method after its layout is done.");
            return;
        }

        final int availableWidth = tv.getMeasuredWidth() - tv.getPaddingLeft() - tv.getPaddingRight();
        final Paint paint = tv.getPaint(); // 画笔，包含字体信息

        // 将缩进处理成空格
        final StringBuilder indentSpace = new StringBuilder();
        final float spaceWidth = paint.measureText(" ");
        int i = 0;
        for (; i < indentSpaceCount && i * spaceWidth < availableWidth; i++) {
            indentSpace.append(" ");
        }
        final float indentWidth = i * spaceWidth;

        final StringBuilder newText = new StringBuilder();
        // 将原始文本按行拆分
        final String[] textLines = text.replaceAll("\r", "").split("\n");
        for (String textLine : textLines) {
            if (paint.measureText(textLine) <= availableWidth) {
                // 如果整行宽度在控件可用宽度之内，就不处理了
                newText.append(textLine);
            } else {
                // 如果整行宽度超过控件可用宽度，则按字符测量，在超过可用宽度的前一个字符处手动换行
                float lineWidth = 0;
                for (int index = 0, length = textLine.length(); index < length; index++) {
                    final char ch = textLine.charAt(index);
                    lineWidth += paint.measureText(String.valueOf(ch));
                    if (lineWidth <= availableWidth) {
                        newText.append(ch);
                    } else {
                        newText.append("\n");
                        lineWidth = 0;
                        index--;
                    }
                    // 从手动换行的第二行开始，加上悬挂缩进
                    if (lineWidth < 0.1f && index != 0) {
                        newText.append(indentSpace);
                        lineWidth += indentWidth;
                    }
                }
            }
            newText.append("\n");
        }
        // 把结尾多余的\n去掉
        if (!text.endsWith("\n")) {
            newText.deleteCharAt(newText.length() - 1);
        }

        tv.setText(newText);
    }
}
