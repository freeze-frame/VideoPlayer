/*
 * Created on 2018/9/16 4:09 PM.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.text.ParcelableSpan;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.customview.widget.ViewDragHelper;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.util.Synthetic;
import com.google.android.exoplayer2.source.MediaSourceFactory;
import com.google.android.exoplayer2.text.CaptionStyleCompat;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.util.Util;
import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.texturevideoview.drawable.CircularProgressDrawable;
import com.liuzhenlin.texturevideoview.service.BackgroundPlaybackControllerService;
import com.liuzhenlin.texturevideoview.utils.BitmapUtils;
import com.liuzhenlin.texturevideoview.utils.FileUtils;
import com.liuzhenlin.texturevideoview.utils.ParallelThreadExecutor;
import com.liuzhenlin.texturevideoview.utils.ScreenUtils;
import com.liuzhenlin.texturevideoview.utils.TimeUtil;
import com.liuzhenlin.texturevideoview.utils.TransitionListenerAdapter;
import com.liuzhenlin.texturevideoview.utils.URLUtils;
import com.liuzhenlin.texturevideoview.utils.Utils;
import com.liuzhenlin.texturevideoview.utils.VideoUtils;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A View used to display video content onto {@link TextureView}, which takes care of computing
 * the measurements of the child widgets from the video and synchronizing them with the state of
 * the {@link VideoPlayer}.
 *
 * <p>This class requires the permission(s):
 * <ul>
 *   <li>{@link android.Manifest.permission#READ_EXTERNAL_STORAGE} for a local audio/video file</li>
 *   <li>{@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE} for saving captured video photos
 *       or cutout short-videos/GIFs into disk</li>
 *   <li>{@link android.Manifest.permission#INTERNET} to network based streaming content</li>
 * </ul>
 *
 * <p>This is similar to {@link android.widget.VideoView}, but it comes with a custom control
 * containing buttons like "Play/Pause", "Skip Next", "Choose Episode", progress sliders for
 * adjusting screen brightness, volume and video progress, etc.
 *
 * <p>By default, when this view is in fullscreen mode, all of the "Skip Next" and "Choose Episode"
 * buttons are invisible to the user and even not kept in the view hierarchy for any layout purpose
 * regardless of whether or not a {@link PlayListAdapter} is set for the view displaying the playlist
 * as the data-associated logic code related to the videos should normally be maintained in some
 * specific class of you; but, if reasonable and necessary, for the former button, you can set
 * `canSkipToNext` to `true` through the code like {@code mVideoView.setCanSkipToNext(true)}, and
 * to the latter one, simply pass `true` into one of the {@link #setCanSkipToPrevious(boolean)} and
 * {@link #setCanSkipToNext(boolean)} methods to break the limit so that after clicked the button,
 * the user can have a look at the playlist and choose a preferred video from it to play.
 *
 * <P>An {@link OpCallback} usually is required for this class, which allows us to adjust the
 * brightness of the window this view is attached to, or this feature will not be enabled at all.
 *
 * <p>{@link IVideoPlayer.OnPlaybackStateChangeListener} can be used to monitor the state of
 * the player or the current video playback.
 * {@link IVideoPlayer.VideoListener} offers default/no-op implementations of each callback method,
 * through which we're able to get notified by all the events related to video playbacks we publish.<br>
 * <strong>NOTE:</strong> If possible, do avoid invoking one/some of the methods in
 * {@link IVideoPlayer} that may cause the current playback state to change at the call site
 * of some method of the listeners above, in case unexpected result occurs though we have performed
 * some state checks before and after some of the call sites to those methods.
 *
 * <p>Using a TextureVideoView is simple enough.
 * The library sample ({@link com.liuzhenlin.texturevideoview.sample.DemoActivity}) demonstrates
 * how to play a video with this class.
 *
 * @author <a href="mailto:2233788867@qq.com">刘振林</a>
 */
public class TextureVideoView extends AbsTextureVideoView implements ViewHostEventCallback {

    /** Monitors all events related to (some of the widgets of) this view. */
    public interface EventListener {

        /** Called when the {@link VideoPlayer} for this view changes. */
        void onPlayerChange(@Nullable VideoPlayer videoPlayer);

        /**
         * Called when the Activity this view is attached to should be destroyed
         * or Dialog that should be dismissed, etc.
         */
        void onReturnClicked();

        /** Called when the background playback controller shown in the notification bar disappears. */
        void onBackgroundPlaybackControllerClose();

        /**
         * Called when the mode of this view changes.
         *
         * @param oldMode       the old view mode, one of the constants defined with the `VIEW_MODE_` prefix
         * @param newMode       the new view mode, one of the constants defined with the `VIEW_MODE_` prefix
         * @param layoutMatches true if the layout has been adjusted to match the corresponding mode
         */
        void onViewModeChange(@ViewMode int oldMode, @ViewMode int newMode, boolean layoutMatches);

        /** Called when the video being played is about to be shared with another application. */
        void onShareVideo();

        /**
         * Called when a photo is captured for the user to share it to another app.
         *
         * @param photo the captured image file of the current playing video
         */
        void onShareCapturedVideoPhoto(@NonNull File photo);
    }

    public interface OpCallback {
        /**
         * @return the Window this view is currently attached to
         */
        @NonNull
        Window getWindow();

        /**
         * Gets the class of the host Activity for this view to launch it when the background playback
         * controller as posted by the foreground service {@link BackgroundPlaybackControllerService}
         * is clicked from the notification list. The default implementation simply returns {@code null}
         * with that behaviour disabled for the user.
         * <p>
         * <strong>NOTE:</strong> when this method returns the class of the Activity holding this view,
         * you were supposed to provide the <code>singleTask</code> value for
         * the {@link android.R.attr#launchMode launchMode} attribute in the manifest describing
         * that Activity so as to launch the existing Activity instance instead of a new one
         * from the foreground service's notification.
         */
        @Nullable
        default Class<? extends Activity> getHostActivityClass() {
            return null;
        }

        /**
         * Gets the base external storage directory for your app to store the captured video photos
         * or cutout short-videos/GIFs.
         * <p>
         * If the returned value is nonnull, the final storage directory will be the directory
         * with `/screenshots`, '/clips/ShortVideos' or '/clips/GIFs' appended,
         * or the primary external storage directory concatenating with your application name
         * will be created (if it does not exist) as the basis.
         */
        @Nullable
        default String getAppExternalFilesDir() {
            return null;
        }
    }

    public static abstract class PlayListAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
        TextureVideoView videoView;
        ViewGroup drawerView;
        RecyclerView playlist;

        final OnClickListener onClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClick(v, playlist.getChildAdapterPosition(v));
                videoView.closeDrawer(drawerView);
            }
        };
        final OnLongClickListener onLongClickListener = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return onItemLongClick(v, playlist.getChildAdapterPosition(v));
            }
        };

        @CallSuper
        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            playlist = recyclerView;
            drawerView = (ViewGroup) recyclerView.getParent();
            videoView = (TextureVideoView) drawerView.getParent();
        }

        @CallSuper
        @Override
        public void onViewAttachedToWindow(@NonNull VH holder) {
            holder.itemView.setOnClickListener(onClickListener);
            holder.itemView.setOnLongClickListener(onLongClickListener);
        }

        @CallSuper
        @Override
        public void onViewDetachedFromWindow(@NonNull VH holder) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setOnLongClickListener(null);
        }

        /**
         * Callback method to be invoked when an item in the RecyclerView has been clicked.
         *
         * @param view     The itemView that was clicked.
         * @param position The position of the view in the adapter.
         */
        public void onItemClick(@NonNull View view, int position) {
        }

        /**
         * Callback method to be invoked when an item in the RecyclerView has been clicked and held.
         *
         * @param view     The itemView that was clicked and held.
         * @param position The position of the view in the list.
         * @return true if the callback consumed the long click, false otherwise.
         */
        public boolean onItemLongClick(@NonNull View view, int position) {
            return false;
        }
    }

    private static final String TAG = "TextureVideoView";

    @Synthetic int mPrivateFlags = PFLAG_CONTROLS_SHOWING;

    /** If the controls are showing, this is marked into {@link #mPrivateFlags}. */
    private static final int PFLAG_CONTROLS_SHOWING = 1;

    /** Once set, any method call to {@link #showControls(boolean, boolean)} will do thing. */
    private static final int PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS = 1 << 1;

    /** Set by {@link #setLocked(boolean, boolean)} */
    private static final int PFLAG_LOCKED = 1 << 2;

    /** Set by {@link #setClipViewBounds(boolean)} */
    private static final int PFLAG_CLIP_VIEW_BOUNDS = 1 << 3;

    /** Set by {@link #setFullscreenMode(boolean, int)} */
    private static final int PFLAG_IN_FULLSCREEN_MODE = 1 << 4;

    /** Set by {@link #setVideoStretchedToFitFullscreenLayout(boolean)} */
    private static final int PFLAG_VIDEO_STRETCHED_TO_FIT_FULLSCREEN_LAYOUT = 1 << 5;

    /**
     * When set, we will turn off the video playback and release the player object and
     * some other resources associated with it when the currently playing video ends.
     */
    private static final int PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS = 1 << 6;

    /** Set via {@link #setCanSkipToPrevious(boolean)} */
    private static final int PFLAG_CAN_SKIP_TO_PREVIOUS = 1 << 7;

    /** Set via {@link #setCanSkipToNext(boolean)} */
    private static final int PFLAG_CAN_SKIP_TO_NEXT = 1 << 8;

    /** The last aggregated visibility. Used to detect when it truly changes. */
    private static final int PFLAG_AGGREGATED_VISIBLE = 1 << 9;

    /**
     * Flag indicating that the playback position is not expected to be moved to
     * where the video seek bar is dragged as we stop tracking touch on it.
     */
    private static final int PFLAG_DISALLOW_PLAYBACK_POSITION_SEEK_ON_STOP_TRACKING_TOUCH = 1 << 10;

    @ViewMode
    private int mViewMode = VIEW_MODE_DEFAULT;

    @IntDef({
            VIEW_MODE_DEFAULT,
            VIEW_MODE_MINIMUM,
            VIEW_MODE_FULLSCREEN,
            VIEW_MODE_LOCKED_FULLSCREEN,
            VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN,
            VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface ViewMode {
    }

    /** Default mode for this view (unlocked, non-fullscreen and non-minimized). */
    public static final int VIEW_MODE_DEFAULT = 1;

    /** This view is minimized now, typically in picture-in-picture mode. */
    public static final int VIEW_MODE_MINIMUM = 2;

    /** This view is currently in fullscreen mode. */
    public static final int VIEW_MODE_FULLSCREEN = 3;

    /** This view is currently in fullscreen and locked mode. */
    public static final int VIEW_MODE_LOCKED_FULLSCREEN = 4;

    /**
     * This view is currently in fullscreen mode and the video is stretched to fit
     * the fullscreen layout.
     */
    public static final int VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN = 5;

    /**
     * This view is currently in fullscreen and locked mode and the video is stretched to fit
     * the fullscreen layout.
     */
    public static final int VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN = 6;

    /** The amount of time till we fade out the controls. */
    private static final int TIMEOUT_SHOW_CONTROLS = 5000; // ms
    /** The amount of time till we fade out the brightness or volume frame. */
    private static final int TIMEOUT_SHOW_BRIGHTNESS_OR_VOLUME = 1000; // ms
    /** The amount of time till we fade out the view displaying the captured photo of the video. */
    private static final int TIMEOUT_SHOW_CAPTURED_PHOTO = 3000; // ms

    @Nullable
    @Synthetic VideoPlayer mVideoPlayer;

    /** The listener for all the events related to this view we publish. */
    @Nullable
    @Synthetic EventListener mEventListener;

    @Nullable
    @Synthetic OpCallback mOpCallback;

    @Synthetic final ConstraintLayout mContentView;
    @Synthetic final ViewGroup mDrawerView;

    @Synthetic final RecyclerView mPlayList;
    @Synthetic View mMoreView;
    @Synthetic TrackSelectionView mTrackSelectionView;

    /** Shows the video playback. */
    @Synthetic final TextureView mTextureView;
    /** Caches the Surface created for this view. */
    @Synthetic Surface mSurface;
    /** Surface used by the {@link VideoPlayer} set for this view. */
    @Synthetic Surface mUsedSurface; // null or mSurface

    private final SubtitleView mSubtitleView;
    private static final CaptionStyleCompat sDefaultCaptionStyle =
            new CaptionStyleCompat(
                    /* foregroundColor= */ Color.WHITE,
                    /* backgroundColor= */ Color.TRANSPARENT,
                    /* windowColor= */ Color.TRANSPARENT,
                    /* edgeType= */ CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                    /* edgeColor= */ Color.BLACK,
                    /* typeface= */ null);

    @Synthetic final ViewGroup mTopControlsFrame;
    @Synthetic final TextView mTitleText;
    @Synthetic final View mShareButton;
    @Synthetic final View mTrackButton;
    @Synthetic final View mMoreButton;

    @Synthetic final ImageView mLockUnlockButton;
    @Synthetic final View mCameraButton;
    @Synthetic final View mVideoCameraButton;

    @Synthetic final ViewGroup mBrightnessOrVolumeFrame;
    private final TextView mBrightnessOrVolumeText;
    @Synthetic final ProgressBar mBrightnessOrVolumeProgress;

    private final ViewGroup mBottomControlsFrame;
    @Synthetic ImageView mToggleButton;
    @Synthetic SeekBar mVideoSeekBar;
    // BEGIN fields: Bottom controls only in non-fullscreen mode
    private TextView mVideoProgressText;
    private TextView mVideoDurationText;
    @Synthetic View mMinimizeButton;
    @Synthetic View mFullscreenButton;
    // END fields: Bottom controls only in non-fullscreen mode
    // BEGIN fields: Bottom controls only in fullscreen mode
    @Synthetic View mSkipNextButton;
    private TextView mVideoProgressDurationText;
    @Synthetic AppCompatSpinner mSpeedSpinner;
    @Synthetic View mChooseEpisodeButton;
    // END fields: Bottom controls only in fullscreen mode

    /**
     * Scrim view with a 33.3% black background shows on our TextureView to obscure primary
     * video frames while the video thumb text is visible to the user.
     */
    @Synthetic final View mScrimView;
    @Synthetic final TextView mSeekingVideoThumbText;

    @Synthetic final ViewGroup mSeekingTextProgressFrame;
    @Synthetic final TextView mSeekingProgressDurationText;
    @Synthetic final ProgressBar mSeekingProgress;

    private final ImageView mLoadingImage;
    private final CircularProgressDrawable mLoadingDrawable;

    @Synthetic View mCapturedPhotoView;
    @Synthetic Bitmap mCapturedBitmap;
    @Synthetic File mSavedPhoto;
    @Synthetic AsyncTask<Void, Void, File> mSaveCapturedPhotoTask;

    private View mClipView;
    @Synthetic AsyncTask<Void, Bitmap, Void> mLoadClipThumbsTask;

    private ListPopupWindow mSpinnerListPopup;
    @Synthetic PopupWindow mSpinnerPopup;

    /** The minimum height of the drawer views (the playlist and the 'more' view) */
    @Synthetic int mDrawerViewMinimumHeight;

    /** Caches the initial `paddingTop` of the top controls frame */
    @Synthetic final int mNavInitialPaddingTop;

    /** Title of the video */
    @Synthetic String mTitle;

    private final String mStringPlay;
    private final String mStringPause;
    private final String mStringLock;
    @Synthetic final String mStringUnlock;
    private final String mStringBrightnessFollowsSystem;
    private final String[] mSpeedsStringArray;
    @Synthetic final float mSeekingViewHorizontalOffset;
    @Synthetic final float mSeekingVideoThumbCornerRadius;

    /**
     * Bright complement to the primary branding color. By default, this is the color applied to
     * framework controls (via colorControlActivated).
     */
    protected final int mColorAccent;

    /**
     * Distance in pixels a touch can wander before we think the user is scrolling.
     */
    protected final int mTouchSlop;

    /**
     * Time interpolator used for the animator of stretching or shrinking the texture view that
     * displays the video content.
     */
    private static final Interpolator sStretchShrinkVideoInterpolator = new OvershootInterpolator();

    private final OnChildTouchListener mOnChildTouchListener = new OnChildTouchListener();
    @Synthetic final OnChildClickListener mOnChildClickListener = new OnChildClickListener();
    @Synthetic final OnVideoSeekBarChangeListener mOnVideoSeekBarChangeListener =
            new OnVideoSeekBarChangeListener();

    @Synthetic final MsgHandler mMsgHandler = new MsgHandler(this);

    /**
     * Runnable used to turn off the video playback when a scheduled time point is arrived.
     */
    @Synthetic TimedOffRunnable mTimedOffRunnable;

    /** Indicating that the brightness value of a window should follow the system's */
    public static final int BRIGHTNESS_FOLLOWS_SYSTEM = -1;
    /** Lowest value for the brightness of a window */
    public static final int MIN_BRIGHTNESS = 0;
    /** Highest value for the brightness of a window */
    public static final int MAX_BRIGHTNESS = 255;

    /**
     * The ratio of the progress of the volume seek bar to the current media stream volume,
     * used to improve the smoothness of the volume progress slider, esp. when the user changes
     * its progress through horizontal screen track touches.
     */
    private static final int RATIO_VOLUME_PROGRESS_TO_VOLUME = 20;

    /** Maximum volume of the system media audio stream ({@link AudioManager#STREAM_MUSIC}). */
    @Synthetic final int mMaxVolume;

    @Synthetic final AudioManager mAudioManager;

    private static BackgroundPlaybackControllerServiceConn sBgPlaybackControllerServiceConn;

    private ViewDragHelper mDragHelper;
    private static final int FLAG_IS_OPENING = 0x2; // DrawerLayout.LayoutParams#FLAG_IS_OPENING
    private static final int FLAG_IS_CLOSING = 0x4; // DrawerLayout.LayoutParams#FLAG_IS_CLOSING

    private static Field sLeftDraggerField;
    private static Field sRightDraggerField;
    @Synthetic static Field sDrawerOpenStateField;

    private static Field sListPopupField;
    private static Field sPopupField;
    private static Field sPopupDecorViewField;
    private static Field sForceIgnoreOutsideTouchField;
    @Synthetic static Field sPopupOnDismissListenerField;

    static {
        try {
            sListPopupField = AppCompatSpinner.class.getDeclaredField("mPopup");
            sListPopupField.setAccessible(true);

            Class<?> listPopupClass = ListPopupWindow.class;
            sPopupField = listPopupClass.getDeclaredField("mPopup");
            sPopupField.setAccessible(true);
/*
            try {
                // On platforms after O MR1, we can not use reflections to access the internal hidden
                // fields and methods, so use the slower processing logic that set a touch listener
                // for the popup's decorView and consume the 'down' and 'outside' events according to
                // the same conditions to its original onTouchEvent() method through omitting
                // the invocations to the popup's dismiss() method, so that the popup remains showing
                // within an outside touch event stream till the up event is reached, in which, instead,
                // we will dismiss it manually.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    //noinspection JavaReflectionMemberAccess
                    sPopupDecorViewField = PopupWindow.class.getDeclaredField("mDecorView");
                    sPopupDecorViewField.setAccessible(true);
                } else {
                    // @see ListPopupWindow#setForceIgnoreOutsideTouch() — public hidden method
                    //                                                   — restricted to internal use only
                    // @see ListPopupWindow#show()
                    sForceIgnoreOutsideTouchField = listPopupClass.getDeclaredField("mForceIgnoreOutsideTouch");
                    sForceIgnoreOutsideTouchField.setAccessible(true);
                }
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
*/
            try {
                //noinspection JavaReflectionMemberAccess
                sPopupOnDismissListenerField = PopupWindow.class.getDeclaredField("mOnDismissListener");
                sPopupOnDismissListenerField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            sListPopupField = sPopupField = null;
        }

        try {
            Class<DrawerLayout> drawerLayoutClass = DrawerLayout.class;
            sLeftDraggerField = drawerLayoutClass.getDeclaredField("mLeftDragger");
            sLeftDraggerField.setAccessible(true);
            sRightDraggerField = drawerLayoutClass.getDeclaredField("mRightDragger");
            sRightDraggerField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            sLeftDraggerField = sRightDraggerField = null;
        }
        try {
            Class<LayoutParams> lpClass = LayoutParams.class;
            sDrawerOpenStateField = lpClass.getDeclaredField("openState");
            sDrawerOpenStateField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public TextureVideoView(@NonNull Context context) {
        this(context, null);
    }

    public TextureVideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("ClickableViewAccessibility")
    public TextureVideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(Color.BLACK);
        setScrimColor(ContextCompat.getColor(context, R.color.videoScrimColor));

        mStringPlay = mResources.getString(R.string.play);
        mStringPause = mResources.getString(R.string.pause);
        mStringLock = mResources.getString(R.string.lock);
        mStringUnlock = mResources.getString(R.string.unlock);
        mStringBrightnessFollowsSystem = mResources.getString(R.string.brightnessFollowsSystem);
        mSpeedsStringArray = mResources.getStringArray(R.array.speeds);
        mSeekingViewHorizontalOffset = mResources.getDimension(R.dimen.seekingViewHorizontalOffset);
        mSeekingVideoThumbCornerRadius = mResources.getDimension(R.dimen.seekingVideoThumbCornerRadius);

        mColorAccent = ContextCompat.getColor(context, R.color.colorAccent);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // Inflate the content
        View.inflate(context, R.layout.view_video, this);
        mContentView = findViewById(R.id.content_videoview);
        mDrawerView = findViewById(R.id.drawer_videoview);
        mPlayList = findViewById(R.id.rv_playlist);
        mTextureView = findViewById(R.id.textureView);
        mSubtitleView = findViewById(R.id.subtitleView);
        mScrimView = findViewById(R.id.scrim);
        mSeekingVideoThumbText = findViewById(R.id.text_seekingVideoThumb);
        mSeekingTextProgressFrame = findViewById(R.id.frame_seekingTextProgress);
        mSeekingProgressDurationText = findViewById(R.id.text_seeking_progress_duration);
        mSeekingProgress = findViewById(R.id.pb_seekingProgress);
        mLoadingImage = findViewById(R.id.image_loading);
        mTopControlsFrame = findViewById(R.id.frame_topControls);
        mTitleText = findViewById(R.id.text_title);
        mShareButton = findViewById(R.id.btn_share);
        mTrackButton = findViewById(R.id.btn_track);
        mMoreButton = findViewById(R.id.btn_more);
        mLockUnlockButton = findViewById(R.id.btn_lockUnlock);
        mCameraButton = findViewById(R.id.btn_camera);
        mVideoCameraButton = findViewById(R.id.btn_videoCamera);
        mBrightnessOrVolumeFrame = findViewById(R.id.frame_brightness_or_volume);
        mBrightnessOrVolumeText = findViewById(R.id.text_brightness_or_volume);
        mBrightnessOrVolumeProgress = findViewById(R.id.pb_brightness_or_volume);
        mBottomControlsFrame = findViewById(R.id.frame_bottomControls);
        inflateBottomControls();

        mNavInitialPaddingTop = mTopControlsFrame.getPaddingTop();

        setDrawerLockModeInternal(LOCK_MODE_LOCKED_CLOSED, mDrawerView);
        addDrawerListener(new SimpleDrawerListener() {
            int scrollState;
            float slideOffset;

            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                if (scrollState == STATE_SETTLING) {
                    // NOTE: showControls(boolean) will not work in the case that the drawer is visible.
                    if (slideOffset < 0.5f && slideOffset < this.slideOffset) {
                        if (!isControlsShowing()) {
                            mPrivateFlags &= ~PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                            showControls(true);
                        }
                    } else if (slideOffset > 0.5f && slideOffset > this.slideOffset) {
                        if (isControlsShowing()) {
                            showControls(false);
                            mPrivateFlags |= PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                        }
                    }
                }
                this.slideOffset = slideOffset;
            }

            @SuppressLint("SwitchIntDef")
            @Override
            public void onDrawerStateChanged(int newState) {
                switch (newState) {
                    case STATE_SETTLING:
                        if (sDrawerOpenStateField != null) {
                            try {
                                final int state = sDrawerOpenStateField.getInt(mDrawerView.getLayoutParams());
                                if ((state & FLAG_IS_OPENING) != 0) {
                                    showControls(false);
                                    mPrivateFlags |= PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                                } else if ((state & FLAG_IS_CLOSING) != 0) {
                                    mPrivateFlags &= ~PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                                    showControls(true);
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case STATE_IDLE:
                        if (slideOffset == 0) {
                            if (mPlayList.getVisibility() == View.VISIBLE) {
                                mPlayList.setVisibility(GONE);
                            }
                            if (mMoreView != null) {
                                mDrawerView.removeView(mMoreView);
                                mMoreView = null;
                            }
                            if (mTrackSelectionView != null) {
                                mDrawerView.removeView(mTrackSelectionView);
                                mTrackSelectionView = null;
                            }
                        }
                        break;
                }
                scrollState = newState;
            }
        });

        mContentView.setTouchInterceptor(mOnChildTouchListener);
        mContentView.setOnTouchListener(mOnChildTouchListener);
        mDrawerView.setOnTouchListener(mOnChildTouchListener);

        mTitleText.setOnClickListener(mOnChildClickListener);
        mShareButton.setOnClickListener(mOnChildClickListener);
        mTrackButton.setOnClickListener(mOnChildClickListener);
        mMoreButton.setOnClickListener(mOnChildClickListener);
        mLockUnlockButton.setOnClickListener(mOnChildClickListener);
        mCameraButton.setOnClickListener(mOnChildClickListener);
        mVideoCameraButton.setOnClickListener(mOnChildClickListener);

        // Prepare video playback
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                mUsedSurface = mSurface = new Surface(surface);
                if (mVideoPlayer != null) {
                    mVideoPlayer.onVideoSurfaceChanged(mUsedSurface);
                    mVideoPlayer.openVideo();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mVideoPlayer != null) {
                    mVideoPlayer.closeVideo();
                    if (mUsedSurface != null) {
                        mVideoPlayer.onVideoSurfaceChanged(null);
                    }
                }
                mSurface.release();
                mUsedSurface = mSurface = null;
                return true;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });

        mSubtitleView.setStyle(sDefaultCaptionStyle);

        mAudioManager = (AudioManager)
                context.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        Typeface tf = Typeface.createFromAsset(mResources.getAssets(), "fonts/avenirnext-medium.ttf");
        mSeekingVideoThumbText.setTypeface(tf);
        mSeekingProgressDurationText.setTypeface(tf);

        mLoadingDrawable = new CircularProgressDrawable(context);
        mLoadingDrawable.setColorSchemeColors(mColorAccent);
        mLoadingDrawable.setStrokeWidth(mResources.getDimension(R.dimen.circular_progress_stroke_width));
        mLoadingDrawable.setStrokeCap(Paint.Cap.ROUND);
        mLoadingImage.setImageDrawable(mLoadingDrawable);
    }

    @SuppressLint("RestrictedApi")
    private void inflateBottomControls() {
        ViewGroup root = mBottomControlsFrame;
        if (root.getChildCount() > 0) {
            root.removeViewAt(0);
        }

        if (isInFullscreenMode()) {
            if (isLocked()) {
                mVideoSeekBar = (SeekBar) LayoutInflater.from(mContext).inflate(
                        R.layout.bottom_controls_fullscreen_locked, root, false);
                root.addView(mVideoSeekBar);

                mVideoProgressText = null;
                mVideoDurationText = null;
                mMinimizeButton = null;
                mFullscreenButton = null;
                mSkipNextButton = null;
                mVideoProgressDurationText = null;
                mSpeedSpinner = null;
                mSpinnerListPopup = null;
                mSpinnerPopup = null;
                mChooseEpisodeButton = null;
                mToggleButton = null;
                return;
            }

            View.inflate(mContext, R.layout.bottom_controls_fullscreen, root);
            mSkipNextButton = root.findViewById(R.id.btn_skipNext);
            mVideoProgressDurationText = root.findViewById(R.id.text_videoProgressDuration);
            mSpeedSpinner = root.findViewById(R.id.spinner_speed);
            mChooseEpisodeButton = root.findViewById(R.id.btn_chooseEpisode);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    mContext, R.layout.item_speed_spinner, mSpeedsStringArray);
            adapter.setDropDownViewResource(R.layout.dropdown_item_speed_spinner);

            mSpeedSpinner.setPopupBackgroundResource(R.color.bg_popup);
            mSpeedSpinner.setAdapter(adapter);
            final float playbackSpeed = mVideoPlayer == null ?
                    IVideoPlayer.DEFAULT_PLAYBACK_SPEED : mVideoPlayer.mPlaybackSpeed;
            mSpeedSpinner.setSelection(indexOfPlaybackSpeed(playbackSpeed), false);
            mSpeedSpinner.setOnTouchListener(mOnChildTouchListener);
            mSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (view instanceof TextView /* This may be null during state restore */) {
                        TextView tv = (TextView) view;

                        ViewGroup.LayoutParams lp = tv.getLayoutParams();
                        lp.width = parent.getWidth();
                        lp.height = parent.getHeight();
                        tv.setLayoutParams(lp);

                        final String speed = tv.getText().toString();
                        if (mVideoPlayer != null) {
                            mVideoPlayer.setPlaybackSpeed(
                                    Float.parseFloat(speed.substring(0, speed.lastIndexOf('x'))));
                        }
                        // Filter the non-user-triggered selection changes, so that the visibility of
                        // the controls stay unchanged.
                        if ((mPrivateFlags & PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS) != 0) {
                            mPrivateFlags &= ~PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                            showControls(true, false);
                            checkCameraButtonsVisibilities();
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            refreshSpeedSpinner();

            if (sListPopupField != null && sPopupField != null) {
                try {
                    mSpinnerListPopup = (ListPopupWindow) sListPopupField.get(mSpeedSpinner);
                    //noinspection ConstantConditions
                    mSpinnerListPopup.setForceIgnoreOutsideTouch(true);
/*
                    // Works on platforms prior to P
                    if (sForceIgnoreOutsideTouchField != null) {
                        // Sets the field `mForceIgnoreOutsideTouch` of the ListPopupWindow to `true`
                        // to discourage it from setting `mOutsideTouchable` to `true` for the popup
                        // in its show() method, so that the popup receives no outside touch event
                        // to dismiss itself.
                        sForceIgnoreOutsideTouchField.setBoolean(mSpinnerListPopup, true);
                    }
 */
                    mSpinnerPopup = (PopupWindow) sPopupField.get(mSpinnerListPopup);
                    // This popup window reports itself as focusable so that it can intercept the
                    // back button. However, if we are currently in fullscreen mode, what will the
                    // aftereffect be？Yeah，the system bars will become visible to the user and
                    // even affect the user to choose a reasonable speed for the player.
                    // For all of the reasons, we're supposed to prevent it from doing that.
                    //noinspection ConstantConditions
                    mSpinnerPopup.setFocusable(false);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (!canSkipToNext()) {
                mSkipNextButton.setVisibility(GONE);
                if (!canSkipToPrevious()) {
                    mChooseEpisodeButton.setVisibility(GONE);
                }
            }

            mSkipNextButton.setOnClickListener(mOnChildClickListener);
            mChooseEpisodeButton.setOnClickListener(mOnChildClickListener);

            mVideoProgressText = null;
            mVideoDurationText = null;
            mMinimizeButton = null;
            mFullscreenButton = null;
        } else {
            View.inflate(mContext, R.layout.bottom_controls, root);
            mVideoProgressText = root.findViewById(R.id.text_videoProgress);
            mVideoDurationText = root.findViewById(R.id.text_videoDuration);
            mMinimizeButton = root.findViewById(R.id.btn_minimize);
            mFullscreenButton = root.findViewById(R.id.btn_fullscreen);

            mMinimizeButton.setOnClickListener(mOnChildClickListener);
            mFullscreenButton.setOnClickListener(mOnChildClickListener);

            mSkipNextButton = null;
            mVideoProgressDurationText = null;
            mSpeedSpinner = null;
            mSpinnerListPopup = null;
            mSpinnerPopup = null;
            mChooseEpisodeButton = null;
        }

        mVideoSeekBar = root.findViewById(R.id.sb_video);
        mVideoSeekBar.setOnSeekBarChangeListener(mOnVideoSeekBarChangeListener);

        mToggleButton = root.findViewById(R.id.btn_toggle);
        mToggleButton.setOnClickListener(mOnChildClickListener);
        adjustToggleState(mVideoPlayer != null && mVideoPlayer.isPlaying());
        checkButtonsAbilities();
    }

    private void adjustToggleState(boolean playing) {
        if (!isLocked()) {
            if (playing) {
                mToggleButton.setImageResource(R.drawable.ic_pause_white_32dp);
                mToggleButton.setContentDescription(mStringPause);
            } else {
                mToggleButton.setImageResource(R.drawable.ic_play_white_32dp);
                mToggleButton.setContentDescription(mStringPlay);
            }
        }
    }

    private void refreshSpeedSpinner() {
        if (!(mVideoPlayer instanceof SystemVideoPlayer)
                || Utils.isMediaPlayerPlaybackSpeedAdjustmentSupported()) {
            mSpeedSpinner.setEnabled(true);
        } else {
            mSpeedSpinner.setEnabled(false);
            mSpeedSpinner.setSelection(indexOfPlaybackSpeed(IVideoPlayer.DEFAULT_PLAYBACK_SPEED));
            if (isSpinnerPopupShowing()) {
                dismissSpinnerPopup();
            }
        }
    }

    private int indexOfPlaybackSpeed(float speed) {
        final String speedString = speed + "x";
        for (int i = 0; i < mSpeedsStringArray.length; i++) {
            if (mSpeedsStringArray[i].equals(speedString)) return i;
        }
        return -1;
    }

    /** @return the {@link VideoPlayer} object used by this view or null if unset */
    @Nullable
    public VideoPlayer getVideoPlayer() {
        return mVideoPlayer;
    }

    /**
     * See {@link #setVideoPlayer(VideoPlayer, boolean) setVideoPlayer(videoPlayer, true)}
     */
    public void setVideoPlayer(@Nullable VideoPlayer videoPlayer) {
        setVideoPlayer(videoPlayer, true);
    }

    /**
     * Sets the {@link VideoPlayer} for this view to load and play video contents.
     *
     * @param videoPlayer               the player to use, or {@code null} to detach the current player.
     * @param restoreLastPlayerSettings whether to restore the player settings to the newly set
     *                                  VideoPlayer (if the last is nonnull).
     */
    public void setVideoPlayer(@Nullable VideoPlayer videoPlayer, boolean restoreLastPlayerSettings) {
        VideoPlayer lastVideoPlayer = mVideoPlayer;
        if (lastVideoPlayer == videoPlayer) return;
        mVideoPlayer = videoPlayer;
        if (mEventListener != null) {
            mEventListener.onPlayerChange(videoPlayer);
        }

        // Closes the 'more' view first since we will not fully sync its UI state
        if (mMoreView != null) {
            closeDrawer(mDrawerView);
        }

        if (mSpeedSpinner != null) {
            refreshSpeedSpinner();
        }

        // Due to special processing mechanism to the Surface used by bilibili's IjkMediaPlayer,
        // we may need to recreate a new Surface for use with the new play engine.
        if (videoPlayer != null && !(videoPlayer instanceof IjkVideoPlayer)
                && lastVideoPlayer instanceof IjkVideoPlayer) {
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            if (surfaceTexture != null) {
                mSurface.release();
                mUsedSurface = mSurface = new Surface(surfaceTexture);
                videoPlayer.onVideoSurfaceChanged(mUsedSurface);
            }
        }

        if (lastVideoPlayer == null || !restoreLastPlayerSettings) {
            if (lastVideoPlayer != null) {
                lastVideoPlayer.closeVideoInternal(false);
            }

            if (videoPlayer != null) {
                if (lastVideoPlayer != null) {
                    if (!ObjectsCompat.equals(videoPlayer.mVideoUri, lastVideoPlayer.mVideoUri)) {
                        onVideoUriChanged(videoPlayer.mVideoUri);
                    }
                    if (videoPlayer.mVideoWidth != lastVideoPlayer.mVideoWidth
                            || videoPlayer.mVideoHeight != lastVideoPlayer.mVideoHeight) {
                        onVideoSizeChanged(videoPlayer.mVideoWidth, videoPlayer.mVideoHeight);
                    }
                    if (videoPlayer.mVideoDuration != lastVideoPlayer.mVideoDuration) {
                        onVideoDurationChanged(Math.max(0, videoPlayer.mVideoDuration));
                    }
                    if (videoPlayer.mPlaybackSpeed != lastVideoPlayer.mPlaybackSpeed) {
                        onPlaybackSpeedChanged(videoPlayer.mPlaybackSpeed);
                    }
                    final boolean canAudioPlayInBackground = videoPlayer.isAudioAllowedToPlayInBackground();
                    if (canAudioPlayInBackground != lastVideoPlayer.isAudioAllowedToPlayInBackground()) {
                        onAudioAllowedToPlayInBackgroundChanged(canAudioPlayInBackground);
                    }
                    final boolean singleVideoLoopPlayback = videoPlayer.isSingleVideoLoopPlayback();
                    if (singleVideoLoopPlayback != lastVideoPlayer.isSingleVideoLoopPlayback()) {
                        onSingleVideoLoopPlaybackModeChanged(singleVideoLoopPlayback);
                    }
                    // Here we also synchronize the UI state when VideoPlayer set from null to nonnull.
                } else {
                    onVideoUriChanged(videoPlayer.mVideoUri);
                    onVideoSizeChanged(videoPlayer.mVideoWidth, videoPlayer.mVideoHeight);
                    onVideoDurationChanged(Math.max(0, videoPlayer.mVideoDuration));
                    onPlaybackSpeedChanged(videoPlayer.mPlaybackSpeed);
                    onAudioAllowedToPlayInBackgroundChanged(videoPlayer.isAudioAllowedToPlayInBackground());
                    onSingleVideoLoopPlaybackModeChanged(videoPlayer.isSingleVideoLoopPlayback());
                }

                videoPlayer.openVideo();
                videoPlayer.play(false);
            }
        } else {
            // Caches the playback speed in case it changes during the video off
            final float lastPlaybackSpeed = lastVideoPlayer.mPlaybackSpeed;
            lastVideoPlayer.closeVideoInternal(false);

            if (videoPlayer == null) return;
            videoPlayer.mVideoListeners = null;
            List<IVideoPlayer.VideoListener> videoListeners = lastVideoPlayer.mVideoListeners;
            if (videoListeners != null && !videoListeners.isEmpty()) {
                videoPlayer.mVideoListeners = new ArrayList<>(videoListeners);
            }
            videoPlayer.mOnPlaybackStateChangeListeners = null;
            List<IVideoPlayer.OnPlaybackStateChangeListener> onPlaybackStateChangeListeners =
                    lastVideoPlayer.mOnPlaybackStateChangeListeners;
            if (onPlaybackStateChangeListeners != null && !onPlaybackStateChangeListeners.isEmpty()) {
                videoPlayer.mOnPlaybackStateChangeListeners =
                        new ArrayList<>(onPlaybackStateChangeListeners);
            }
            videoPlayer.mOnSkipPrevNextListener = lastVideoPlayer.mOnSkipPrevNextListener;
            // videoPlayer.setVideoUri(lastVideoPlayer.mVideoUri) may do nothing if
            // the encoded string representations of the new player's original Uri
            // and the one to be set for it are equal.
            videoPlayer.setVideoUri(lastVideoPlayer.mVideoUri);
            if (lastVideoPlayer.mVideoWidth != 0 || lastVideoPlayer.mVideoHeight != 0) {
                videoPlayer.onVideoSizeChanged(lastVideoPlayer.mVideoWidth, lastVideoPlayer.mVideoHeight);
            }
            if (lastVideoPlayer.mVideoDuration != IVideoPlayer.TIME_UNSET) {
                videoPlayer.onVideoDurationChanged(lastVideoPlayer.mVideoDuration);
                videoPlayer.seekTo(lastVideoPlayer.getVideoProgress(), false);
            }
            videoPlayer.setPlaybackSpeed(lastPlaybackSpeed);
            videoPlayer.setAudioAllowedToPlayInBackground(lastVideoPlayer.isAudioAllowedToPlayInBackground());
            videoPlayer.setSingleVideoLoopPlayback(lastVideoPlayer.isSingleVideoLoopPlayback());
        }
    }

    /**
     * Sets the listener to monitor all the events related to (some of the widgets of) this view.
     *
     * @see EventListener
     */
    public void setEventListener(@Nullable EventListener listener) {
        mEventListener = listener;
    }

    /**
     * Sets the callback to receive the operation callbacks from this view.
     *
     * @see OpCallback
     */
    public void setOpCallback(@Nullable OpCallback opCallback) {
        mOpCallback = opCallback;
    }

    /**
     * @return the brightness of the window this view is attached to or 0
     *         if no {@link OpCallback} is set.
     */
    public int getBrightness() {
        if (mOpCallback != null) {
            return ScreenUtils.getWindowBrightness(mOpCallback.getWindow());
        }
        return 0;
    }

    /**
     * Sets the brightness for the window to which this view is attached.
     * <p>
     * <strong>NOTE:</strong> When changing current view's brightness, you should invoke
     * this method instead of a direct call to {@link ScreenUtils#setWindowBrightness(Window, int)}
     */
    public void setBrightness(int brightness) {
        if (mOpCallback != null) {
            brightness = Util.constrainValue(brightness, BRIGHTNESS_FOLLOWS_SYSTEM, MAX_BRIGHTNESS);
            // Changes the brightness of the current Window
            ScreenUtils.setWindowBrightness(mOpCallback.getWindow(), brightness);
            // Sets that progress for the brightness ProgressBar
            refreshBrightnessProgress(brightness);
        }
    }

    @Synthetic int volumeToProgress(int volume) {
        return volume * RATIO_VOLUME_PROGRESS_TO_VOLUME;
    }

    @Synthetic int progressToVolume(int progress) {
        return (int) ((float) progress / RATIO_VOLUME_PROGRESS_TO_VOLUME + 0.5f);
    }

    /**
     * @return the current volume of the media, maybe 0 if the ringer mode is silent or vibration
     */
    public int getVolume() {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * Sets the media volume of the system used in the player.
     */
    public void setVolume(int volume) {
        volume = Util.constrainValue(volume, 0, mMaxVolume);
        // Changes the system's media volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        // Sets that progress for the volume ProgressBar
        refreshVolumeProgress(volumeToProgress(volume));
    }

    @Override
    public boolean canSkipToPrevious() {
        return (mPrivateFlags & PFLAG_CAN_SKIP_TO_PREVIOUS) != 0;
    }

    /**
     * Sets whether or not we can skip the playing video to the previous one.
     */
    public void setCanSkipToPrevious(boolean able) {
        if (able != canSkipToPrevious()) {
            mPrivateFlags = mPrivateFlags & ~PFLAG_CAN_SKIP_TO_PREVIOUS
                    | (able ? PFLAG_CAN_SKIP_TO_PREVIOUS : 0);

            if (mChooseEpisodeButton != null) {
                mChooseEpisodeButton.setVisibility((!able && !canSkipToNext()) ? GONE : VISIBLE);
            }

            if (canAccessBackgroundPlaybackControllerService()) {
                sBgPlaybackControllerServiceConn.service.onCanSkipToPreviousChange(able);
            }
        }
    }

    @Override
    public boolean canSkipToNext() {
        return (mPrivateFlags & PFLAG_CAN_SKIP_TO_NEXT) != 0;
    }

    /**
     * Sets whether or not we can skip the playing video to the next one.
     * <p>
     * If set to `true` and this view is currently in full screen mode, the 'Skip Next' button
     * will become visible to the user.
     */
    public void setCanSkipToNext(boolean able) {
        if (able != canSkipToNext()) {
            mPrivateFlags = mPrivateFlags & ~PFLAG_CAN_SKIP_TO_NEXT
                    | (able ? PFLAG_CAN_SKIP_TO_NEXT : 0);

            if (mSkipNextButton != null) {
                mSkipNextButton.setVisibility(able ? VISIBLE : GONE);
            }
            if (mChooseEpisodeButton != null) {
                mChooseEpisodeButton.setVisibility(
                        (!able && !canSkipToPrevious()) ? GONE : VISIBLE);
            }

            if (canAccessBackgroundPlaybackControllerService()) {
                sBgPlaybackControllerServiceConn.service.onCanSkipToNextChange(able);
            }
        }
    }

    /**
     * @param <VH> A class that extends {@link RecyclerView.ViewHolder} that was used by the adapter.
     * @return the RecyclerView adapter for the video playlist or `null` if not set
     */
    @Nullable
    public <VH extends RecyclerView.ViewHolder> PlayListAdapter<VH> getPlayListAdapter() {
        //noinspection unchecked
        return (PlayListAdapter<VH>) mPlayList.getAdapter();
    }

    /**
     * Sets an adapter for the RecyclerView that displays the video playlist
     *
     * @param adapter see {@link PlayListAdapter}
     * @param <VH>    A class that extends {@link RecyclerView.ViewHolder} that will be used by the adapter.
     */
    public <VH extends RecyclerView.ViewHolder> void setPlayListAdapter(@Nullable PlayListAdapter<VH> adapter) {
        if (adapter != null && mPlayList.getLayoutManager() == null) {
            mPlayList.setLayoutManager(new LinearLayoutManager(mContext));
            mPlayList.addItemDecoration(
                    new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL));
        }
        mPlayList.setAdapter(adapter);
    }

    /**
     * @return title of the video.
     */
    @Nullable
    public String getTitle() {
        return mTitle;
    }

    /**
     * Sets the title of the video to play.
     */
    public void setTitle(@Nullable String title) {
        if ("".equals(title)) {
            title = null;
        }
        if (!ObjectsCompat.equals(title, mTitle)) {
            mTitle = title;
            if (isInFullscreenMode()) {
                mTitleText.setText(title);
            }

            if (canAccessBackgroundPlaybackControllerService()) {
                sBgPlaybackControllerServiceConn.service.onMediaTitleChange(title);
            }
        }
    }

    /**
     * @return {@code true} if the bounds of this view is clipped to adapt for the video
     */
    public boolean isClipViewBounds() {
        return (mPrivateFlags & PFLAG_CLIP_VIEW_BOUNDS) != 0;
    }

    /**
     * Sets whether this view should crop its border to fit the aspect ratio of the video.
     * <p>
     * <strong>NOTE:</strong> After invoking this method, you may need to directly call
     * {@link #requestLayout()} to refresh the layout or do that via an implicit invocation
     * which will call it internally (such as {@link #setLayoutParams(ViewGroup.LayoutParams)}).
     *
     * @param clip If true, the bounds of this view will be clipped;
     *             otherwise, black bars will be filled to the view's remaining area.
     */
    public void setClipViewBounds(boolean clip) {
        if (clip != isClipViewBounds()) {
            mPrivateFlags = mPrivateFlags & ~PFLAG_CLIP_VIEW_BOUNDS
                    | (clip ? PFLAG_CLIP_VIEW_BOUNDS : 0);
            if (clip) {
                ViewCompat.setBackground(this, null);
            } else {
                setBackgroundColor(Color.BLACK);
            }
        }
    }

    /**
     * @return whether this view is in fullscreen mode or not
     */
    public boolean isInFullscreenMode() {
        return (mPrivateFlags & PFLAG_IN_FULLSCREEN_MODE) != 0;
    }

    /**
     * Sets this view to put it into fullscreen mode or not.
     * If set, minimize and fullscreen buttons will disappear (visibility set to {@link #GONE}) and
     * the specified padding `navTopInset` will be inserted at the top of the top controls' frame.
     * <p>
     * <strong>NOTE:</strong> This method does not resize the view as the system bars and
     * the screen orientation may need to be adjusted simultaneously, meaning that you should
     * implement your own logic to resize it.
     *
     * @param fullscreen  Whether this view should go into fullscreen mode.
     * @param navTopInset The downward offset of the navigation widget relative to its initial
     *                    position. Normally, when setting fullscreen mode, you need to move it
     *                    down a proper distance, so that it appears below the status bar.
     */
    public void setFullscreenMode(boolean fullscreen, int navTopInset) {
        final int paddingTop = mNavInitialPaddingTop + navTopInset;
        if (mTopControlsFrame.getPaddingTop() != paddingTop) {
            mTopControlsFrame.setPadding(
                    mTopControlsFrame.getPaddingLeft(),
                    paddingTop,
                    mTopControlsFrame.getPaddingRight(),
                    mTopControlsFrame.getPaddingBottom());
        }

        if (fullscreen != isInFullscreenMode()) {
            mPrivateFlags = mPrivateFlags & ~PFLAG_IN_FULLSCREEN_MODE
                    | (fullscreen ? PFLAG_IN_FULLSCREEN_MODE : 0);
            if (fullscreen) {
                mTitleText.setText(mTitle);
                if (isControlsShowing()) {
                    mLockUnlockButton.setVisibility(VISIBLE);
                    mCameraButton.setVisibility(VISIBLE);
                    mVideoCameraButton.setVisibility(VISIBLE);
                }
            } else {
                mTitleText.setText(null);
                if (isLocked()) {
                    setLocked(false, false);
                    if (isControlsShowing()) {
                        // Removes the PFLAG_CONTROLS_SHOWING flag first, since our controls
                        // to be showed changes
                        mPrivateFlags &= ~PFLAG_CONTROLS_SHOWING;
                        showControls(true, false);
                    }
                    mLockUnlockButton.setVisibility(GONE);
                } else {
                    mLockUnlockButton.setVisibility(GONE);
                    mCameraButton.setVisibility(GONE);
                    mVideoCameraButton.setVisibility(GONE);
                    cancelVideoPhotoCapture();
                    hideClipView(true /* usually true */);
                    // Only closes the playlist when this view is out of fullscreen mode
                    if (mPlayList.getVisibility() == VISIBLE && isDrawerVisible(mDrawerView)) {
                        closeDrawer(mDrawerView);
                    }
                }
            }
            inflateBottomControls();

            final int mode = fullscreen
                    ? isVideoStretchedToFitFullscreenLayout() ? //@formatter:off
                            VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN : VIEW_MODE_FULLSCREEN //@formatter:on
                    : VIEW_MODE_DEFAULT;
            setViewMode(mode, true);
        }
    }

    /**
     * @return whether the video is forced to be stretched in proportion to fit the layout size
     *         in fullscreen.
     */
    public boolean isVideoStretchedToFitFullscreenLayout() {
        return (mPrivateFlags & PFLAG_VIDEO_STRETCHED_TO_FIT_FULLSCREEN_LAYOUT) != 0;
    }

    /**
     * Sets the video to be forced to be proportionally stretched to fit the layout size in fullscreen,
     * which may be incompletely displayed if its aspect ratio is unequal to the current view's.
     * <p>
     * <strong>NOTE:</strong> If the clip view bounds flag is also set, then it always wins.
     */
    public void setVideoStretchedToFitFullscreenLayout(boolean stretched) {
        setVideoStretchedToFitFullscreenLayoutInternal(stretched, true);
    }

    @Synthetic void setVideoStretchedToFitFullscreenLayoutInternal(boolean stretched, boolean checkSwitch) {
        if (stretched == isVideoStretchedToFitFullscreenLayout()) {
            return;
        }
        mPrivateFlags = mPrivateFlags & ~PFLAG_VIDEO_STRETCHED_TO_FIT_FULLSCREEN_LAYOUT
                | (stretched ? PFLAG_VIDEO_STRETCHED_TO_FIT_FULLSCREEN_LAYOUT : 0);
        if (checkSwitch && mMoreView != null) {
            Checkable toggle = mMoreView.findViewById(R.id.btn_stretchVideo);
            if (stretched != toggle.isChecked()) {
                toggle.setChecked(stretched);
            }
        }
        if (isInFullscreenMode()) {
            if (!isClipViewBounds() && mVideoPlayer != null) {
                final int videoWidth = mVideoPlayer.getVideoWidth();
                final int videoHeight = mVideoPlayer.getVideoHeight();
                if (videoWidth != 0 && videoHeight != 0) {
                    final int width = mContentView.getWidth();
                    final int height = mContentView.getHeight();
                    if (!Utils.areEqualIgnorePrecisionError(
                            (float) width / height, (float) videoWidth / videoHeight)) {
                        final float scale = stretched ?
                                Math.max((float) width / mTextureView.getWidth(),
                                        (float) height / mTextureView.getHeight())
                                : 1.0f;
                        ViewPropertyAnimatorCompat vpac = ViewCompat.animate(mTextureView);
                        vpac.withLayer()
                                .scaleX(scale)
                                .scaleY(scale)
                                .setInterpolator(sStretchShrinkVideoInterpolator)
                                .setDuration(500)
                                .start();
                        mTextureView.setTag(vpac);
                    }
                }
            }

            if (isLocked()) {
                if (stretched) {
                    setViewMode(VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN, true);
                } else {
                    setViewMode(VIEW_MODE_LOCKED_FULLSCREEN, true);
                }
            } else if (stretched) {
                setViewMode(VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN, true);
            } else {
                setViewMode(VIEW_MODE_FULLSCREEN, true);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        final boolean clipBounds = isClipViewBounds();
        final boolean fullscreen = isInFullscreenMode();

//        if (!clipBounds) {
//            width -= (getPaddingLeft() + getPaddingRight());
//            height -= (getPaddingTop() + getPaddingBottom());
//        }
        final float aspectRatio = (float) width / height;

        if (mVideoPlayer != null) {
            final int videoWidth = mVideoPlayer.getVideoWidth();
            final int videoHeight = mVideoPlayer.getVideoHeight();
            if (videoWidth != 0 && videoHeight != 0) {
                final float videoAspectRatio = (float) videoWidth / videoHeight;

                final int tvw, tvh;
                if (videoAspectRatio >= aspectRatio) {
                    tvw = width;
                    tvh = (int) (width / videoAspectRatio + 0.5f);
                } else {
                    tvw = (int) (height * videoAspectRatio + 0.5f);
                    tvh = height;
                }

                if (clipBounds) {
                    width = tvw;
                    height = tvh;
                }

                ViewPropertyAnimatorCompat vpac = (ViewPropertyAnimatorCompat) mTextureView.getTag();
                if (vpac != null) {
                    vpac.cancel();
                }
                if (fullscreen && !clipBounds && isVideoStretchedToFitFullscreenLayout()) {
                    final float scale = Math.max((float) width / tvw, (float) height / tvh);
                    mTextureView.setScaleX(scale);
                    mTextureView.setScaleY(scale);
                } else {
                    mTextureView.setScaleX(1.0f);
                    mTextureView.setScaleY(1.0f);
                }

                ViewGroup.LayoutParams tvlp = mTextureView.getLayoutParams();
                tvlp.width = tvw;
                tvlp.height = tvh;
//                mTextureView.setLayoutParams(tvlp);
            }
        }

        ViewGroup.LayoutParams lp = mDrawerView.getLayoutParams();
        if (fullscreen) {
            // When in landscape mode, we need to make the drawer view appear to the user appropriately.
            // Its width should not occupy too much display space，so as not to affect the user
            // to preview the video content.
            if (aspectRatio > 1.0f) {
                mDrawerViewMinimumHeight = height;
                lp.width = (int) (width / 2f + 0.5f); // XXX: to make this more adaptable
            } else {
                mDrawerViewMinimumHeight = 0;
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            mDrawerViewMinimumHeight = 0;
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = (int) (height * 0.85f + 0.5f);
        }
//        mDrawerView.setLayoutParams(lp);

        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Ensures the drawer view to be opened/closed normally during this layout change
        int openState = 0;
        if (changed && sDrawerOpenStateField != null) {
            LayoutParams lp = (LayoutParams) mDrawerView.getLayoutParams();
            try {
                openState = sDrawerOpenStateField.getInt(lp);
                if ((openState & (FLAG_IS_OPENING | FLAG_IS_CLOSING)) != 0) {
                    if (mDragHelper == null) {
                        final int absHG = Utils.getAbsoluteHorizontalGravity(this, lp.gravity);
                        mDragHelper = (ViewDragHelper) (absHG == Gravity.LEFT ?
                                sLeftDraggerField.get(this) : sRightDraggerField.get(this));
                    }
                    //noinspection ConstantConditions
                    if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_SETTLING) {
                        mDragHelper.abort();
                    } else {
                        // Only when
                        // `(openState & (FLAG_IS_OPENING | FLAG_IS_CLOSING)) != 0)
                        //      && drawerState /* mDragHelper.getViewDragState() */ == STATE_SETTLING`
                        // is the drawer view really being opened/closed.
                        openState = 0;
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        super.onLayout(changed, l, t, r, b);
        if (changed && openState != 0) {
            if ((openState & FLAG_IS_OPENING) != 0) {
                openDrawer(mDrawerView);
            } else if ((openState & FLAG_IS_CLOSING) != 0) {
                closeDrawer(mDrawerView);
            }
        }

        // Postponing checking over the visibilities of the camera buttons ensures that we can
        // correctly get the widget locations on screen so that we can decide whether or not
        // to show them and the View displaying the captured video photo.
        mMsgHandler.removeMessages(MsgHandler.MSG_CHECK_CAMERA_BUTTONS_VISIBILITIES);
        mMsgHandler.sendEmptyMessage(MsgHandler.MSG_CHECK_CAMERA_BUTTONS_VISIBILITIES);
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);

        // Update our internal visibility tracking so we can detect changes
        final boolean oldVisible = (mPrivateFlags & PFLAG_AGGREGATED_VISIBLE) != 0;
        mPrivateFlags = (mPrivateFlags & ~PFLAG_AGGREGATED_VISIBLE)
                | (isVisible ? PFLAG_AGGREGATED_VISIBLE : 0);

        if (isVisible != oldVisible) {
            final Surface usedSurface = mUsedSurface;
            // Uses the previously created Surface when this view becomes visible again
            if (isVisible && usedSurface == null) {
                mUsedSurface = mSurface;
                // No Surface will be used for the player when this view is invisible
            } else if (!isVisible && usedSurface != null) {
                mUsedSurface = null;
            }
            if (mVideoPlayer != null && mUsedSurface != usedSurface) {
                mVideoPlayer.onVideoSurfaceChanged(mUsedSurface);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelVideoPhotoCapture();
        hideClipView(false);

        mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME);
        mBrightnessOrVolumeFrame.setVisibility(GONE);

        if (mTimedOffRunnable != null) {
            removeCallbacks(mTimedOffRunnable);
            mTimedOffRunnable = null;
        }

        tryStopBackgroundPlaybackControllerService();
        if (mVideoPlayer != null) {
            mVideoPlayer.closeVideoInternal(false);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        final boolean handled = super.onKeyUp(keyCode, event);
        if (handled && keyCode == KeyEvent.KEYCODE_BACK) {
            // Fix a bug in Android 8.1 and below: we used the LOCKED_CLOSED drawer lock mode,
            // but as long as the drawer view is visible, the event is consumed,
            // resulting in the onBackPressed() method to be not called from the host.
            return false;
        }
        return handled;
    }

    /**
     * Call this when the host of the view (Activity for instance) has detected the user's press of
     * the back key to close some widget opened or exit from the fullscreen mode.
     *
     * @return true if the back key event is handled by this view
     */
    @Override
    public boolean onBackPressed() {
        if (mClipView != null) {
            hideClipView(true);
            return true;
        } else if (isDrawerVisible(mDrawerView)) {
            closeDrawer(mDrawerView);
            return true;
        } else if (isInFullscreenMode()) {
            setViewMode(VIEW_MODE_DEFAULT, false);
            return true;
        }
        return false;
    }

    @Override
    public void onMinimizationModeChange(boolean minimized) {
        setViewMode(minimized ? VIEW_MODE_MINIMUM : VIEW_MODE_DEFAULT, true);
    }

    /**
     * @return the present mode for this view, maybe one of
     *         {@link #VIEW_MODE_DEFAULT},
     *         {@link #VIEW_MODE_MINIMUM},
     *         {@link #VIEW_MODE_FULLSCREEN},
     *         {@link #VIEW_MODE_LOCKED_FULLSCREEN},
     *         {@link #VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN},
     *         {@link #VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN}
     */
    @ViewMode
    public final int getViewMode() {
        return mViewMode;
    }

    @Synthetic void setViewMode(@ViewMode int mode, boolean layoutMatches) {
        final int old = mViewMode;
        if (old != mode) {
            mViewMode = mode;
            if (mEventListener != null) {
                mEventListener.onViewModeChange(old, mode, layoutMatches);
            }
        }
    }

    /**
     * Returns whether or not the current view is locked.
     */
    public boolean isLocked() {
        return (mPrivateFlags & PFLAG_LOCKED) != 0;
    }

    /** See {@link #setLocked(boolean, boolean) setLocked(locked, true)} */
    public void setLocked(boolean locked) {
        setLocked(locked, true);
    }

    /**
     * Sets this view to be locked or not. When it is locked, all option controls are hided
     * except for the lock toggle button and a progress bar used for indicating where
     * the current video is played and the invisible control related ops are disabled, too.
     *
     * @param locked  Whether to lock this view
     * @param animate Whether the locking or unlocking of this view should be animated.
     *                This only makes sense when this view is currently in fullscreen mode.
     */
    public void setLocked(boolean locked, boolean animate) {
        if (locked != isLocked()) {
            final boolean fullscreen = isInFullscreenMode();
            final boolean showing = isControlsShowing();
            if (fullscreen && showing) {
                if (animate && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Fade fade = new Fade();
                    Utils.includeChildrenForTransition(fade, mContentView,
                            mTopControlsFrame,
                            mLockUnlockButton, mCameraButton, mVideoCameraButton,
                            mBottomControlsFrame);

                    ChangeBounds cb = new ChangeBounds();
                    Utils.includeChildrenForTransition(cb, mContentView, mBottomControlsFrame);

                    TransitionManager.beginDelayedTransition(mContentView,
                            new TransitionSet().addTransition(fade).addTransition(cb));
                }
                showControls(false, false);
            }
            if (locked) {
                mPrivateFlags |= PFLAG_LOCKED;
                mLockUnlockButton.setContentDescription(mStringLock);
                mLockUnlockButton.setImageResource(R.drawable.ic_lock_white_24dp);
            } else {
                mPrivateFlags &= ~PFLAG_LOCKED;
                mLockUnlockButton.setContentDescription(mStringUnlock);
                mLockUnlockButton.setImageResource(R.drawable.ic_unlock_white_24dp);
            }
            if (fullscreen) {
                inflateBottomControls();
                if (showing) {
                    showControls(true, false);
                }

                if (locked) {
                    if (isVideoStretchedToFitFullscreenLayout()) {
                        setViewMode(VIEW_MODE_VIDEO_STRETCHED_LOCKED_FULLSCREEN, true);
                    } else {
                        setViewMode(VIEW_MODE_LOCKED_FULLSCREEN, true);
                    }
                } else if (isVideoStretchedToFitFullscreenLayout()) {
                    setViewMode(VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN, true);
                } else {
                    setViewMode(VIEW_MODE_FULLSCREEN, true);
                }
            }
        }
    }

    /**
     * @return whether the controls are currently showing or not
     */
    public boolean isControlsShowing() {
        return (mPrivateFlags & PFLAG_CONTROLS_SHOWING) != 0;
    }

    /** See {@link #showControls(boolean, boolean) showControls(show, true)} */
    public void showControls(boolean show) {
        showControls(show, true);
    }

    /**
     * Shows the controls on screen. They will go away automatically after
     * {@value #TIMEOUT_SHOW_CONTROLS} milliseconds of inactivity.
     * If the controls are already showing, Calling this method also makes sense, as it will keep
     * the controls showing till a new {@value #TIMEOUT_SHOW_CONTROLS} ms delay is past.
     *
     * @param animate whether to fade in/out the controls smoothly or not
     */
    public void showControls(boolean show, boolean animate) {
        if ((mPrivateFlags & PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS) != 0) {
            return;
        }
        if (show) {
            if (mVideoPlayer != null && mVideoPlayer.isPlaying()) {
                showControls(TIMEOUT_SHOW_CONTROLS, animate);
            } else {
                // stay showing
                showControls(-1, animate);
            }
        } else {
            hideControls(animate);
        }
    }

    /**
     * Shows the controls on screen. They will go away automatically
     * after `timeout` milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use negative to show the controls
     *                till {@link #hideControls(boolean)} is called.
     * @param animate whether to fade in the controls smoothly or not
     */
    @Synthetic void showControls(int timeout, boolean animate) {
        mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_CONTROLS);

        if ((mPrivateFlags & PFLAG_CONTROLS_SHOWING) == 0) {
            mPrivateFlags |= PFLAG_CONTROLS_SHOWING;
            final boolean unlocked = !isLocked();
            if (animate) {
                beginControlsFadingTransition(true, unlocked);
            }
            if (unlocked) {
                mTopControlsFrame.setVisibility(VISIBLE);
            }
            if (isInFullscreenMode()) {
                mLockUnlockButton.setVisibility(VISIBLE);
                if (unlocked) {
                    mCameraButton.setVisibility(VISIBLE);
                    mVideoCameraButton.setVisibility(VISIBLE);
                }
            }
            mBottomControlsFrame.setVisibility(VISIBLE);
        }

        // Cause the video progress bar to be updated even if it is already showing.
        // This happens, for example, if video is paused with the progress bar showing,
        // the user hits play.
        mMsgHandler.removeMessages(MsgHandler.MSG_REFRESH_VIDEO_PROGRESS);
        mMsgHandler.sendEmptyMessage(MsgHandler.MSG_REFRESH_VIDEO_PROGRESS);

        if (timeout >= 0) {
            mMsgHandler.sendEmptyMessageDelayed(MsgHandler.MSG_HIDE_CONTROLS, timeout);
        }
    }

    /**
     * Hides the controls at both ends in the vertical from the screen.
     *
     * @param animate whether to fade out the controls smoothly or not
     */
    private void hideControls(boolean animate) {
        // Removes the pending action of hiding the controls as this is being called.
        mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_CONTROLS);

        if ((mPrivateFlags & PFLAG_CONTROLS_SHOWING) != 0) {
            mPrivateFlags &= ~PFLAG_CONTROLS_SHOWING;
            final boolean unlocked = !isLocked();
            if (animate) {
                beginControlsFadingTransition(false, unlocked);
            }
            if (unlocked) {
                mTopControlsFrame.setVisibility(GONE);
            }
            if (isInFullscreenMode()) {
                mLockUnlockButton.setVisibility(GONE);
                if (unlocked) {
                    mCameraButton.setVisibility(GONE);
                    mVideoCameraButton.setVisibility(GONE);
                    cancelVideoPhotoCapture();
                }
            }
            mBottomControlsFrame.setVisibility(GONE);
        }
    }

    private void beginControlsFadingTransition(boolean in, boolean unlocked) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition transition = new Fade(in ? Fade.IN : Fade.OUT);
            if (unlocked) {
                Utils.includeChildrenForTransition(transition, mContentView,
                        mTopControlsFrame,
                        mLockUnlockButton, mCameraButton, mVideoCameraButton,
                        mBottomControlsFrame);
            } else {
                Utils.includeChildrenForTransition(transition, mContentView,
                        mLockUnlockButton,
                        mBottomControlsFrame);
            }
            TransitionManager.beginDelayedTransition(mContentView, transition);
        }
    }

    private void showTextureView(boolean show) {
        if (show) {
            mTextureView.setVisibility(VISIBLE);

            // Hides the TextureView only if its SurfaceTexture was created upon its first drawing
            // since it had been attached to this view, or no Surface would be available for
            // rendering the video content (Further to say, according to the logic of us,
            // there would start no video at all).
        } else if (mSurface != null) {
            // Temporarily makes the TextureView invisible. Do NOT use GONE as the Surface used to
            // render the video content will also be released when it is detached from this view
            // (the onSurfaceTextureDestroyed() method of its SurfaceTextureListener is called).
            mTextureView.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void showSubtitles(@Nullable List<Cue> cues) {
        mSubtitleView.setCues(cues);
    }

    @Override
    public void showSubtitle(@Nullable CharSequence text, @Nullable Rect textBounds) {
        List<Cue> cues = null;
        if (!TextUtils.isEmpty(text)) {
            cues = new ArrayList<>(1);

            final float viewportW = mTextureView.getWidth();
            final float viewportH = mTextureView.getHeight();

            if (textBounds == null || textBounds.isEmpty() || viewportW == 0 || viewportH == 0) {
                //noinspection ConstantConditions
                cues.add(new Cue(text));
            } else {
                //noinspection ConstantConditions
                cues.add(new Cue(text,
                        /* textAlignment= */ null,
                        /* line= */(float) textBounds.top / viewportH,
                        /* lineType= */ Cue.LINE_TYPE_FRACTION,
                        /* lineAnchor= */ Cue.ANCHOR_TYPE_START,
                        /* position= */(float) textBounds.left / viewportW,
                        /* positionAnchor= */ Cue.ANCHOR_TYPE_START,
                        /* size= */  (float) textBounds.width() / viewportW));
            }
        }
        mSubtitleView.setCues(cues);
    }

    private void showLoadingView(boolean show) {
        if (show) {
            if (mLoadingImage.getVisibility() != VISIBLE) {
                mLoadingImage.setVisibility(VISIBLE);
                mLoadingDrawable.start();
            }
        } else if (mLoadingImage.getVisibility() != GONE) {
            mLoadingImage.setVisibility(GONE);
            mLoadingDrawable.stop();
        }
    }

    @Synthetic void checkCameraButtonsVisibilities() {
        boolean show = isControlsShowing() && isInFullscreenMode() && !isLocked();
        if (show && isSpinnerPopupShowing()) {
            final int[] location = new int[2];

            View popupRoot = mSpinnerPopup.getContentView().getRootView();
            popupRoot.getLocationOnScreen(location);
            final int popupTop = location[1];

            View camera = mVideoCameraButton;
            camera.getLocationOnScreen(location);
            final int cameraBottom = location[1] + camera.getHeight();

            if (popupTop < cameraBottom + 25f * mResources.getDisplayMetrics().density) {
                show = false;
            }
        }
        if (!show) {
            cancelVideoPhotoCapture();
        }
        final int visibility = show ? VISIBLE : GONE;
        mCameraButton.setVisibility(visibility);
        mVideoCameraButton.setVisibility(visibility);

        if (show) {
            checkCameraButtonAbility();
        }
    }

    private void checkCameraButtonAbility() {
        TextureView tv = mTextureView;
        mCameraButton.setEnabled(mSurface != null && tv.getWidth() != 0 && tv.getHeight() != 0);
    }

    private void checkButtonsAbilities() {
        VideoPlayer vp = mVideoPlayer;
        if (isInFullscreenMode()) {
            if (!isLocked()) {
                checkCameraButtonAbility();
                mVideoCameraButton.setEnabled(vp != null
                        && vp.mVideoWidth != 0 && vp.mVideoHeight != 0
                        && vp.mVideoDuration != IVideoPlayer.TIME_UNSET);
            }
        } else {
            mMinimizeButton.setEnabled(vp != null && vp.mVideoWidth != 0 && vp.mVideoHeight != 0);
        }
    }

    @Synthetic void hideCapturedPhotoView(boolean share) {
        if (mCapturedPhotoView != null) {
            Transition transition = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                transition = (Transition) mCapturedPhotoView.getTag();
                transition.addListener(new TransitionListenerAdapter() {
                    @Override
                    public void onTransitionEnd(Transition transition) {
                        // Recycling of the bitmap captured for the playing video MUST ONLY be done
                        // after the transition ends, in case we use a recycled bitmap for drawing.
                        mCapturedBitmap.recycle();
                        mCapturedBitmap = null;
                    }
                });
                TransitionManager.beginDelayedTransition(mContentView, transition);
            }
            mContentView.removeView(mCapturedPhotoView);
            mCapturedPhotoView = null;
            if (transition == null) {
                mCapturedBitmap.recycle();
                mCapturedBitmap = null;
            }

            if (share && mEventListener != null) {
                mEventListener.onShareCapturedVideoPhoto(mSavedPhoto);
            }
            mSavedPhoto = null;
        }
    }

    private void cancelVideoPhotoCapture() {
        Animation a = mContentView.getAnimation();
        if (a != null && a.hasStarted() && !a.hasEnded()) {
            // We call onAnimationEnd() manually to ensure it to be called immediately to restore
            // the playback state when the animation cancels and before the next animation starts
            // (if any), when the listener of the animation will be removed, so that no second call
            // will be introduced by the clearAnimation() method below.
            ((Animation.AnimationListener) mContentView.getTag()).onAnimationEnd(a);
            mContentView.clearAnimation();
        }
        if (mSaveCapturedPhotoTask != null) {
            mSaveCapturedPhotoTask.cancel(false);
            mSaveCapturedPhotoTask = null;
        }
        mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_CAPTURED_PHOTO_VIEW);
        hideCapturedPhotoView(false);
    }

    @Synthetic void captureVideoPhoto() {
        if (mSurface == null) return;

        final int width = mTextureView.getWidth();
        final int height = mTextureView.getHeight();
        if (width == 0 || height == 0) return;

        final ViewGroup content = mContentView;

        Animation animation = content.getAnimation();
        if (animation != null && animation.hasStarted() && !animation.hasEnded()) {
            ((Animation.AnimationListener) content.getTag()).onAnimationEnd(animation);
            animation.cancel();
        }
        if (mSaveCapturedPhotoTask != null) {
            mSaveCapturedPhotoTask.cancel(false);
            mSaveCapturedPhotoTask = null;
        }

        final Bitmap bitmap = mTextureView.getBitmap(width, height);

        final float oldAspectRatio = mCapturedPhotoView == null ?
                0 : (float) mCapturedBitmap.getWidth() / mCapturedBitmap.getHeight();
        final float aspectRatio = (float) width / height;

        final boolean capturedPhotoViewValid;
        if (mCapturedPhotoView == null) {
            capturedPhotoViewValid = false;
        } else {
            mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_CAPTURED_PHOTO_VIEW);
            if (aspectRatio >= 1 && oldAspectRatio >= 1
                    || aspectRatio < 1 && oldAspectRatio < 1) {
                capturedPhotoViewValid = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(
                            content, (Transition) mCapturedPhotoView.getTag());
                }
                mCapturedPhotoView.setVisibility(INVISIBLE);
            } else {
                capturedPhotoViewValid = false;

                hideCapturedPhotoView(false);
            }
        }

        if (animation == null) {
            animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration(256);
        }
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            boolean playing;

            @SuppressLint("StaticFieldLeak")
            @Override
            public void onAnimationStart(Animation animation) {
                playing = mVideoPlayer != null && mVideoPlayer.isPlaying();
                if (playing) {
                    mVideoPlayer.pause(true);
                }

                final String appExternalFilesDir = obtainAppExternalFilesDir();
                mSaveCapturedPhotoTask = new AsyncTask<Void, Void, File>() {
                    @SuppressLint("SimpleDateFormat")
                    @Override
                    public File doInBackground(Void... voids) {
                        return FileUtils.saveBitmapToDisk(
                                mContext,
                                bitmap,
                                /* format= */ Bitmap.CompressFormat.PNG,
                                /* quality= */ 100,
                                /* directory= */ appExternalFilesDir + "/screenshots",
                                /* fileName= */ mTitle + "_"
                                        + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS") //@formatter:off
                                                .format(System.currentTimeMillis()) //@formatter:on
                                        + ".png");
                    }

                    @Override
                    public void onPostExecute(File photo) {
                        mSavedPhoto = photo;
                        if (photo == null) {
                            Utils.showUserCancelableSnackbar(TextureVideoView.this,
                                    R.string.saveScreenshotFailed, Snackbar.LENGTH_SHORT);
                            if (capturedPhotoViewValid) {
                                hideCapturedPhotoView(false);
                            }
                        } else {
                            mCapturedBitmap = bitmap;

                            View cpv = mCapturedPhotoView;
                            if (capturedPhotoViewValid) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    TransitionManager.beginDelayedTransition(
                                            content, (Transition) cpv.getTag());
                                }
                                cpv.setVisibility(VISIBLE);
                            } else {
                                mCapturedPhotoView = cpv = LayoutInflater.from(mContext).inflate(
                                        aspectRatio > 1
                                                ? R.layout.layout_captured_video_photo
                                                : R.layout.layout_captured_video_photo_portrait,
                                        content, false);
                            }

                            TextView shareButton = cpv.findViewById(R.id.btn_sharePhoto);
                            shareButton.setOnClickListener(mOnChildClickListener);

                            ImageView photoImage = cpv.findViewById(R.id.image_videoPhoto);
                            photoImage.setImageBitmap(bitmap);

                            if (!Utils.areEqualIgnorePrecisionError(aspectRatio, oldAspectRatio)) {
                                ViewGroup.LayoutParams lp = photoImage.getLayoutParams();
                                if (aspectRatio > 1) {
                                    // Not use measure(0,0) for the TextView here for two key reasons:
                                    // 1. It causes both the photoImage & shareButton to be displayed
                                    //    not as expected in a layout whose direction is right-to-left.
                                    // 2. Under some circumstances, it will not work correctly, like
                                    //    large font size mode.
                                    shareButton.post(() -> {
                                        lp.width = shareButton.getWidth();
                                        lp.height = (int) (lp.width / aspectRatio + 0.5f);
                                        photoImage.setLayoutParams(lp);
                                    });
                                } else {
                                    // Makes the text arrange vertically
                                    final String text = shareButton.getText().toString();
                                    final int length = text.length();
                                    final StringBuilder sb = new StringBuilder();
                                    for (int i = 0; i < length; i++) {
                                        sb.append(text.substring(i, i + 1));
                                        if (i < length - 1) sb.append("\n");
                                    }
                                    shareButton.setText(sb);

                                    shareButton.post(() -> {
                                        lp.height = shareButton.getHeight();
                                        lp.width = (int) (lp.height * aspectRatio + 0.5f);
                                        photoImage.setLayoutParams(lp);
                                    });
                                }
                            }

                            if (!capturedPhotoViewValid) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    Transition transition = new Fade();
                                    Utils.includeChildrenForTransition(transition, content);
                                    TransitionManager.beginDelayedTransition(content, transition);

                                    cpv.setTag(transition);
                                }
                                content.addView(cpv);
                            }
                            mMsgHandler.sendEmptyMessageDelayed(
                                    MsgHandler.MSG_HIDE_CAPTURED_PHOTO_VIEW,
                                    TIMEOUT_SHOW_CAPTURED_PHOTO);
                        }

                        mSaveCapturedPhotoTask = null;
                    }
                }.executeOnExecutor(ParallelThreadExecutor.getSingleton());
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animation.setAnimationListener(null);
                content.setTag(null);

                if (playing && mVideoPlayer != null) {
                    mVideoPlayer.play(mVideoPlayer.isInnerPlayerCreated());
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        animation.setAnimationListener(listener);
        content.setTag(listener);
        content.startAnimation(animation);
    }

    @Synthetic String obtainAppExternalFilesDir() {
        String directory = null;
        if (mOpCallback != null) {
            directory = mOpCallback.getAppExternalFilesDir();
        }
        if (directory == null) {
            directory = Environment.getExternalStorageDirectory() + "/" + mAppName;
        }
        return directory;
    }

    @SuppressLint("StaticFieldLeak")
    @Synthetic void showClipView() {
        if (mClipView != null) return;

        VideoPlayer videoPlayer = mVideoPlayer;
        // Not available when video info is waiting to be known
        if (videoPlayer == null
                || videoPlayer.mVideoDuration == IVideoPlayer.TIME_UNSET
                || (videoPlayer.mVideoWidth == 0 || videoPlayer.mVideoHeight == 0)) {
            return;
        }

        final int progress = videoPlayer.getVideoProgress();
        final int duration = videoPlayer.getNoNegativeVideoDuration();
        final float videoAspectRatio = (float) videoPlayer.getVideoWidth() / videoPlayer.getVideoHeight();
        final Uri videoUri = videoPlayer.mVideoUri;

        final int defaultRange = VideoClipView.DEFAULT_MAX_CLIP_DURATION
                + VideoClipView.DEFAULT_MIN_UNSELECTED_CLIP_DURATION;
        final int range; // selectable time interval in millisecond, starting with 0.
        final int rangeOffset; // first value of the above interval plus the mapped playback position.
        if (duration >= defaultRange) {
            range = defaultRange;
            final float halfOfMaxClipDuration = VideoClipView.DEFAULT_MAX_CLIP_DURATION / 2f;
            final float intervalOffset = (defaultRange - halfOfMaxClipDuration) / 2f;
            float intervalEnd = progress + halfOfMaxClipDuration + intervalOffset;
            if (intervalEnd > duration) {
                intervalEnd = duration;
            }
            rangeOffset = Math.max((int) (intervalEnd - range + 0.5f), 0);
        } else {
            range = duration;
            rangeOffset = 0;
        }

        final int[] interval = new int[2];

        final ViewGroup view;
        mClipView = view = (ViewGroup) LayoutInflater.from(mContext)
                .inflate(R.layout.layout_video_clip, mContentView, false);
        final View cutoutShortVideoButton = view.findViewById(R.id.btn_cutoutShortVideo);
        final View cutoutGifButton = view.findViewById(R.id.btn_cutoutGif);
        final View cancelButton = view.findViewById(R.id.btn_cancel);
        final View okButton = view.findViewById(R.id.btn_ok);
        final TextView vcdText = view.findViewById(R.id.text_videoclipDescription);
        final SurfaceView sv = view.findViewById(R.id.surfaceView);
        final VideoClipView vcv = view.findViewById(R.id.view_videoclip);

        cutoutShortVideoButton.setSelected(true);

        @SuppressLint("SimpleDateFormat") final OnClickListener listener = v -> {
            if (v == cutoutShortVideoButton) {
                cutoutShortVideoButton.setSelected(true);
                cutoutGifButton.setSelected(false);

            } else if (v == cutoutGifButton) {
                cutoutGifButton.setSelected(true);
                cutoutShortVideoButton.setSelected(false);

            } else if (v == cancelButton) {
                hideClipView(true);

            } else if (v == okButton) {
                hideClipView(true);

                /*
                 * Below code blocks for cutting out the desired short video or GIF.
                 * This should normally be done on a worker thread rather than the main thread that
                 * blocks the UI from updating itself till the op completes, but here we do it on
                 * the main thread just for temporary convenience as the code undoubtedly needs
                 * to be improved at some point in the future.
                 */

                final boolean cutoutShortVideo = cutoutShortVideoButton.isSelected();
                if (!cutoutShortVideo) {
                    Utils.showUserCancelableSnackbar(this,
                            R.string.gifClippingIsNotYetSupported, Snackbar.LENGTH_SHORT);
                    return;
                }

                final String srcPath = FileUtils.UriResolver.getPath(mContext, videoUri);
                if (srcPath == null) {
                    Log.e(TAG, "Failed to resolve the path of the video being clipped.");
                    Utils.showUserCancelableSnackbar(this,
                            R.string.clippingFailed, Snackbar.LENGTH_SHORT);
                    return;
                }

                //noinspection ConstantConditions
                final String destDirectory = obtainAppExternalFilesDir()
                        + "/clips/" + (cutoutShortVideo ? "ShortVideos" : "GIFs");
                //noinspection ConstantConditions
                final String destName = mTitle + "_"
                        + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS") //@formatter:off
                                .format(System.currentTimeMillis()) //@formatter:on
                        + (cutoutShortVideo ? ".mp4" : ".gif");
                final String destPath = destDirectory + "/" + destName;
                File destFile = null;
                //noinspection ConstantConditions,StatementWithEmptyBody
                if (cutoutShortVideo) {
                    try {
                        destFile = VideoUtils.clip(srcPath, destPath, interval[0], interval[1]);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                } else {
                    // TODO: the logic of cutting out a GIF
                }
                if (destFile == null) {
                    Utils.showUserCancelableSnackbar(this,
                            R.string.clippingFailed, Snackbar.LENGTH_SHORT);
                } else //noinspection ConstantConditions
                    if (cutoutShortVideo) {
                        FileUtils.recordMediaFileToDatabaseAndScan(mContext,
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                destFile, "video/mp4");
                        Utils.showUserCancelableSnackbar(this,
                                mResources.getString(R.string.shortVideoHasBeenSavedTo, destName, destDirectory),
                                true, Snackbar.LENGTH_INDEFINITE);
                    } else {
                        FileUtils.recordMediaFileToDatabaseAndScan(mContext,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                destFile, "image/gif");
                        Utils.showUserCancelableSnackbar(this,
                                mResources.getString(R.string.gifHasBeenSavedTo, destName, destDirectory),
                                true, Snackbar.LENGTH_INDEFINITE);
                    }
            }
        };
        cutoutShortVideoButton.setOnClickListener(listener);
        cutoutGifButton.setOnClickListener(listener);
        cancelButton.setOnClickListener(listener);
        okButton.setOnClickListener(listener);

//        view.measure(MeasureSpec.makeMeasureSpec(mContentView.getWidth(), MeasureSpec.EXACTLY),
//                MeasureSpec.makeMeasureSpec(mContentView.getHeight(), MeasureSpec.EXACTLY));
//        sv.getLayoutParams().width = (int) (sv.getMeasuredHeight() * videoAspectRatio + 0.5f);
        ConstraintLayout.LayoutParams svlp = (ConstraintLayout.LayoutParams) sv.getLayoutParams();
        svlp.dimensionRatio = String.valueOf(videoAspectRatio);

        final SurfaceHolder holder = sv.getHolder();
        final MediaSourceFactory factory =
                Utils.canUseExoPlayer() && videoPlayer instanceof ExoVideoPlayer
                        ? ((ExoVideoPlayer) videoPlayer).obtainMediaSourceFactory(videoUri) : null;
        final VideoClipPlayer player = new VideoClipPlayer(mContext, holder, videoUri, mExoUserAgent, factory);
        final Runnable trackProgressRunnable = new Runnable() {
            @Override
            public void run() {
                final int position = player.getCurrentPosition();
                vcv.setSelection(position - rangeOffset);
                if (player.isPlaying()) {
                    if (position < interval[0] || position > interval[1]) {
                        player.seekTo(interval[0]);
                    }
                    vcv.post(this);
                }
            }
        };
        final boolean[] selectionBeingDragged = {false};
        vcv.addOnSelectionChangeListener(new VideoClipView.OnSelectionChangeListener() {
            final String seconds = mResources.getString(R.string.seconds);
            final ForegroundColorSpan colorAccentSpan = new ForegroundColorSpan(mColorAccent);

            @Override
            public void onStartTrackingTouch() {
                if (player.isPlaying()) {
                    holder.setKeepScreenOn(false);
                    player.pause();
                    vcv.removeCallbacks(trackProgressRunnable);
                }
                selectionBeingDragged[0] = true;
            }

            @Override
            public void onSelectionIntervalChange(int start, int end, boolean fromUser) {
                interval[0] = rangeOffset + start;
                interval[1] = rangeOffset + end;

                final int total = (int) (vcv.getMaximumClipDuration() / 1000f + 0.5f);
                final int selected = (int) ((end - start) / 1000f + 0.5f);
                final String s = mResources.getString(
                        R.string.canTakeUpToXSecondsXSecondsSelected, total, selected);
                final SpannableString ss = new SpannableString(s);
                ss.setSpan(colorAccentSpan,
                        s.lastIndexOf(String.valueOf(selected)),
                        s.lastIndexOf(seconds) + seconds.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                vcdText.setText(ss);
            }

            @Override
            public void onSelectionChange(int start, int end, int selection, boolean fromUser) {
                if (fromUser) {
                    player.seekTo(rangeOffset + selection);
                }
            }

            @Override
            public void onStopTrackingTouch() {
                if (holder.getSurface().isValid()) {
                    holder.setKeepScreenOn(true);
                    if (mVideoPlayer != null) {
                        mVideoPlayer.closeVideoInternal(true /* no or little use */);
                    }
                    player.play();
                    vcv.post(trackProgressRunnable);
                }
                selectionBeingDragged[0] = false;
            }
        });
        // MUST set the durations after the above OnSelectionChangeListener was added to vcv, which
        // will ensure the onSelectionInternalChange() method to be called for the first time.
        if (range < defaultRange) {
            vcv.setMaximumClipDuration(range);
            vcv.setMinimumClipDuration(Math.min(VideoClipView.DEFAULT_MIN_CLIP_DURATION, range));
            vcv.setMinimumUnselectedClipDuration(0);
        }
        final int minClipDuration = vcv.getMinimumClipDuration();
        final int maxClipDuration = vcv.getMaximumClipDuration();
        final int minUnselectedClipDuration = vcv.getMinimumUnselectedClipDuration();
        final int totalDuration = maxClipDuration + minUnselectedClipDuration;
        final int initialSelection = progress - rangeOffset;
        final int tmpInterval = (int) (maxClipDuration / 2f + 0.5f);
        int intervalEnd = initialSelection + tmpInterval;
        if (intervalEnd > totalDuration) {
            intervalEnd = totalDuration;
        }
        int intervalStart = intervalEnd - tmpInterval;
        if (tmpInterval < minClipDuration) {
            final int diff = minClipDuration - tmpInterval;
            final int remaining = totalDuration - intervalEnd;
            if (remaining >= diff) {
                intervalEnd += diff;
            } else {
                intervalEnd = totalDuration;
                intervalStart -= diff - remaining;
            }
        }
        vcv.setSelectionInterval(intervalStart, intervalEnd);
        vcv.setSelection(initialSelection);
        vcv.post(() -> {
            final int thumbHeight = vcv.getThumbDisplayHeight();
            final float thumbWidth = thumbHeight * videoAspectRatio;
            final int thumbGalleryWidth = vcv.getThumbGalleryWidth();
            final int thumbCount = (int) (thumbGalleryWidth / thumbWidth + 0.5f);
            final int finalThumbWidth = (int) ((float) thumbGalleryWidth / thumbCount + 0.5f);
            mLoadClipThumbsTask = new AsyncTask<Void, Bitmap, Void>() {
                @Override
                public Void doInBackground(Void... voids) {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    try {
                        final String videoUriString = videoUri.toString();
                        if (URLUtils.isNetworkUrl(videoUriString)) {
                            mmr.setDataSource(videoUriString, Collections.emptyMap());
                        } else {
                            mmr.setDataSource(mContext, videoUri);
                        }
                        if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null) {
                            for (int i = 0; i < thumbCount && !isCancelled(); ) {
                                Bitmap frame = mmr.getFrameAtTime((long)
                                        (rangeOffset + (i++ + 0.5) * range / thumbCount) * 1000L);
                                if (frame == null) {
                                    // If no frame at the specified time position is retrieved,
                                    // create a empty placeholder bitmap instead.
                                    frame = Bitmap.createBitmap(
                                            finalThumbWidth, thumbHeight, Bitmap.Config.ALPHA_8);
                                } else {
                                    frame = BitmapUtils.createScaledBitmap(
                                            frame, finalThumbWidth, thumbHeight, true);
                                }
                                publishProgress(frame);
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } finally {
                        mmr.release();
                    }
                    return null;
                }

                @Override
                public void onProgressUpdate(Bitmap... thumbs) {
                    vcv.addThumbnail(thumbs[0]);
                }

                @Override
                public void onPostExecute(Void aVoid) {
                    mLoadClipThumbsTask = null;
                }
            }.executeOnExecutor(ParallelThreadExecutor.getSingleton());
        });
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                player.create();
                // Seeks to the playback millisecond position mapping to the initial selection
                // as we were impossible to seek in the above OnSelectionChangeListener's
                // onSelectionChange() method when the player was not created; also we
                // have been leaving out the selection changes that are caused by the program code
                // rather than the user.
                player.seekTo(progress);
                if (!selectionBeingDragged[0]) {
                    holder.setKeepScreenOn(true);
                    // We need to make sure of the video to be closed before the clip preview starts,
                    // because the video in one of the special formats (e.g. mov) will not play if
                    // the video resource is not released in advance.
                    // This will also abandon the audio focus gained for the preceding video playback.
                    // That's why we do this just before the preview starts, for the purpose of not
                    // letting another media application have the opportunity to resume its playback.
                    if (mVideoPlayer != null) {
                        mVideoPlayer.closeVideoInternal(true /* no or little use */);
                    }
                    player.play();
                    vcv.post(trackProgressRunnable);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                holder.setKeepScreenOn(false);
                player.pause();
                player.release();
                vcv.removeCallbacks(trackProgressRunnable);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition transition = new Fade();
            Utils.includeChildrenForTransition(transition, mContentView,
                    mTopControlsFrame,
                    mLockUnlockButton, mCameraButton, mVideoCameraButton,
                    mBottomControlsFrame,
                    view);
            transition.excludeTarget(sv, true);
            TransitionManager.beginDelayedTransition(mContentView, transition);
        }
        videoPlayer.pause(true);
        showControls(false, false);
        mContentView.addView(view);
    }

    private void hideClipView(boolean fromUser) {
        if (mClipView != null) {
            mContentView.removeView(mClipView);
            mClipView = null;
            if (mLoadClipThumbsTask != null) {
                mLoadClipThumbsTask.cancel(false);
                mLoadClipThumbsTask = null;
            }

            if (mVideoPlayer != null) {
                mVideoPlayer.play(fromUser);
            }
            showControls(true); // Make sure the controls will be showed immediately
        }
    }

    @Synthetic void refreshBrightnessProgress(int progress) {
        if ((mOnChildTouchListener.touchFlags & OnChildTouchListener.TFLAG_ADJUSTING_BRIGHTNESS)
                == OnChildTouchListener.TFLAG_ADJUSTING_BRIGHTNESS) {
            final boolean brightnessFollowsSystem = progress == -1;
            mBrightnessOrVolumeText.setText(
                    brightnessFollowsSystem
                            ? mStringBrightnessFollowsSystem
                            : mResources.getString(R.string.brightnessProgress, //@formatter:off
                                    (float) progress / MAX_BRIGHTNESS * 100f)); //@formatter:on
            mBrightnessOrVolumeProgress.setProgress(brightnessFollowsSystem ? 0 : progress);
        }
    }

    @Synthetic void refreshVolumeProgress(int progress) {
        if ((mOnChildTouchListener.touchFlags & OnChildTouchListener.TFLAG_ADJUSTING_VOLUME)
                == OnChildTouchListener.TFLAG_ADJUSTING_VOLUME) {
            mBrightnessOrVolumeText.setText(
                    mResources.getString(R.string.volumeProgress,
                            (float) progress / volumeToProgress(mMaxVolume) * 100f));
            mBrightnessOrVolumeProgress.setProgress(progress);
        }
    }

    @Synthetic void refreshVideoProgress(int progress) {
        refreshVideoProgress(progress, true);
    }

    @Synthetic void refreshVideoProgress(int progress, boolean refreshSeekBar) {
        VideoPlayer videoPlayer = mVideoPlayer;
        if (videoPlayer == null) progress = 0;
        final int videoBufferProgress = videoPlayer == null ? 0 : videoPlayer.getVideoBufferProgress();
        final int videoDuration = videoPlayer == null ? 0 : videoPlayer.getNoNegativeVideoDuration();
        final String videoDurationString = videoPlayer == null ?
                VideoPlayer.DEFAULT_STRING_VIDEO_DURATION : videoPlayer.mVideoDurationString;
        if (!isLocked()) {
            if (isInFullscreenMode()) {
                mVideoProgressDurationText.setText(
                        mResources.getString(R.string.progress_duration,
                                TimeUtil.formatTimeByColon(progress), videoDurationString));
            } else {
                mVideoProgressText.setText(TimeUtil.formatTimeByColon(progress));
            }
        }
        if (mVideoSeekBar.getMax() != videoDuration) {
            mVideoSeekBar.setMax(videoDuration);
            if (mVideoDurationText != null) {
                mVideoDurationText.setText(videoDurationString);
            }
        }
        mVideoSeekBar.setSecondaryProgress(videoBufferProgress);
        if (refreshSeekBar) {
            mVideoSeekBar.setProgress(progress);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void cancelDraggingVideoSeekBar(boolean seekPlaybackPosition) {
        if (!seekPlaybackPosition) {
            mPrivateFlags |= PFLAG_DISALLOW_PLAYBACK_POSITION_SEEK_ON_STOP_TRACKING_TOUCH;
        }

        MotionEvent ev = null;
        if ((mOnChildTouchListener.touchFlags & OnChildTouchListener.TFLAG_ADJUSTING_VIDEO_PROGRESS) != 0) {
            ev = Utils.obtainCancelEvent();
            mOnChildTouchListener.onTouchContent(ev);

        } else if (mVideoSeekBar.isPressed()) {
            ev = Utils.obtainCancelEvent();
            mVideoSeekBar.onTouchEvent(ev);
            // Sets an OnTouchListener for it to intercept the subsequent touch events within
            // this event stream, so that the seek bar stays not dragged.
            mVideoSeekBar.setOnTouchListener((v, event) -> {
                final int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.setOnTouchListener(null);
                        return action != MotionEvent.ACTION_DOWN;
                }
                return true;
            });
        }
        if (ev != null) ev.recycle();

        if (!seekPlaybackPosition) {
            mPrivateFlags &= ~PFLAG_DISALLOW_PLAYBACK_POSITION_SEEK_ON_STOP_TRACKING_TOUCH;
        }
    }

    @Synthetic boolean isSpinnerPopupShowing() {
        return mSpinnerPopup != null && mSpinnerPopup.isShowing();
    }

    @Synthetic void dismissSpinnerPopup() {
        if (mSpinnerListPopup != null) {
            mSpinnerListPopup.dismiss();
        }
    }

    private final class OnChildTouchListener implements OnTouchListener, ConstraintLayout.TouchInterceptor {
        OnChildTouchListener() {
        }

        int touchFlags;
        static final int TFLAG_STILL_DOWN_ON_POPUP = 1;
        static final int TFLAG_DOWN_ON_STATUS_BAR_AREA = 1 << 1;
        static final int TFLAG_ADJUSTING_BRIGHTNESS = 1 << 2;
        static final int TFLAG_ADJUSTING_VOLUME = 1 << 3;
        static final int TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME =
                TFLAG_ADJUSTING_BRIGHTNESS | TFLAG_ADJUSTING_VOLUME;
        static final int TFLAG_ADJUSTING_VIDEO_PROGRESS = 1 << 4;
        static final int MASK_ADJUSTING_PROGRESS_FLAGS =
                TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME | TFLAG_ADJUSTING_VIDEO_PROGRESS;

        // for SpeedSpinner
        float popupDownX, popupDownY;
        final Runnable postPopupOnClickedRunnable = this::onClickSpinner;

        // for ContentView
        int activePointerId = MotionEvent.INVALID_POINTER_ID;
        float downX, downY;
        float lastX, lastY;
        final GestureDetector detector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return isLocked() || isSpinnerPopupShowing() /*|| isDrawerVisible(mDrawerView)*/;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (isLocked() || isSpinnerPopupShowing()) {
                    return true;
                }
                if (isDrawerVisible(mDrawerView)) {
                    closeDrawer(mDrawerView);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isSpinnerPopupShowing()) {
                    dismissSpinnerPopup();

                } else if (!isDrawerVisible(mDrawerView)) {
                    showControls(!isControlsShowing());
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isSpinnerPopupShowing()) {
                    dismissSpinnerPopup();

                } else if (mVideoPlayer != null && !(isLocked() || isDrawerVisible(mDrawerView))) {
                    mVideoPlayer.toggle(true);
                }
                return true;
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return isLocked() || isSpinnerPopupShowing() /*|| isDrawerVisible(mDrawerView)*/;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return isLocked() /*|| isSpinnerPopupShowing() || isDrawerVisible(mDrawerView)*/;
            }
        });

        @Override
        public boolean shouldInterceptTouchEvent(@NonNull MotionEvent ev) {
            if (isSpinnerPopupShowing()) {
                // If the spinner's popup is showing, let content view intercept the touch events to
                // prevent the user from pressing the buttons ('play/pause', 'skip next', 'back', etc.)
                // All the things we do is for the aim that try our best to make the popup act as if
                // it was focusable.
                return true;
            }
            // No child of the content view but the 'unlock' button can receive touch events when
            // this view is locked.
            if (isLocked()) {
                final float x = ev.getX();
                final float y = ev.getY();
                final View lub = mLockUnlockButton;
                return x < lub.getLeft() || x > lub.getRight() || y < lub.getTop() || y > lub.getBottom();
            }
            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (v == mContentView) {
                return onTouchContent(event);
            } else if (v == mDrawerView) {
                return onTouchDrawerTransparentArea(event);
            } else if (v == mSpeedSpinner) {
                return onTouchSpinner(event);
            }
            return false;
        }

        // Offer the speed spinner an OnClickListener as needed
        @SuppressWarnings("SameReturnValue")
        boolean onTouchSpinner(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    popupDownX = event.getX();
                    popupDownY = event.getY();
                    touchFlags |= TFLAG_STILL_DOWN_ON_POPUP;
                    mSpeedSpinner.removeCallbacks(postPopupOnClickedRunnable);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((touchFlags & TFLAG_STILL_DOWN_ON_POPUP) != 0) {
                        final float absDx = Math.abs(event.getX() - popupDownX);
                        final float absDy = Math.abs(event.getY() - popupDownY);
                        if (absDx * absDx + absDy * absDy > mTouchSlop * mTouchSlop) {
                            touchFlags &= ~TFLAG_STILL_DOWN_ON_POPUP;
                            mSpeedSpinner.removeCallbacks(postPopupOnClickedRunnable);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if ((touchFlags & TFLAG_STILL_DOWN_ON_POPUP) != 0) {
                        touchFlags &= ~TFLAG_STILL_DOWN_ON_POPUP;
                        // Delay 100 milliseconds to let the spinner's onClick() be called before
                        // our one is called so that we can access the variables created in its show()
                        // method via reflections without any NullPointerException.
                        // This is a bit similar to the GestureDetector's onSingleTapConfirmed() method,
                        // but not so rigorous as our logic processing is lightweight and effective
                        // enough in this use case.
                        mSpeedSpinner.postDelayed(postPopupOnClickedRunnable, 100);
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_CANCEL:
                    touchFlags &= ~TFLAG_STILL_DOWN_ON_POPUP;
                    mSpeedSpinner.removeCallbacks(postPopupOnClickedRunnable);
                    break;
            }
            return false; // we just need an OnClickListener, so not consume events
        }

        @SuppressLint("ClickableViewAccessibility")
        void onClickSpinner() {
            mPrivateFlags |= PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
            showControls(-1, true);
            checkCameraButtonsVisibilities();

            if (mSpinnerPopup == null) return;
            try {
/*
                // Needed on platform versions >= P only
                if (sPopupDecorViewField != null) {
                    // Although this is a member field in the PopupWindow class, it is created in the
                    // popup's show() method and reset to `null` each time the popup dismisses. Thus,
                    // always retrieving it via reflection after the spinner clicked is really needed.
                    ((View) sPopupDecorViewField.get(mSpinnerPopup)).setOnTouchListener((v, event) ->
                            // This is roughly the same as the onTouchEvent() of the popup's decorView,
                            // but just returns `true` according to the same conditions on actions 'down'
                            // and 'outside' instead of additionally dismissing the popup as we need it
                            // to remain showing within this event stream till the up event is arrived.
                            //
                            // @see PopupWindow.PopupDecorView.onTouchEvent(MotionEvent)
                    {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                final float x = event.getX();
                                final float y = event.getY();
                                if (x < 0 || x >= v.getWidth() || y < 0 || y >= v.getHeight()) {
                                    // no dismiss()
                                    return true;
                                }
                                break;
                            case MotionEvent.ACTION_OUTSIDE:
                                // no dismiss()
                                return true;
                        }
                        return false;
                    });
                }
*/
                if (sPopupOnDismissListenerField == null) return;
                // A local variable in Spinner/AppCompatSpinner class. Do NOT cache!
                // We do need to get it via reflection each time the spinner's popup window
                // shows to the user, though this may cause the program to run slightly slower.
                final PopupWindow.OnDismissListener listener =
                        (PopupWindow.OnDismissListener) sPopupOnDismissListenerField.get(mSpinnerPopup);
                // This is a little bit of a hack, but... we need to get notified when the spinner's
                // popup window dismisses, so as not to cause the controls unhiddable (even if
                // the client calls showControls(false), it does nothing for the
                // PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS flag keeps it from doing what the client wants).
                mSpinnerPopup.setOnDismissListener(() -> {
                    // First, lets the internal one get notified to release some related resources
                    //noinspection ConstantConditions
                    listener.onDismiss();

                    // Then, do what we want (hide the controls in both the vertical ends after
                    // a delay of 5 seconds)
                    mPrivateFlags &= ~PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
                    showControls(true, false);
                    checkCameraButtonsVisibilities();

                    // Third, clear reference to let gc do its work
                    mSpinnerPopup.setOnDismissListener(null);
                });
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("SameReturnValue")
        boolean onTouchDrawerTransparentArea(MotionEvent event) {
            detector.onTouchEvent(event);
            return true;
        }

        boolean onTouchContent(MotionEvent event) {
            if (detector.onTouchEvent(event) || isLocked()) {
                return true;
            }

            final int action = event.getAction();

            if (isSpinnerPopupShowing()) {
                if (action == MotionEvent.ACTION_UP) {
                    dismissSpinnerPopup();
                }
                return true;
            }

            // In fullscreen mode, if the y coordinate of the initial 'down' event is less than
            // the navigation top inset, it is easy to make the brightness/volume progress bar showing
            // while the user is pulling down the status bar, of which, however, the user may have
            // no tendency. In that case, to avoid touch conflicts, we just return `true` instead.
            if (isInFullscreenMode()) {
                if (action == MotionEvent.ACTION_DOWN) {
                    final int navTopInset = mTopControlsFrame.getPaddingTop() - mNavInitialPaddingTop;
                    touchFlags = touchFlags & ~TFLAG_DOWN_ON_STATUS_BAR_AREA
                            | (event.getY() <= navTopInset ? TFLAG_DOWN_ON_STATUS_BAR_AREA : 0);
                }
                if ((touchFlags & TFLAG_DOWN_ON_STATUS_BAR_AREA) != 0) return true;
            }

            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    final int actionIndex = event.getActionIndex();
                    lastX = downX = event.getX(actionIndex);
                    lastY = downY = event.getY(actionIndex);
                    activePointerId = event.getPointerId(actionIndex);
                    break;

                case MotionEvent.ACTION_MOVE:
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex < 0) {
                        Log.e(TAG, "Error processing slide; pointer index for id "
                                + activePointerId + " not found. Did any MotionEvents get skipped?");
                        return false;
                    }

                    final boolean rtl = Utils.isLayoutRtl(mContentView);

                    final float x = event.getX(pointerIndex);
                    final float y = event.getY(pointerIndex);
                    // positive when finger swipes towards the end of horizontal
                    final float deltaX = rtl ? lastX - x : x - lastX;
                    final float deltaY = lastY - y; // positive when finger swipes up
                    lastX = x;
                    lastY = y;

                    switch (touchFlags & MASK_ADJUSTING_PROGRESS_FLAGS) {
                        case TFLAG_ADJUSTING_BRIGHTNESS: {
                            final int progress = mBrightnessOrVolumeProgress.getProgress();
                            final int newProgress = computeProgressOnTrackTouchVertically(
                                    mBrightnessOrVolumeProgress, deltaY, 1.0f);
                            if (newProgress == progress) {
                                if (progress == 0 && deltaY < 0) {
                                    setBrightness(-1);
                                }
                            } else {
                                setBrightness(newProgress);
                            }
                        }
                        break;

                        case TFLAG_ADJUSTING_VOLUME: {
                            final int progress = mBrightnessOrVolumeProgress.getProgress();
                            final int newProgress = computeProgressOnTrackTouchVertically(
                                    mBrightnessOrVolumeProgress, deltaY, 1.0f);
                            if (newProgress != progress) {
                                mAudioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC, progressToVolume(newProgress), 0);
                                refreshVolumeProgress(newProgress);
                            }
                        }
                        break;

                        case TFLAG_ADJUSTING_VIDEO_PROGRESS: {
                            final int progress = mVideoSeekBar.getProgress();
                            final int newProgress = computeProgressOnTrackTouchHorizontally(
                                    mVideoSeekBar, deltaX, 0.33333334f);
                            if (newProgress != progress) {
                                mVideoSeekBar.setProgress(newProgress);
                                mOnVideoSeekBarChangeListener.onProgressChanged(
                                        mVideoSeekBar, newProgress, true);
                            }
                        }
                        break;

                        case TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME:
                            //noinspection IntegerDivisionInFloatingPointContext
                            if (mOpCallback != null &&
                                    (!rtl && x < mContentView.getWidth() / 2
                                            || rtl && x > mContentView.getWidth() / 2)) {
                                touchFlags = touchFlags & ~TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME
                                        | TFLAG_ADJUSTING_BRIGHTNESS;
                                mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME);
                                mBrightnessOrVolumeFrame.setVisibility(VISIBLE);
                                mBrightnessOrVolumeProgress.setMax(MAX_BRIGHTNESS);
                                refreshBrightnessProgress(getBrightness());
                            } else {
                                touchFlags = touchFlags & ~TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME
                                        | TFLAG_ADJUSTING_VOLUME;
                                mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME);
                                mBrightnessOrVolumeFrame.setVisibility(VISIBLE);
                                mBrightnessOrVolumeProgress.setMax(volumeToProgress(mMaxVolume));
                                refreshVolumeProgress(volumeToProgress(getVolume()));
                            }
                            break;

                        default:
                            final float absDx = Math.abs(x - downX);
                            final float absDy = Math.abs(y - downY);
                            if (absDy >= absDx) {
                                if (absDy > mTouchSlop) {
                                    touchFlags = touchFlags & ~MASK_ADJUSTING_PROGRESS_FLAGS
                                            | TFLAG_ADJUSTING_BRIGHTNESS_OR_VOLUME;
                                }
                            } else if (absDx > mTouchSlop) {
                                touchFlags = touchFlags & ~MASK_ADJUSTING_PROGRESS_FLAGS
                                        | TFLAG_ADJUSTING_VIDEO_PROGRESS;
                                if (!isControlsShowing()) {
                                    mVideoSeekBar.setProgress(
                                            mVideoPlayer == null ? 0 : mVideoPlayer.getVideoProgress());
                                }
                                mOnVideoSeekBarChangeListener.onStartTrackingTouch(mVideoSeekBar);
                            }
                            break;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    onSecondaryPointerUp(event);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Cache the touchFlags to avoid VideoSeekBar's onStopTrackingTouch() to be called
                    // once again from the cancelDraggingVideoSeekBar() method.
                    final int tflags = this.touchFlags;
                    touchFlags &= ~MASK_ADJUSTING_PROGRESS_FLAGS;
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    switch (tflags & MASK_ADJUSTING_PROGRESS_FLAGS) {
                        case TFLAG_ADJUSTING_BRIGHTNESS:
                        case TFLAG_ADJUSTING_VOLUME:
                            mMsgHandler.sendEmptyMessageDelayed(
                                    MsgHandler.MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME,
                                    TIMEOUT_SHOW_BRIGHTNESS_OR_VOLUME);
                            break;
                        case TFLAG_ADJUSTING_VIDEO_PROGRESS:
                            mOnVideoSeekBarChangeListener.onStopTrackingTouch(mVideoSeekBar);
                            break;
                    }
                    break;
            }
            return true;
        }

        private void onSecondaryPointerUp(MotionEvent ev) {
            final int pointerIndex = ev.getActionIndex();
            final int pointerId = ev.getPointerId(pointerIndex);
            if (pointerId == activePointerId) {
                // This was our active pointer going up.
                // Choose a new active pointer and adjust accordingly.
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                activePointerId = ev.getPointerId(newPointerIndex);
                lastX = downX = ev.getX(newPointerIndex);
                lastY = downY = ev.getY(newPointerIndex);
            }
        }

        int computeProgressOnTrackTouchHorizontally(
                ProgressBar progressBar, float deltaX,
                @SuppressWarnings("SameParameterValue") float sensitivity) {
            final int maxProgress = progressBar.getMax();
            final int progress = progressBar.getProgress()
                    + Math.round((float) maxProgress / mContentView.getWidth() * deltaX * sensitivity);
            return Util.constrainValue(progress, 0, maxProgress);
        }

        int computeProgressOnTrackTouchVertically(
                ProgressBar progressBar, float deltaY,
                @SuppressWarnings("SameParameterValue") float sensitivity) {
            final int maxProgress = progressBar.getMax();
            final int progress = progressBar.getProgress()
                    + Math.round((float) maxProgress / mContentView.getHeight() * deltaY * sensitivity);
            return Util.constrainValue(progress, 0, maxProgress);
        }
    }

    private final class OnChildClickListener implements View.OnClickListener {
        OnChildClickListener() {
        }

        @Override
        public void onClick(View v) {
            if (mTitleText == v) {
                if (mEventListener != null) {
                    mEventListener.onReturnClicked();
                }
            } else if (mShareButton == v) {
                if (mEventListener != null) {
                    showControls(false);
                    mEventListener.onShareVideo();
                }
            } else if (mTrackButton == v) {
                if (mTrackSelectionView == null) {
                    mTrackSelectionView = (TrackSelectionView)
                            LayoutInflater.from(mContext)
                                    .inflate(R.layout.drawer_view_track_selection, mDrawerView, false);
                    mTrackSelectionView.setVideoPlayer(mVideoPlayer);
                    mTrackSelectionView.setMinimumHeight(mDrawerViewMinimumHeight);
                    mDrawerView.addView(mTrackSelectionView);
                    openDrawer(mDrawerView);
                }
            } else if (mMoreButton == v) {
                showMoreView();

            } else if (mLockUnlockButton == v) {
                setLocked(mStringUnlock.contentEquals(v.getContentDescription()));

            } else if (mCameraButton == v) {
                showControls(true, false);
                captureVideoPhoto();

            } else if (mVideoCameraButton == v) {
                showClipView();

            } else if (mToggleButton == v) {
                if (mVideoPlayer != null) {
                    mVideoPlayer.toggle(true);
                }
            } else if (mSkipNextButton == v) {
                if (mVideoPlayer != null) {
                    mVideoPlayer.skipToNextIfPossible();
                }
            } else if (mFullscreenButton == v) {
                final int mode = isVideoStretchedToFitFullscreenLayout() ?
                        VIEW_MODE_VIDEO_STRETCHED_FULLSCREEN : VIEW_MODE_FULLSCREEN;
                setViewMode(mode, false);

            } else if (mMinimizeButton == v) {
                setViewMode(VIEW_MODE_MINIMUM, false);

            } else if (mChooseEpisodeButton == v) {
                if (ViewCompat.getMinimumHeight(mPlayList) != mDrawerViewMinimumHeight) {
                    mPlayList.setMinimumHeight(mDrawerViewMinimumHeight);
                }
                mPlayList.setVisibility(VISIBLE);
                openDrawer(mDrawerView);

            } else {
                final int id = v.getId();
                if (id == R.id.btn_sharePhoto) {
                    mMsgHandler.removeMessages(MsgHandler.MSG_HIDE_CAPTURED_PHOTO_VIEW);
                    hideCapturedPhotoView(true);

                } else if (id == R.id.btn_stretchVideo) {
                    setVideoStretchedToFitFullscreenLayoutInternal(((Checkable) v).isChecked(), false);

                } else if (id == R.id.btn_loopSingleVideo) {
                    if (mVideoPlayer != null) {
                        mVideoPlayer.setSingleVideoLoopPlayback(((Checkable) v).isChecked());
                    }
                } else if (id == R.id.btn_allowAudioToPlayInBackground) {
                    if (mVideoPlayer != null) {
                        mVideoPlayer.setAudioAllowedToPlayInBackground(((Checkable) v).isChecked());
                    }
                } else if (id == R.id.text_whenThisEpisodeEnds) {
                    final boolean selected = !v.isSelected();
                    v.setSelected(selected);
                    mMoreView.findViewById(R.id.text_30Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_anHour).setSelected(false);
                    mMoreView.findViewById(R.id.text_90Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_2Hours).setSelected(false);

                    updateTimedOffSchedule(selected, -1);

                } else if (id == R.id.text_30Minutes) {
                    final boolean selected = !v.isSelected();
                    v.setSelected(selected);
                    mMoreView.findViewById(R.id.text_whenThisEpisodeEnds).setSelected(false);
                    mMoreView.findViewById(R.id.text_anHour).setSelected(false);
                    mMoreView.findViewById(R.id.text_90Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_2Hours).setSelected(false);

                    updateTimedOffSchedule(selected, TimedOffRunnable.OFF_TIME_30_MINUTES);

                } else if (id == R.id.text_anHour) {
                    final boolean selected = !v.isSelected();
                    v.setSelected(selected);
                    mMoreView.findViewById(R.id.text_whenThisEpisodeEnds).setSelected(false);
                    mMoreView.findViewById(R.id.text_30Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_90Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_2Hours).setSelected(false);

                    updateTimedOffSchedule(selected, TimedOffRunnable.OFF_TIME_AN_HOUR);

                } else if (id == R.id.text_90Minutes) {
                    final boolean selected = !v.isSelected();
                    v.setSelected(selected);
                    mMoreView.findViewById(R.id.text_whenThisEpisodeEnds).setSelected(false);
                    mMoreView.findViewById(R.id.text_30Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_anHour).setSelected(false);
                    mMoreView.findViewById(R.id.text_2Hours).setSelected(false);

                    updateTimedOffSchedule(selected, TimedOffRunnable.OFF_TIME_90_MINUTES);

                } else if (id == R.id.text_2Hours) {
                    final boolean selected = !v.isSelected();
                    v.setSelected(selected);
                    mMoreView.findViewById(R.id.text_whenThisEpisodeEnds).setSelected(false);
                    mMoreView.findViewById(R.id.text_30Minutes).setSelected(false);
                    mMoreView.findViewById(R.id.text_anHour).setSelected(false);
                    mMoreView.findViewById(R.id.text_90Minutes).setSelected(false);

                    updateTimedOffSchedule(selected, TimedOffRunnable.OFF_TIME_2_HOURS);

                } else if (id == R.id.text_mediaplayer) {
                    if (!v.isSelected()) {
                        VideoPlayer videoPlayer = VideoPlayer.Factory.newInstance(
                                SystemVideoPlayer.class, mContext);
                        if (videoPlayer != null) {
                            v.setSelected(true);
                            mMoreView.findViewById(R.id.text_exoplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_ijkplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_vlcplayer).setSelected(false);

                            videoPlayer.setVideoView(TextureVideoView.this);
                            setVideoPlayer(videoPlayer);
                        }
                    }
                } else if (id == R.id.text_exoplayer) {
                    if (!v.isSelected()) {
                        VideoPlayer videoPlayer = VideoPlayer.Factory.newInstance(
                                ExoVideoPlayer.class, mContext);
                        if (videoPlayer != null) {
                            v.setSelected(true);
                            mMoreView.findViewById(R.id.text_mediaplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_ijkplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_vlcplayer).setSelected(false);

                            videoPlayer.setVideoView(TextureVideoView.this);
                            setVideoPlayer(videoPlayer);
                        }
                    }
                } else if (id == R.id.text_ijkplayer) {
                    if (!v.isSelected()) {
                        VideoPlayer videoPlayer = VideoPlayer.Factory.newInstance(
                                IjkVideoPlayer.class, mContext);
                        if (videoPlayer != null) {
                            v.setSelected(true);
                            mMoreView.findViewById(R.id.text_mediaplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_exoplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_vlcplayer).setSelected(false);

                            videoPlayer.setVideoView(TextureVideoView.this);
                            setVideoPlayer(videoPlayer);
                        }
                    }
                } else if (id == R.id.text_vlcplayer) {
                    if (!v.isSelected()) {
                        VideoPlayer videoPlayer = VideoPlayer.Factory.newInstance(
                                VlcVideoPlayer.class, mContext);
                        if (videoPlayer != null) {
                            v.setSelected(true);
                            mMoreView.findViewById(R.id.text_mediaplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_exoplayer).setSelected(false);
                            mMoreView.findViewById(R.id.text_ijkplayer).setSelected(false);

                            videoPlayer.setVideoView(TextureVideoView.this);
                            setVideoPlayer(videoPlayer);
                        }
                    }
                }
            }
        }

        void updateTimedOffSchedule(boolean selected, int offTime) {
            switch (offTime) {
                case -1:
                    mPrivateFlags = mPrivateFlags & ~PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS
                            | (selected ? PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS : 0);
                    if (mTimedOffRunnable != null) {
                        removeCallbacks(mTimedOffRunnable);
                        mTimedOffRunnable = null;
                    }
                    break;
                case TimedOffRunnable.OFF_TIME_30_MINUTES:
                case TimedOffRunnable.OFF_TIME_AN_HOUR:
                case TimedOffRunnable.OFF_TIME_90_MINUTES:
                case TimedOffRunnable.OFF_TIME_2_HOURS:
                    mPrivateFlags &= ~PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS;
                    if (selected) {
                        if (mTimedOffRunnable == null) {
                            mTimedOffRunnable = new TimedOffRunnable();
                        } else {
                            removeCallbacks(mTimedOffRunnable);
                        }
                        mTimedOffRunnable.offTime = offTime;
                        postDelayed(mTimedOffRunnable, offTime);
                    } else {
                        if (mTimedOffRunnable != null) {
                            removeCallbacks(mTimedOffRunnable);
                            mTimedOffRunnable = null;
                        }
                    }
                    break;
            }
        }

        void showMoreView() {
            if (mMoreView != null) return;

            View view;
            mMoreView = view = LayoutInflater.from(mContext).inflate(
                    R.layout.drawer_view_more, mDrawerView, false);
            SwitchCompat svb = view.findViewById(R.id.btn_stretchVideo);
            SwitchCompat lsvb = view.findViewById(R.id.btn_loopSingleVideo);
            SwitchCompat aatpibb = view.findViewById(R.id.btn_allowAudioToPlayInBackground);
            TextView whenThisEpisodeEndsText = view.findViewById(R.id.text_whenThisEpisodeEnds);
            TextView _30MinutesText = view.findViewById(R.id.text_30Minutes);
            TextView anHourText = view.findViewById(R.id.text_anHour);
            TextView _90MinutesText = view.findViewById(R.id.text_90Minutes);
            TextView _2HoursText = view.findViewById(R.id.text_2Hours);
            TextView mediaplayerText = view.findViewById(R.id.text_mediaplayer);
            TextView exoplayerText = view.findViewById(R.id.text_exoplayer);
            TextView ijkplayerText = view.findViewById(R.id.text_ijkplayer);
            TextView vlcplayerText = view.findViewById(R.id.text_vlcplayer);

            IVideoPlayer videoPlayer = mVideoPlayer;
            TimedOffRunnable tor = mTimedOffRunnable;
            svb.setChecked(isVideoStretchedToFitFullscreenLayout());
            lsvb.setChecked(videoPlayer != null && videoPlayer.isSingleVideoLoopPlayback());
            aatpibb.setChecked(videoPlayer != null && videoPlayer.isAudioAllowedToPlayInBackground());
            whenThisEpisodeEndsText.setSelected((mPrivateFlags & PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS) != 0);
            _30MinutesText.setSelected(tor != null && tor.offTime == TimedOffRunnable.OFF_TIME_30_MINUTES);
            anHourText.setSelected(tor != null && tor.offTime == TimedOffRunnable.OFF_TIME_AN_HOUR);
            _90MinutesText.setSelected(tor != null && tor.offTime == TimedOffRunnable.OFF_TIME_90_MINUTES);
            _2HoursText.setSelected(tor != null && tor.offTime == TimedOffRunnable.OFF_TIME_2_HOURS);
            mediaplayerText.setSelected(videoPlayer instanceof SystemVideoPlayer);
            ijkplayerText.setSelected(videoPlayer instanceof IjkVideoPlayer);
            if (Utils.canUseExoPlayer()) {
                exoplayerText.setSelected(videoPlayer instanceof ExoVideoPlayer);
            } else {
                exoplayerText.setVisibility(View.GONE);
            }
            if (Utils.canUseVlcPlayer()) {
                vlcplayerText.setSelected(videoPlayer instanceof VlcVideoPlayer);
            } else {
                vlcplayerText.setVisibility(View.GONE);
            }
            // Scrolls to a proper horizontal position to make the selected text user-visible
            ViewGroup hsvc = (ViewGroup) anHourText.getParent();
            HorizontalScrollView hsv = (HorizontalScrollView) hsvc.getParent();
            if (_30MinutesText.isSelected()) {
                _30MinutesText.post(() -> {
                    final boolean rtl = Utils.isLayoutRtl(hsvc);
                    hsv.scrollTo(
                            rtl ? anHourText.getRight() : anHourText.getLeft() - hsv.getWidth(),
                            0);
                });
            } else if (anHourText.isSelected()) {
                anHourText.post(() -> {
                    final boolean rtl = Utils.isLayoutRtl(hsvc);
                    hsv.scrollTo(
                            rtl ? _90MinutesText.getRight() : _90MinutesText.getLeft() - hsv.getWidth(),
                            0);
                });
            } else if (_90MinutesText.isSelected()) {
                _90MinutesText.post(() -> {
                    final boolean rtl = Utils.isLayoutRtl(hsvc);
                    hsv.scrollTo(
                            rtl ? _2HoursText.getRight() : _2HoursText.getLeft() - hsv.getWidth(),
                            0);
                });
            } else if (_2HoursText.isSelected()) {
                _2HoursText.post(() -> {
                    final boolean rtl = Utils.isLayoutRtl(hsvc);
                    hsv.fullScroll(rtl ? FOCUS_LEFT : FOCUS_RIGHT);
                });
            }

            svb.setOnClickListener(this);
            lsvb.setOnClickListener(this);
            aatpibb.setOnClickListener(this);
            whenThisEpisodeEndsText.setOnClickListener(this);
            _30MinutesText.setOnClickListener(this);
            anHourText.setOnClickListener(this);
            _90MinutesText.setOnClickListener(this);
            _2HoursText.setOnClickListener(this);
            mediaplayerText.setOnClickListener(this);
            exoplayerText.setOnClickListener(this);
            ijkplayerText.setOnClickListener(this);
            vlcplayerText.setOnClickListener(this);

            view.setMinimumHeight(mDrawerViewMinimumHeight);
            mDrawerView.addView(view);
            openDrawer(mDrawerView);
        }
    }

    private final class OnVideoSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        OnVideoSeekBarChangeListener() {
        }

        int start;
        volatile int current;
        MediaMetadataRetriever mmr;
        AsyncTask<Void, Object, Void> task;
        ParcelableSpan progressTextSpan;
        ValueAnimator fadeAnimator;
        ValueAnimator translateAnimator;
        Animator.AnimatorListener animatorListener;
        static final int DURATION = 800; // ms

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                current = progress;
                if (mmr == null) {
                    mSeekingProgressDurationText.setText(getProgressDurationText(progress));
                    mSeekingProgress.setProgress(progress);
                }
                refreshVideoProgress(progress, false);

                if (translateAnimator == null) {
                    final View target = mmr == null ? mSeekingTextProgressFrame : mSeekingVideoThumbText;
                    final boolean rtl = Utils.isLayoutRtl(mContentView);
                    final float end = !rtl && progress > start || rtl && progress < start ?
                            mSeekingViewHorizontalOffset : -mSeekingViewHorizontalOffset;
                    ValueAnimator ta;
                    translateAnimator = ta = ValueAnimator.ofFloat(0, end);
                    ta.addListener(animatorListener);
                    ta.addUpdateListener(
                            animation -> target.setTranslationX((float) animation.getAnimatedValue()));
                    ta.setDuration(DURATION);
                    ta.setRepeatMode(ValueAnimator.RESTART);
                    ta.start();
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            current = start = seekBar.getProgress();

            mPrivateFlags |= PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
            showControls(-1, true);
            // Do not refresh the video progress bar with the current playback position
            // while the user is dragging it.
            mMsgHandler.removeMessages(MsgHandler.MSG_REFRESH_VIDEO_PROGRESS);

            Animator.AnimatorListener listener = animatorListener;
            ValueAnimator fa = fadeAnimator;
            if (fa != null) {
                // hide the currently showing view (mSeekingVideoThumbText/mSeekingTextProgressFrame)
                fa.end();
            }
            if (translateAnimator != null) {
                // Reset horizontal translation to 0 for the just hidden view
                translateAnimator.end();
            }
            animatorListener = listener; // Reuse the animator listener if it is not recycled
            // Decide which view to show
            if (isInFullscreenMode()) {
                Uri videoUri = mVideoPlayer == null ? null : mVideoPlayer.mVideoUri;
                if (videoUri != null && !URLUtils.isNetworkUrl(videoUri.toString())) {
                    mmr = new MediaMetadataRetriever();
                    try {
                        mmr.setDataSource(mContext, videoUri);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        mmr.release();
                        mmr = null;
                    }
                }
            }
            if (mmr != null) {
                // The media contains video content
                if (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null) {
                    task = new UpdateVideoThumbTask();
                    task.executeOnExecutor(ParallelThreadExecutor.getSingleton());
                    showSeekingVideoThumb(true);
                } else {
                    mmr.release();
                    mmr = null;
                    showSeekingTextProgress(true);
                }
            } else {
                showSeekingTextProgress(true);
            }
            // Start the fade in animation
            if (fa == null) {
                if (animatorListener == null) {
                    animatorListener = new AnimatorListenerAdapter() {
                        // Override for compatibility with APIs below 26.
                        // This will not get called on platforms O and higher.
                        @Override
                        public void onAnimationStart(Animator animation) {
                            onAnimationStart(animation, isReverse(animation));
                        }

                        // Override for compatibility with APIs below 26.
                        // This will not get called on platforms O and higher.
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            onAnimationEnd(animation, isReverse(animation));
                        }

                        boolean isReverse(Animator animation) {
                            // When reversing, the animation's repeat mode was set to REVERSE
                            // before it started.
                            return ((ValueAnimator) animation).getRepeatMode() == ValueAnimator.REVERSE;
                        }

                        @TargetApi(Build.VERSION_CODES.O)
                        @Override
                        public void onAnimationStart(Animator animation, boolean isReverse) {
                            final boolean isThumbVisible = mSeekingVideoThumbText.getVisibility() == VISIBLE;
                            final boolean isFadeAnimation = animation == fadeAnimator;

                            Animator other = isFadeAnimation ? translateAnimator : fadeAnimator;
                            if (other == null || !other.isRunning()) {
                                updateLayer(LAYER_TYPE_HARDWARE, isThumbVisible);
                            }

                            if (isFadeAnimation) {
                                animation.setDuration(isReverse || isThumbVisible ?
                                        DURATION : (long) (DURATION * 2f / 3f + 0.5f));
                            }
                        }

                        @TargetApi(Build.VERSION_CODES.O)
                        @Override
                        public void onAnimationEnd(Animator animation, boolean isReverse) {
                            final boolean isThumbVisible = mSeekingVideoThumbText.getVisibility() == VISIBLE;
                            final boolean isFadeAnimation = animation == fadeAnimator;

                            Animator other = isFadeAnimation ? translateAnimator : fadeAnimator;
                            if (other == null || !other.isRunning()) {
                                updateLayer(LAYER_TYPE_NONE, isThumbVisible);
                            }

                            if (isReverse) {
                                if (isFadeAnimation) {
                                    fadeAnimator = null;
                                } else {
                                    translateAnimator = null;
                                }
                                if (fadeAnimator == null && translateAnimator == null) {
                                    animatorListener = null;
                                    if (isThumbVisible) {
                                        recycleVideoThumb();
                                        // Clear the text to make sure it doesn't show anything
                                        // the next time it appears, otherwise a separate text
                                        // would be displayed on it, which we do not want.
                                        mSeekingVideoThumbText.setText("");
                                        showSeekingVideoThumb(false);
                                    } else {
                                        showSeekingTextProgress(false);
                                    }
                                }
                            }
                        }

                        void updateLayer(int layerType, boolean isThumbVisible) {
                            //noinspection StatementWithEmptyBody
                            if (isThumbVisible) {
                                mSeekingVideoThumbText.setLayerType(layerType, null);
                                if (ViewCompat.isAttachedToWindow(mSeekingVideoThumbText)) {
                                    mSeekingVideoThumbText.buildLayer();
                                }
                                mScrimView.setLayerType(layerType, null);
                                if (ViewCompat.isAttachedToWindow(mScrimView)) {
                                    mScrimView.buildLayer();
                                }
                            } else {
//                                mSeekingTextProgressFrame.setLayerType(layerType, null);
//                                if (ViewCompat.isAttachedToWindow(mSeekingTextProgressFrame)) {
//                                    mSeekingTextProgressFrame.buildLayer();
//                                }
                            }
                        }
                    };
                }
                fadeAnimator = fa = ValueAnimator.ofFloat(0.0f, 1.0f);
                fa.addListener(animatorListener);
                fa.addUpdateListener(animation -> {
                    final float alpha = (float) animation.getAnimatedValue();
                    if (mSeekingVideoThumbText.getVisibility() == VISIBLE) {
                        mScrimView.setAlpha(alpha);
                        mSeekingVideoThumbText.setAlpha(alpha);
                    } else {
                        mSeekingTextProgressFrame.setAlpha(alpha);
                    }
                });
            } else {
                // If the fade in/out animator has not been released before we need one again,
                // reuse it to avoid unnecessary memory re-allocations.
                fadeAnimator = fa;
            }
            fa.setRepeatMode(ValueAnimator.RESTART);
            fa.start();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Avoids being called again in the cancelDraggingVideoSeekBar() method
            seekBar.setPressed(false);

            if ((mPrivateFlags & PFLAG_DISALLOW_PLAYBACK_POSITION_SEEK_ON_STOP_TRACKING_TOUCH) == 0
                    && mVideoPlayer != null) {
                final int progress = current;
                if (progress != start)
                    mVideoPlayer.seekTo(progress, true);
            }

            mPrivateFlags &= ~PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS;
            showControls(true, false);

            if (mmr != null) {
                task.cancel(false);
                task = null;
                mmr.release();
                mmr = null;
            }
            if (translateAnimator != null) {
                translateAnimator.setRepeatMode(ValueAnimator.REVERSE);
                translateAnimator.reverse();
            }
            fadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
            fadeAnimator.reverse();
        }

        void showSeekingVideoThumb(boolean show) {
            if (show) {
                mScrimView.setVisibility(VISIBLE);
                mSeekingVideoThumbText.setVisibility(VISIBLE);
            } else {
                mScrimView.setVisibility(GONE);
                mSeekingVideoThumbText.setVisibility(GONE);
            }
        }

        void showSeekingTextProgress(boolean show) {
            if (show) {
                final int progress, duration;
                if (mVideoPlayer == null) {
                    duration = progress = 0;
                } else {
                    progress = current;
                    duration = mVideoPlayer.getNoNegativeVideoDuration();
                }
                mSeekingProgressDurationText.setText(getProgressDurationText(progress));
                mSeekingProgress.setMax(duration);
                mSeekingProgress.setProgress(progress);
                mSeekingTextProgressFrame.setVisibility(VISIBLE);
            } else {
                mSeekingTextProgressFrame.setVisibility(GONE);
            }
        }

        CharSequence getProgressDurationText(int progress) {
            if (progressTextSpan == null) {
                progressTextSpan = new ForegroundColorSpan(mColorAccent);
            }
            final String vds;
            if (mVideoPlayer == null) {
                progress = 0;
                vds = VideoPlayer.DEFAULT_STRING_VIDEO_DURATION;
            } else {
                vds = mVideoPlayer.mVideoDurationString;
            }
            final String ps = TimeUtil.formatTimeByColon(progress);
            final SpannableString ss = new SpannableString(
                    mResources.getString(R.string.progress_duration, ps, vds));
            ss.setSpan(progressTextSpan, 0, ps.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return ss;
        }

        void recycleVideoThumb() {
            Drawable thumb = mSeekingVideoThumbText.getCompoundDrawables()[3];
            // Removes the drawable that holds a reference to the bitmap to be recycled,
            // in case we still use the recycled bitmap on the next drawing of the TextView.
            mSeekingVideoThumbText.setCompoundDrawables(null, null, null, null);
            if (thumb instanceof BitmapDrawable) {
                ((BitmapDrawable) thumb).getBitmap().recycle();
            }
        }

        @SuppressLint("StaticFieldLeak")
        final class UpdateVideoThumbTask extends AsyncTask<Void, Object, Void> {
            static final boolean RETRIEVE_SCALED_FRAME_FROM_MMR = true;
            static final float RATIO = 0.25f;
            int last = -1;

            @Override
            public Void doInBackground(Void... voids) {
                while (!isCancelled()) {
                    int now = current;
                    if (now == last) continue;
                    last = now;

                    View tv = mTextureView;
                    final int width = (int) (tv.getWidth()/* * tv.getScaleX()*/ * RATIO + 0.5f);
                    final int height = (int) (tv.getHeight()/* * tv.getScaleY()*/ * RATIO + 0.5f);

                    Bitmap thumb = null;
                    if (RETRIEVE_SCALED_FRAME_FROM_MMR
                            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        thumb = mmr.getScaledFrameAtTime(now * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC, width, height);
                    } else {
                        Bitmap tmp = mmr.getFrameAtTime(now * 1000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if (tmp != null) {
                            thumb = BitmapUtils.createScaledBitmap(tmp, width, height, true);
                        }
                    }
                    if (thumb == null) continue;
                    thumb = BitmapUtils.createRoundCornerBitmap(
                            thumb, mSeekingVideoThumbCornerRadius, true);

                    publishProgress(getProgressDurationText(now), new BitmapDrawable(mResources, thumb));
                }
                return null;
            }

            @Override
            public void onProgressUpdate(Object... objs) {
                recycleVideoThumb();
                mSeekingVideoThumbText.setText((CharSequence) objs[0]);
                mSeekingVideoThumbText.setCompoundDrawablesWithIntrinsicBounds(
                        null, null, null, (Drawable) objs[1]);
            }
        }
    }

    private static final class MsgHandler extends Handler {
        static final int MSG_HIDE_CONTROLS = 1;
        static final int MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME = 2;
        static final int MSG_HIDE_CAPTURED_PHOTO_VIEW = 3;
        static final int MSG_CHECK_CAMERA_BUTTONS_VISIBILITIES = 4;
        static final int MSG_REFRESH_VIDEO_PROGRESS = 5;

        final WeakReference<TextureVideoView> videoViewRef;

        MsgHandler(TextureVideoView videoView) {
            videoViewRef = new WeakReference<>(videoView);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            TextureVideoView videoView = videoViewRef.get();
            if (videoView == null) return;
            VideoPlayer videoPlayer = videoView.mVideoPlayer;

            switch (msg.what) {
                case MSG_HIDE_CONTROLS:
                    videoView.showControls(false);
                    break;
                case MSG_HIDE_BRIGHTNESS_OR_VOLUME_FRAME:
                    videoView.mBrightnessOrVolumeFrame.setVisibility(GONE);
                    break;
                case MSG_HIDE_CAPTURED_PHOTO_VIEW:
                    videoView.hideCapturedPhotoView(false);
                    break;
                case MSG_CHECK_CAMERA_BUTTONS_VISIBILITIES:
                    videoView.checkCameraButtonsVisibilities();
                    break;
                case MSG_REFRESH_VIDEO_PROGRESS: {
                    if (videoPlayer == null) {
                        videoView.refreshVideoProgress(0);
                        break;
                    }

                    final int progress = videoPlayer.getVideoProgress();
                    if (videoView.isControlsShowing() && videoPlayer.isPlaying()) {
                        // Dynamic delay to keep pace with the actual progress of the video most accurately.
                        sendEmptyMessageDelayed(MSG_REFRESH_VIDEO_PROGRESS, 1000 - progress % 1000);
                    }
                    videoView.refreshVideoProgress(progress);
                }
                break;

                case BackgroundPlaybackControllerService.MSG_PLAY:
                    if (videoPlayer != null) {
                        videoPlayer.play(true);
                    }
                    break;
                case BackgroundPlaybackControllerService.MSG_PAUSE:
                    if (videoPlayer != null) {
                        videoPlayer.pause(true);
                    }
                    break;
                case BackgroundPlaybackControllerService.MSG_SKIP_TO_PREVIOUS:
                    if (videoPlayer != null) {
                        videoPlayer.skipToPreviousIfPossible();
                    }
                    break;
                case BackgroundPlaybackControllerService.MSG_SKIP_TO_NEXT:
                    if (videoPlayer != null) {
                        videoPlayer.skipToNextIfPossible();
                    }
                    break;
                case BackgroundPlaybackControllerService.MSG_CLOSE:
                    videoView.tryStopBackgroundPlaybackControllerService();
                    if (videoView.mEventListener != null) {
                        videoView.mEventListener.onBackgroundPlaybackControllerClose();
                    }
                    break;
            }
        }
    }

    private final class TimedOffRunnable implements Runnable {
        int offTime;
        static final int OFF_TIME_30_MINUTES = 30 * 60 * 1000; // ms
        static final int OFF_TIME_AN_HOUR = 2 * OFF_TIME_30_MINUTES;
        static final int OFF_TIME_90_MINUTES = 3 * OFF_TIME_30_MINUTES;
        static final int OFF_TIME_2_HOURS = 4 * OFF_TIME_30_MINUTES;

        TimedOffRunnable() {
        }

        @Override
        public void run() {
            mTimedOffRunnable = null;
            if (mMoreView != null) {
                switch (offTime) {
                    case OFF_TIME_30_MINUTES:
                        mMoreView.findViewById(R.id.text_30Minutes).setSelected(false);
                        break;
                    case OFF_TIME_AN_HOUR:
                        mMoreView.findViewById(R.id.text_anHour).setSelected(false);
                        break;
                    case OFF_TIME_90_MINUTES:
                        mMoreView.findViewById(R.id.text_90Minutes).setSelected(false);
                        break;
                    case OFF_TIME_2_HOURS:
                        mMoreView.findViewById(R.id.text_2Hours).setSelected(false);
                        break;
                }
            }
            if (mVideoPlayer != null) {
                mVideoPlayer.closeVideoInternal(true);
            }
        }
    }

    private static final class BackgroundPlaybackControllerServiceConn implements ServiceConnection {
        BackgroundPlaybackControllerService.Proxy service;

        TextureVideoView serviceHolder;

        BackgroundPlaybackControllerServiceConn() {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            this.service = (BackgroundPlaybackControllerService.Proxy) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }

        boolean isServiceAccessibleFrom(TextureVideoView videoView) {
            return serviceHolder == videoView;
        }

        void connectFor(TextureVideoView videoView) {
            if (serviceHolder == videoView) {
                // Do nothing when the service is already connected for the videoView
                return;
            } else {
                if (serviceHolder != null) {
                    // Disconnect the running service for the existing holder when we are choosing
                    // another TextureVideoView object as the new service's holder
                    disconnectFor(serviceHolder);
                }
                serviceHolder = videoView;
            }

            VideoPlayer vp = videoView.mVideoPlayer;
            Intent it = new Intent(videoView.mContext, BackgroundPlaybackControllerService.class)
                    .putExtra(InternalConsts.EXTRA_MESSENGER, new Messenger(videoView.mMsgHandler))
                    .putExtra(InternalConsts.EXTRA_MEDIA_URI, vp == null ? null : vp.mVideoUri)
                    .putExtra(InternalConsts.EXTRA_MEDIA_TITLE, videoView.mTitle)
                    .putExtra(InternalConsts.EXTRA_IS_PLAYING, vp != null && vp.isPlaying())
                    .putExtra(InternalConsts.EXTRA_IS_BUFFERING,
                            vp != null && (vp.mInternalFlags & VideoPlayer.$FLAG_VIDEO_IS_BUFFERING) != 0)
                    .putExtra(InternalConsts.EXTRA_CAN_SKIP_TO_PREVIOUS, videoView.canSkipToPrevious())
                    .putExtra(InternalConsts.EXTRA_CAN_SKIP_TO_NEXT, videoView.canSkipToNext())
                    .putExtra(InternalConsts.EXTRA_MEDIA_PROGRESS,
                            vp == null ? 0L : (long) vp.getVideoProgress())
                    .putExtra(InternalConsts.EXTRA_MEDIA_DURATION,
                            vp == null ? 0L : (long) vp.getNoNegativeVideoDuration());
            if (videoView.mOpCallback != null) {
                Class<? extends Activity> hostActivityClass = videoView.mOpCallback.getHostActivityClass();
                if (hostActivityClass != null) {
                    it.putExtra(InternalConsts.EXTRA_PLAYBACK_ACTIVITY_CLASS, hostActivityClass);
                }
            }
            videoView.mContext.bindService(it, this, Context.BIND_AUTO_CREATE);
        }

        boolean disconnectFor(TextureVideoView videoView) {
            if (service != null) {
                // Only the holder has access to disconnecting the running service
                if (videoView == serviceHolder) {
                    videoView.mContext.unbindService(this);
                    service = null;
                    serviceHolder = null;
                    return true;
                }
            }
            return false;
        }
    }

    private boolean canAccessBackgroundPlaybackControllerService() {
        return sBgPlaybackControllerServiceConn != null
                && sBgPlaybackControllerServiceConn.service != null
                && sBgPlaybackControllerServiceConn.isServiceAccessibleFrom(this);
    }

    private void startBackgroundPlaybackControllerService() {
        if (sBgPlaybackControllerServiceConn == null) {
            sBgPlaybackControllerServiceConn = new BackgroundPlaybackControllerServiceConn();
        }
        sBgPlaybackControllerServiceConn.connectFor(this);
    }

    @Synthetic void tryStopBackgroundPlaybackControllerService() {
        if (sBgPlaybackControllerServiceConn != null) {
            if (sBgPlaybackControllerServiceConn.disconnectFor(this)) {
                sBgPlaybackControllerServiceConn = null;
            }
        }
    }

    // --------------- package-private overridden super methods ------------------------

    @Nullable
    @Override
    Surface getSurface() {
        return mSurface;
    }

    @Override
    void onVideoUriChanged(@Nullable Uri uri) {
        showTextureView(false);

        if (canAccessBackgroundPlaybackControllerService()) {
            sBgPlaybackControllerServiceConn.service.onMediaUriChange(uri);
        }
    }

    @Override
    void onVideoDurationChanged(int duration) {
        checkButtonsAbilities();

        if (canAccessBackgroundPlaybackControllerService()) {
            //noinspection ConstantConditions
            sBgPlaybackControllerServiceConn.service
                    .onMediaDurationChanged(mVideoPlayer.getVideoProgress(), duration);
        }
    }

    @Override
    void onVideoSizeChanged(int width, int height) {
        checkButtonsAbilities();
        if (width != 0 && height != 0) {
            requestLayout();
        }
    }

    @Override
    void onVideoStarted() {
        showTextureView(true);

        setKeepScreenOn(true);
        adjustToggleState(true);
        if (mViewMode != VIEW_MODE_MINIMUM) {
            if ((mPrivateFlags & PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS) != 0) {
                // If the PFLAG_IGNORE_SHOW_CONTROLS_METHOD_CALLS flag is marked into mPrivateFlags,
                // Calling showControls(true) is meaningless as this flag is a hindrance for
                // the subsequent program in that method to continue.
                // So we need resend MSG_REFRESH_VIDEO_PROGRESS to make sure the video seek bar
                // to be updated as the video plays.
                mMsgHandler.removeMessages(MsgHandler.MSG_REFRESH_VIDEO_PROGRESS);
                mMsgHandler.sendEmptyMessage(MsgHandler.MSG_REFRESH_VIDEO_PROGRESS);
            } else {
                showControls(true);
            }
        }

        if (canAccessBackgroundPlaybackControllerService()) {
            //noinspection ConstantConditions
            sBgPlaybackControllerServiceConn.service.onMediaPlay(mVideoPlayer.getVideoProgress());
        }
    }

    @Override
    void onVideoStopped() {
        setKeepScreenOn(false);
        adjustToggleState(false);
        if (mViewMode != VIEW_MODE_MINIMUM) {
            showControls(true);
        }

        if (canAccessBackgroundPlaybackControllerService()) {
            //noinspection ConstantConditions
            sBgPlaybackControllerServiceConn.service.onMediaPause(mVideoPlayer.getVideoProgress());
        }
    }

    @Override
    void onVideoRepeat() {
        if (canAccessBackgroundPlaybackControllerService()) {
            sBgPlaybackControllerServiceConn.service.onMediaRepeat();
        }
    }

    @Override
    void onVideoBufferingStateChanged(boolean buffering) {
        showLoadingView(buffering);

        if (canAccessBackgroundPlaybackControllerService()) {
            //noinspection ConstantConditions
            sBgPlaybackControllerServiceConn.service
                    .onMediaBufferingStateChanged(buffering, mVideoPlayer.getVideoProgress());
        }
    }

    @Override
    void onPlaybackSpeedChanged(float speed) {
        if (mSpeedSpinner != null) {
            mSpeedSpinner.setSelection(indexOfPlaybackSpeed(speed), true);
        }
    }

    @Override
    void onAudioAllowedToPlayInBackgroundChanged(boolean allowed) {
        if (mMoreView != null) {
            Checkable toggle = mMoreView.findViewById(R.id.btn_allowAudioToPlayInBackground);
            if (allowed != toggle.isChecked()) {
                toggle.setChecked(allowed);
            }
        }
        if (allowed) {
            startBackgroundPlaybackControllerService();
        } else {
            tryStopBackgroundPlaybackControllerService();
        }
    }

    @Override
    void onSingleVideoLoopPlaybackModeChanged(boolean looping) {
        if (mMoreView != null) {
            Checkable toggle = mMoreView.findViewById(R.id.btn_loopSingleVideo);
            if (looping != toggle.isChecked()) {
                toggle.setChecked(looping);
            }
        }
    }

    @Override
    boolean willTurnOffWhenThisEpisodeEnds() {
        return (mPrivateFlags & PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS) != 0;
    }

    @Override
    void onVideoTurnedOffWhenTheEpisodeEnds() {
        mPrivateFlags &= ~PFLAG_TURN_OFF_WHEN_THIS_EPISODE_ENDS;
        if (mMoreView != null) {
            mMoreView.findViewById(R.id.text_whenThisEpisodeEnds).setSelected(false);
        }
    }
}