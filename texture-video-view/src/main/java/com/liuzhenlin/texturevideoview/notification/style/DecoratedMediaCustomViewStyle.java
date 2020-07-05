/*
 * Created on 2019/12/10 9:13 AM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.notification.style;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationBuilderWithBuilderAccessor;
import androidx.core.app.NotificationCompat;

import com.liuzhenlin.texturevideoview.R;

/**
 * Notification style for media custom views that are decorated by the system.
 *
 * <p>Instead of providing a media notification that is completely custom, a developer can set
 * this style and still obtain system decorations like the notification header with the expand
 * affordance.
 *
 * <p>
 * <strong>NOTE:</strong> Unlike the standard version
 * {@link androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle},
 * on platform versions prior to LOLLIPOP, this style does not support adding a cancel button for
 * the notification or any action buttons to the decor view created in this class. This decor is
 * just light-wight enough for you to inset some layout paddings around your media custom view
 * and to adjust its background to make it shown properly on each platform version.
 *
 * <p>
 * <strong>Also NOTE:</strong> The {@link androidx.media.app.NotificationCompat.MediaStyle} has been
 * being treated differently from the standard or any other template in some system UIs like MIUI.
 * For more detailed info, see
 * <a href="https://dev.mi.com/console/doc/detail?pId=1300">MIUI 10 媒体通知适配</a>
 *
 * <p>Use {@link NotificationCompat.Builder#setCustomContentView(RemoteViews)},
 * {@link NotificationCompat.Builder#setCustomBigContentView(RemoteViews)} and
 * {@link NotificationCompat.Builder#setCustomHeadsUpContentView(RemoteViews)}
 * to set the corresponding custom views to display.
 *
 * <p>To use this style with your Notification, feed it to
 * {@link NotificationCompat.Builder#setStyle(NotificationCompat.Style)} like so:
 * <pre class="prettyprint">
 * Notification n = new NotificationCompat.Builder()
 *     .setSmallIcon(R.drawable.ic_stat_player)
 *     .setCustomContentView(contentView)
 *     .setStyle(<b>new DecoratedMediaCustomViewStyle()</b>.setMediaSession(mSession))
 *     //...
 *     .build();
 * </pre>
 *
 * <p>If you are using this style, consider using the corresponding styles like
 * {@link androidx.media.R.style#TextAppearance_Compat_Notification_Media} or
 * {@link androidx.media.R.style#TextAppearance_Compat_Notification_Title_Media} in
 * your custom views in order to get the correct styling on each platform version.
 *
 * @author 刘振林
 * @see NotificationCompat.DecoratedCustomViewStyle
 * @see androidx.media.app.NotificationCompat.MediaStyle
 */
public class DecoratedMediaCustomViewStyle extends NotificationCompat.Style {

    /*package*/ MediaSessionCompat.Token mToken;

    public DecoratedMediaCustomViewStyle() {
    }

    public DecoratedMediaCustomViewStyle(NotificationCompat.Builder builder) {
        setBuilder(builder);
    }

    /**
     * Attaches a {@link MediaSessionCompat.Token} to this Notification
     * to provide additional playback information and control to the System UI.
     */
    public void setMediaSession(MediaSessionCompat.Token token) {
        mToken = token;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public void apply(NotificationBuilderWithBuilderAccessor builderAccessor) {
        if (Build.VERSION.SDK_INT >= 24) {
            builderAccessor.getBuilder().setStyle(
                    fillInMediaStyle(new Notification.DecoratedMediaCustomViewStyle()));
        } else if (Build.VERSION.SDK_INT >= 21) {
            builderAccessor.getBuilder().setStyle(
                    fillInMediaStyle(new Notification.MediaStyle()));
        }/* else {
            builderAccessor.getBuilder().setOngoing(true);
        }*/
    }

    @RequiresApi(21)
    /*package*/ Notification.MediaStyle fillInMediaStyle(Notification.MediaStyle style) {
        if (mToken != null) {
            style.setMediaSession((MediaSession.Token) mToken.getToken());
        }
        return style;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public RemoteViews makeContentView(NotificationBuilderWithBuilderAccessor builder) {
        if (Build.VERSION.SDK_INT >= 24) {
            // No custom content view required
            return null;
        }

        final boolean hasContentView = mBuilder.getContentView() != null;
        if (Build.VERSION.SDK_INT >= 21) {
            // If we are on L/M the media notification will only be colored if the expanded version
            // is of media style, so we have to create a custom view for the collapsed version
            // as well in that case.
            final boolean createCustomContent =
                    hasContentView || mBuilder.getBigContentView() != null;
            if (createCustomContent) {
                RemoteViews contentView = generateContentView();
                if (hasContentView) {
                    buildIntoRemoteViews(contentView, mBuilder.getContentView());
                }
                setBackgroundColor(contentView);
                return contentView;
            }
        } else {
            RemoteViews contentView = generateContentView();
            if (hasContentView) {
                buildIntoRemoteViews(contentView, mBuilder.getContentView());
                return contentView;
            }
        }
        return null;
    }

    /*package*/ RemoteViews generateContentView() {
        return applyStandardTemplate(false /* showSmallIcon */,
                getContentViewLayoutResource(), false /* fitIn1U */);
    }

    /*package*/ int getContentViewLayoutResource() {
        return R.layout.notification_template_media_simple_custom;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public RemoteViews makeBigContentView(NotificationBuilderWithBuilderAccessor builder) {
        if (Build.VERSION.SDK_INT >= 24) {
            // No custom big content view required
            return null;
        }

        RemoteViews innerView = mBuilder.getBigContentView() != null
                ? mBuilder.getBigContentView()
                : mBuilder.getContentView();
        if (innerView == null) {
            // No expandable notification
            return null;
        }

        RemoteViews bigContentView = generateBigContentView();
        buildIntoRemoteViews(bigContentView, innerView);
        if (Build.VERSION.SDK_INT >= 21) {
            setBackgroundColor(bigContentView);
        }
        return bigContentView;
    }

    /*package*/ RemoteViews generateBigContentView() {
        return applyStandardTemplate(false /* showSmallIcon */,
                getBigContentViewLayoutResource(), false /* fitIn1U */);
    }

    /*package*/ int getBigContentViewLayoutResource() {
        return R.layout.notification_template_media_simple_custom;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Override
    public RemoteViews makeHeadsUpContentView(NotificationBuilderWithBuilderAccessor builder) {
        if (Build.VERSION.SDK_INT >= 24) {
            // No custom heads up content view required
            return null;
        }

        RemoteViews innerView = mBuilder.getHeadsUpContentView() != null
                ? mBuilder.getHeadsUpContentView()
                : mBuilder.getContentView();
        if (innerView == null) {
            // No expandable notification
            return null;
        }

        RemoteViews headsUpContentView = generateBigContentView();
        buildIntoRemoteViews(headsUpContentView, innerView);
        if (Build.VERSION.SDK_INT >= 21) {
            setBackgroundColor(headsUpContentView);
        }
        return headsUpContentView;
    }

    private void setBackgroundColor(RemoteViews views) {
        @SuppressLint("PrivateResource") //@formatter:off
        final int color = mBuilder.getColor() != NotificationCompat.COLOR_DEFAULT
                ? mBuilder.getColor()
                : mBuilder.mContext.getResources().getColor(
                        R.color.notification_material_background_media_default_color); //@formatter:on
        views.setInt(R.id.notification_main_column_container, "setBackgroundColor", color);
    }
}
