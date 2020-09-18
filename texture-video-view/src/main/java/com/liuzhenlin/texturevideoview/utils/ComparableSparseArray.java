/*
 * Created on 2020-3-24 6:11:17 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.util.SparseArray;

import androidx.collection.SparseArrayCompat;

/**
 * @author 刘振林
 */
public final class ComparableSparseArray<E> extends SparseArray<E> implements Cloneable {

    public ComparableSparseArray() {
    }

    public ComparableSparseArray(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (o instanceof SparseArray) {
            SparseArray<?> m = (SparseArray<?>) o;

            int s = m.size();
            if (s != size())
                return false;

            for (int i = 0; i < s; i++) {
                int key = keyAt(i);
                E value = valueAt(i);
                if (value == null) {
                    if (!(m.get(key) == null && m.indexOfKey(key) >= 0))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
            return true;

        } else if (o instanceof SparseArrayCompat) {
            SparseArrayCompat<?> m = (SparseArrayCompat<?>) o;

            int s = m.size();
            if (s != size())
                return false;

            for (int i = 0; i < s; i++) {
                int key = keyAt(i);
                E value = valueAt(i);
                if (value == null) {
                    if (!(m.get(key) == null && m.indexOfKey(key) >= 0))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0, s = size(); i < s; i++) {
            int key = keyAt(i);
            E value = valueAt(i);
            result += key ^ (value == null ? 0 : value.hashCode());
        }
        return result;
    }

    @Override
    public ComparableSparseArray<E> clone() {
        return (ComparableSparseArray<E>) super.clone();
    }
}
