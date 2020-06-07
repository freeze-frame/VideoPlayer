/*
 * Created on 2020-2-22 9:24:20 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.model;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

/**
 * @author 刘振林
 */
public class TV {
    private String name;
    private String url;

    public TV() {
    }

    public TV(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        TV tv = (TV) o;
        return ObjectsCompat.equals(name, tv.name) &&
                ObjectsCompat.equals(url, tv.url);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(name, url);
    }

    @NonNull
    @Override
    public String toString() {
        return "TV{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
