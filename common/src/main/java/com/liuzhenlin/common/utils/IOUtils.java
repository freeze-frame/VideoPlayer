/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.liuzhenlin.common.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Supports various IO utility functions
 */
public class IOUtils {
    private IOUtils() {
    }

    private static final int BUF_SIZE = 0x2000; // 8K
    private static final int MAX_INT = 0x7fffffff;
    private static final String TAG = "IOUtils";

    @NonNull
    public static byte[] toByteArray(@NonNull File file) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return toByteArray(in);
        } finally {
            closeSilently(in);
        }
    }

    @NonNull
    public static byte[] toByteArray(@NonNull InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(@NonNull InputStream from, @NonNull OutputStream to) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        int r;
        while ((r = from.read(buf)) != -1) {
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    /**
     * Gets the hex string result of the hash computation based on the given algorithm
     * (MD5, SHA-1, SHA-256, etc.) for the input stream.
     */
    @NonNull
    public static String getDigest(@NonNull InputStream in, @NonNull String algorithm)
            throws IOException, NoSuchAlgorithmException {
        return getDigest(in, -1, algorithm);
    }

    /**
     * Gets the hex string result of the hash computation based on the given algorithm
     * (MD5, SHA-1, SHA-256, etc.) for the input stream.
     *
     * @param limit the maximum limit of bytes that can be read, using negative value for no limit.
     */
    @NonNull
    public static String getDigest(@NonNull InputStream in, long limit, @NonNull String algorithm)
            throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[Math.min(BUF_SIZE, limit < 0 ? MAX_INT : (int) limit & MAX_INT)];
        long bytesRead = 0;
        int bytesToRead;
        int len;
        for (; ; ) {
            bytesToRead = limit < 0
                    ? buffer.length : Math.min((int) (limit - bytesRead) & MAX_INT, buffer.length);
            if (bytesToRead == 0) {
                break;
            }
            len = in.read(buffer, 0, bytesToRead);
            if (len == -1) {
                break;
            }
            bytesRead += len;
            digest.update(buffer, 0, len);
        }
        return new BigInteger(1, digest.digest()).toString(16);
    }

    /** Read through the specified input stream and convert it to a UTF-8 encoded string. */
    @Nullable
    public static String decodeStringFromStream(@NonNull InputStream in) throws IOException {
        Reader reader = null;
        try {
            StringBuilder json = null;
            //noinspection CharsetObjectCanBeUsed
            reader = new InputStreamReader(in, "utf-8");
            char[] buffer = new char[BUF_SIZE];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                if (json == null) {
                    json = new StringBuilder();
                }
                json.append(buffer, 0, len);
            }
            return json == null ? null : json.toString();
        } finally {
            closeSilently(reader);
        }
    }

    public static void closeSilently(@Nullable Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                //
            }
        }
    }
}
