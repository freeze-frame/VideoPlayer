/*
 * Created on 2019/5/16 7:19 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

/**
 * @author 刘振林
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    private FileUtils() {
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

    /**
     * 判断SD卡是否已挂载
     */
    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 判断SD卡上是否有足够的空间
     */
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
            IOUtils.copy(in, out);
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

                OutputStream out = null;
                try {
                    out = new FileOutputStream(filePart);
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
                } finally {
                    IOUtils.closeSilently(out);
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
                InputStream in = null;
                try {
                    in = new FileInputStream(file);
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                } finally {
                    IOUtils.closeSilently(in);
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
    public static String getFileMD5(@NonNull File file) {
        return getFileDigest(file, "MD5");
    }

    // 计算文件的 SHA-1 值
    @Nullable
    public static String getFileSha1(@NonNull File file) {
        return getFileDigest(file, "SHA-1");
    }

    @Nullable
    public static String getFileDigest(@NonNull File file, @NonNull String algorithm) {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            return IOUtils.getDigest(in, algorithm);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeSilently(in);
        }
        return null;
    }

    @Nullable
    public static File saveBitmapToDisk(@NonNull Context context,
                                        @NonNull Bitmap bitmap, @NonNull Bitmap.CompressFormat format,
                                        @IntRange(from = 0, to = 100) int quality,
                                        @NonNull String directory, @NonNull String fileName) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            File dirFile = new File(directory);
            if (!dirFile.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dirFile.mkdirs();
            }

            File file = new File(dirFile, fileName);

            boolean successful = false;
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                if (bitmap.compress(format, quality, out)) {
                    out.flush();
                    successful = true;

                    final String mimeType;
                    switch (format) {
                        case JPEG:
                            mimeType = "image/jpeg";
                            break;
                        case PNG:
                            mimeType = "image/png";
                            break;
                        case WEBP:
                            mimeType = "image/webp";
                            break;
                        default:
                            mimeType = null;
                    }
                    recordMediaFileToDatabaseAndScan(context,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, file, mimeType);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (successful) {
                return file;
            } else {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
        return null;
    }

    public static void recordMediaFileToDatabaseAndScan(@NonNull Context context, @NonNull Uri mediaUri,
                                                        @NonNull File file, @Nullable String mimeType) {
        context = context.getApplicationContext();
        final String fileName = file.getName();
        final String filePath = file.getAbsolutePath();
        if (mimeType == null) {
            mimeType = getMimeTypeFromPath(filePath, null);
            if (mimeType == null) {
                throw new NullPointerException("Failed to infer mimeType from the file extension");
            }
        }

        ContentValues values = new ContentValues(6);
        values.put(MediaStore.MediaColumns.TITLE, getFileTitleFromFileName(fileName));
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.DATA, filePath);
        values.put(MediaStore.MediaColumns.SIZE, file.length());
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.DATE_ADDED,
                Utils.roundDouble(System.currentTimeMillis() / 1000.0));
        context.getContentResolver().insert(mediaUri, values);

        MediaScannerConnection.scanFile(context, new String[]{filePath}, new String[]{mimeType}, null);
    }

    @NonNull
    public static String getFileNameFromFilePath(@NonNull String filePath) {
        return filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1);
    }

    @NonNull
    public static String getFileTitleFromFileName(@NonNull String fileName) {
        final int lastIndex = fileName.lastIndexOf(".");
        return lastIndex == -1 ? fileName : fileName.substring(0, lastIndex);
    }

    @Nullable
    public static String getMimeTypeFromPath(@NonNull String filePath, @Nullable String defMineType) {
        final int index = filePath.lastIndexOf(".");
        if (index != -1) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(filePath.substring(index + 1));
        }
        return defMineType;
    }

    @NonNull
    public static File getAppCacheDir(@NonNull Context context) {
        File dir;
        dir = context.getExternalCacheDir();
        if (dir == null) {
            dir = context.getCacheDir();
        }
        return dir;
    }

    public static class UriResolver {

        private UriResolver() {
        }

        @Nullable
        public static String getPath(@NonNull Context context, @NonNull Uri uri) {
            // DocumentProvider
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
                    && DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + File.separator + split[1];
                    }

                    // DownloadsProvider
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
                    return getDataColumn(context, contentUri, null, null);

                    // MediaProvider
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = {split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }

                // MediaStore (and general)
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                if (isGooglePhotosUri(uri)) {
                    return uri.getLastPathSegment();
                }
                return getDataColumn(context, uri, null, null);

                // File
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }

            // Uri.parse(String)
            if ("android.net.Uri$StringUri".equals(uri.getClass().getName())) {
                return uri.toString();
            }

            return null;
        }

        /**
         * Get the value of the data column for this Uri. This is useful for
         * MediaStore Uris, and other file-based ContentProviders.
         *
         * @param context       The context.
         * @param uri           The Uri to query.
         * @param selection     (Optional) Filter used in the query.
         * @param selectionArgs (Optional) Selection arguments used in the query.
         * @return The value of the '_data' column, which is typically a file path.
         */
        private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
            final String column = "_data";
            final String[] projection = {column};
            Cursor cursor = context.getContentResolver().query(
                    uri, projection, selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor.getColumnIndexOrThrow(column));
                    }
                } finally {
                    cursor.close();
                }
            }
            return null;
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        public static boolean isExternalStorageDocument(@NonNull Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        public static boolean isDownloadsDocument(@NonNull Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        public static boolean isMediaDocument(@NonNull Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        public static boolean isGooglePhotosUri(@NonNull Uri uri) {
            return "com.google.android.apps.photos.content".equals(uri.getAuthority());
        }
    }
}
