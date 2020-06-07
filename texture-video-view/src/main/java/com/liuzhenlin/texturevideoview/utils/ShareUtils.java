/*
 * Created on 2020-3-14 5:35:20 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.liuzhenlin.texturevideoview.R;

import java.io.File;

/**
 * @author 刘振林
 */
public class ShareUtils {
    private ShareUtils() {
    }

    public static void shareText(@NonNull Context context,
                                 @NonNull String text, @NonNull String mimeType /* MIME type of the text */) {
        Intent it = new Intent(Intent.ACTION_SEND);
        it.putExtra(Intent.EXTRA_TEXT, text);
        it.setType(mimeType);
        context.startActivity(Intent.createChooser(it, context.getString(R.string.share)));
    }

    public static void shareFile(@NonNull Context context, @NonNull String authority,
                                 @NonNull File file, @NonNull String defMimeType /* default file MIME type */) {
        Intent it = new Intent().setAction(Intent.ACTION_SEND);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            uri = FileProvider.getUriForFile(context, authority, file);
        } else {
            uri = Uri.fromFile(file);
        }
        it.putExtra(Intent.EXTRA_STREAM, uri);
        it.setType(FileUtils.getMimeTypeFromPath(file.getPath(), defMimeType));
        context.startActivity(Intent.createChooser(it, context.getString(R.string.share)));
    }
}
