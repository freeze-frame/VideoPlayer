/*
 * Created on 2017/09/27.
 * Copyright © 2017 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.liuzhenlin.texturevideoview.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * @author 刘振林
 */
public class FileUtils2 {
    private static final String TAG = "FileUtils2";

    private FileUtils2() {
    }

    @NonNull
    public static String formatFileSize(double size) {
        // 如果字节数少于1024，则直接以B为单位，否则先除于1024
        if (size < 1024) {
            return Utils.roundDecimalUpTo2FractionDigitsString(size) + "B";
        } else {
            size = size / 1024d;
        }
        // 如果原字节数除于1024之后，少于1024，则可以直接以KB作为单位
        // 往后以此类推...
        if (size < 1024) {
            return Utils.roundDecimalUpTo2FractionDigitsString(size) + "KB";
        } else {
            size = size / 1024d;
        }
        if (size < 1024) {
            return Utils.roundDecimalUpTo2FractionDigitsString(size) + "MB";
        } else {
            size = size / 1024d;
            return Utils.roundDecimalUpTo2FractionDigitsString(size) + "GB";
        }
    }

    /** 判断SD卡是否已挂载 */
    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /** 判断SD卡上是否有足够的空间 */
    public static boolean hasEnoughStorageOnDisk(long size) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final String storage = Environment.getExternalStorageDirectory().getPath();
            final StatFs fs = new StatFs(storage);
            final long available = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            return available >= size;
        }
        return true;
    }

    public static boolean copyFile(@NonNull File srcFile, @NonNull File desFile) {
        if (isExternalStorageMounted()) {
            if (!hasEnoughStorageOnDisk(srcFile.length())) {
                Log.e(TAG, "内存卡空间不足",
                        new IllegalStateException("sdcard does not have enough storage"));
                return false;
            }
        } else {
            Log.e(TAG, "请先插入内存卡", new IllegalStateException("not found sdcard"));
            return false;
        }

        if (desFile.exists()) {
            if (ObjectsCompat.equals(getFileMD5(srcFile), getFileMD5(desFile))) {
                return true;
            } else {
                //noinspection ResultOfMethodCallIgnored
                desFile.delete();
            }
        }

        try (InputStream in = new FileInputStream(srcFile);
             OutputStream out = new FileOutputStream(desFile)) {
            final byte[] bytes = new byte[8 * 1024];
            int i;
            while ((i = in.read(bytes)) != -1) {
                out.write(bytes, 0, i);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int splitFile(@NonNull String directory,
                                @NonNull String fileName, @NonNull String extension,
                                long filePartLengthLimit) {
        final File file = new File(directory, fileName + extension);
        final long length = file.length();
        final int splitCount = (int) Math.ceil((double) length / filePartLengthLimit);

        File filePart;
        long filePartLength;
        long remainingBytesToWrite;
        int bytesToRead;
        int readBytes;
        final byte[] buffer = new byte[8 * 1024];

        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            for (int i = 1; i <= splitCount; i++) {
                filePart = new File(directory, fileName + i + extension);
                if (filePart.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    filePart.delete();
                }
                //noinspection ResultOfMethodCallIgnored
                filePart.createNewFile();

                try (OutputStream out = new FileOutputStream(filePart)) {
                    while (true) {
                        filePartLength = filePart.length();
                        remainingBytesToWrite = filePartLengthLimit - filePartLength;
                        bytesToRead = remainingBytesToWrite > buffer.length
                                ? buffer.length
                                : (int) remainingBytesToWrite;
                        if (bytesToRead > 0 && (readBytes = in.read(buffer, 0, bytesToRead)) != -1) {
                            out.write(buffer, 0, readBytes);
                        } else {
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        return splitCount;
    }

    public static boolean mergeFiles(@NonNull File[] files, @NonNull File dstFile, boolean deleteInputs) {
        if (dstFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dstFile.delete();
        }
        try {
            //noinspection ResultOfMethodCallIgnored
            dstFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dstFile))) {
            final byte[] buffer = new byte[8 * 1024];
            for (File file : files) {
                try (InputStream in = new FileInputStream(file)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (deleteInputs) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }

        return true;
    }

    // 计算文件的 MD5 值
    @Nullable
    public static String getFileMD5(@Nullable File file) {
        return getFileDigest(file, "MD5");
    }

    // 计算文件的 SHA-1 值
    @Nullable
    public static String getFileSha1(@Nullable File file) {
        return getFileDigest(file, "SHA-1");
    }

    // 计算文件的 SHA-256 值
    @Nullable
    public static String getFileSha256(@Nullable File file) {
        return getFileDigest(file, "SHA-256");
    }

    // 计算文件的 SHA-384 值
    @Nullable
    public static String getFileSha384(@Nullable File file) {
        return getFileDigest(file, "SHA-384");
    }

    // 计算文件的 SHA-512 值
    @Nullable
    public static String getFileSha512(@Nullable File file) {
        return getFileDigest(file, "SHA-512");
    }

    private static String getFileDigest(File file, String algorithm) {
        if (file == null || !file.isFile()) {
            return null;
        }

        try (InputStream in = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance(algorithm);

            int len;
            final byte[] buffer = new byte[8 * 1024];
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }

            return new BigInteger(1, digest.digest()).toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
