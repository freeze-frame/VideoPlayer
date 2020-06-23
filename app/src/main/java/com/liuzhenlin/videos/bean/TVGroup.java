/*
 * Created on 2020-2-22 10:49:50 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.bean;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import java.util.Arrays;

/**
 * @author 刘振林
 */
public class TVGroup {
    private String name;
    private TV[] tvs;

    public TVGroup() {
    }

    public TVGroup(String name, TV[] tvs) {
        this.name = name;
        this.tvs = tvs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TV[] getTVs() {
        return tvs;
    }

    public void setTVs(TV[] tvs) {
        this.tvs = tvs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TVGroup tvGroup = (TVGroup) o;
        return ObjectsCompat.equals(name, tvGroup.name) &&
                Arrays.equals(tvs, tvGroup.tvs);
    }

    @Override
    public int hashCode() {
        int result = ObjectsCompat.hash(name);
        result = 31 * result + Arrays.hashCode(tvs);
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "TVGroup{" +
                "name='" + name + '\'' +
                ", tvs=" + Arrays.toString(tvs) +
                '}';
    }
}
