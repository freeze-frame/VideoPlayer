/*
 * Created on 2018/04/11.
 * Copyright © 2018–2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.view.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.widget.TextViewCompat;

import com.bumptech.glide.util.Synthetic;
import com.google.android.material.snackbar.Snackbar;
import com.liuzhenlin.floatingmenu.DensityUtils;
import com.liuzhenlin.galleryviewer.GalleryViewPager;
import com.liuzhenlin.simrv.Utils;
import com.liuzhenlin.swipeback.SwipeBackActivity;
import com.liuzhenlin.swipeback.SwipeBackLayout;
import com.liuzhenlin.texturevideoview.utils.FileUtils;
import com.liuzhenlin.texturevideoview.utils.SystemBarUtils;
import com.liuzhenlin.videos.App;
import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.R;
import com.liuzhenlin.videos.observer.OnOrientationChangeListener;
import com.liuzhenlin.videos.observer.RotationObserver;
import com.liuzhenlin.videos.observer.ScreenNotchSwitchObserver;
import com.liuzhenlin.videos.presenter.IFeedbackPresenter;
import com.liuzhenlin.videos.utils.DisplayCutoutUtils;
import com.liuzhenlin.videos.utils.OSHelper;
import com.liuzhenlin.videos.utils.UiUtils;
import com.liuzhenlin.videos.view.adapter.GalleryPagerAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

/**
 * @author 刘振林
 */
public class FeedbackActivity extends SwipeBackActivity implements IFeedbackView, View.OnClickListener {

    @Synthetic EditText mEnterProblemsOrAdviceEditor;
    @Synthetic TextView mWordCountIndicator;
    private TextView mPictureCountIndicator;
    private EditText mEnterContactWayEditor;
    @Synthetic Button mCommitButton;

    private Dialog mConfirmSaveDataDialog;
    @Synthetic Dialog mPicturePreviewDialog;

    @Synthetic boolean mShouldSaveDataOnDestroy;

    @Synthetic final IFeedbackPresenter mPresenter = IFeedbackPresenter.newInstance();

    @Nullable
    @Override
    public Activity getPreviousActivity() {
        return MainActivity.this$;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SystemBarUtils.setTransparentStatus(window);
                SystemBarUtils.setLightStatus(window, true);
                // MIUI6...
            } else {
                final boolean isMIUI6Later = OSHelper.getMiuiVersion() >= 6;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                        && isMIUI6Later) {
                    SystemBarUtils.setTransparentStatus(window);
                    SystemBarUtils.setLightStatusForMIUI(window, true);
                } else if (isMIUI6Later) {
                    SystemBarUtils.setTranslucentStatus(window, true);
                    SystemBarUtils.setLightStatusForMIUI(window, true);
                    // FlyMe4...
                } else {
                    final boolean isFlyMe4Later = OSHelper.isFlyme4OrLater();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            && isFlyMe4Later) {
                        SystemBarUtils.setTransparentStatus(window);
                        SystemBarUtils.setLightStatusForFlyme(window, true);
                    } else if (isFlyMe4Later) {
                        SystemBarUtils.setTranslucentStatus(window, true);
                        SystemBarUtils.setLightStatusForFlyme(window, true);
                        // Other Systems
                    } else {
                        SystemBarUtils.setTranslucentStatus(window, true);
                    }
                }
            }
        }

        TextView backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(this);

        TextView saveButton = findViewById(R.id.btn_saveFeedback);
        saveButton.setOnClickListener(this);

        mEnterProblemsOrAdviceEditor = findViewById(R.id.editor_enterProblemsOrAdvice);
        mWordCountIndicator = findViewById(R.id.text_wordCountIndicator);
        mPictureCountIndicator = findViewById(R.id.text_pictureCountIndicator);
        mEnterContactWayEditor = findViewById(R.id.editor_enterContactWay);
        mCommitButton = findViewById(R.id.btn_commit);

        mEnterProblemsOrAdviceEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String text = mEnterProblemsOrAdviceEditor.getText().toString();
                mWordCountIndicator.setText(getString(R.string.wordCount, text.length()));
                mCommitButton.setEnabled(text.trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEnterProblemsOrAdviceEditor.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    EditText editText = (EditText) v;
                    // 当触摸的是EditText且当前EditText可上下滚动时，不允许父布局ScrollView拦截事件
                    if (editText.getLineCount() > TextViewCompat.getMaxLines(editText)) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });

        mPresenter.attachToView(this);

        BaseAdapter adapter = mPresenter.getPictureGridAdapter();
        GridView gridView = findViewById(R.id.grid_pictures);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener((AdapterView.OnItemClickListener) adapter);

        mCommitButton.setOnClickListener(this);

        // 恢复上次退出此页面时保存的数据
        if (savedInstanceState == null) {
            mPresenter.restoreData(null);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        final Window window = getWindow();
        if (Utils.isLayoutRtl(window.getDecorView()))
            getSwipeBackLayout().setEnabledEdges(SwipeBackLayout.EDGE_RIGHT);
        getSwipeBackLayout().addSwipeListener(new SwipeBackLayout.SwipeListener() {
            int oldState = SwipeBackLayout.STATE_IDLE;

            @Override
            public void onScrollStateChange(int edge, int state) {
                if (oldState == SwipeBackLayout.STATE_IDLE && state != SwipeBackLayout.STATE_IDLE) {
                    UiUtils.hideSoftInput(window);
                }
                oldState = state;
            }

            @Override
            public void onScrollPercentChange(int edge, float percent) {
                mShouldSaveDataOnDestroy = percent == 1f;
            }
        });
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPresenter.restoreData(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mPresenter.saveData(outState,
                mEnterProblemsOrAdviceEditor.getText().toString(),
                mEnterContactWayEditor.getText().toString().trim());
    }

    @Override
    public void onBackPressed() {
        final String text = mEnterProblemsOrAdviceEditor.getText().toString();
        final String contactWay = mEnterContactWayEditor.getText().toString().trim();
        if (mPresenter.hasDataChanged(text, contactWay)) {
            View view = View.inflate(this, R.layout.dialog_confirm_save, null);
            view.findViewById(R.id.btn_notSave).setOnClickListener(this);
            view.findViewById(R.id.btn_save).setOnClickListener(this);

            mConfirmSaveDataDialog = new AppCompatDialog(this, R.style.DialogStyle_MinWidth_NoTitle);
            mConfirmSaveDataDialog.setContentView(view);
            mConfirmSaveDataDialog.setCancelable(false);
            mConfirmSaveDataDialog.setCanceledOnTouchOutside(false);
            mConfirmSaveDataDialog.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 滑动返回时默认保存数据
        if (mShouldSaveDataOnDestroy) {
            saveUserFilledData(true);
        }
        if (mPicturePreviewDialog != null) {
            // 当Activity被系统杀掉时，Dialog的onDismiss()不会被调用。
            // 在此手动调用以确保屏幕方向监听和观察刘海屏开关打开/关闭的ContentObserver能被暂停
            mPicturePreviewDialog.dismiss();
        }
        // 回收Bitmaps
        mPresenter.recyclePictures();
        mPresenter.detachFromView(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && requestCode == Consts.REQUEST_CODE_GET_PICTURE) {
            Uri uri = data.getData();
            if (uri != null)
                mPresenter.addPicture(FileUtils.UriResolver.getPath(this, uri));
        }
    }

    @Override
    public void pickPicture() {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                Consts.REQUEST_CODE_GET_PICTURE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_saveFeedback:
                saveUserFilledData(true);
                finish();
                break;

            case R.id.btn_commit:
                final String text = mEnterProblemsOrAdviceEditor.getText().toString();
                final String contactWay = mEnterContactWayEditor.getText().toString().trim();
                mPresenter.sendFeedback(text, contactWay);
                break;

            case R.id.btn_save:
                saveUserFilledData(true);
            case R.id.btn_notSave:
                mConfirmSaveDataDialog.cancel();
                finish();
                break;
        }
    }

    private void saveUserFilledData(@SuppressWarnings("SameParameterValue") boolean toastResultIfSaved) {
        final String text = mEnterProblemsOrAdviceEditor.getText().toString();
        final String contactWay = mEnterContactWayEditor.getText().toString().trim();
        mPresenter.persistentlySaveUserFilledData(text, contactWay, toastResultIfSaved);
    }

    @Override
    public void toastResultOnUserFilledDataSaved() {
        Activity preActivity = getPreviousActivity();
        if (preActivity == null) {
            showToast(this, R.string.saveSuccessful, Toast.LENGTH_SHORT);
        } else {
            UiUtils.showUserCancelableSnackbar(preActivity.getWindow().getDecorView(),
                    R.string.saveSuccessful, Snackbar.LENGTH_SHORT);
        }
    }

    @Override
    public void refreshCurrTexts(@NonNull String text, @NonNull String contactWay) {
        mEnterProblemsOrAdviceEditor.setText(text);
        mEnterProblemsOrAdviceEditor.setSelection(text.length());
        mEnterContactWayEditor.setText(contactWay);
    }

    @NonNull
    @Override
    public IFeedbackView.PictureGridViewHolder getPictureGridViewHolder(
            int position, @Nullable View convertView, @NonNull ViewGroup parent,
            @NonNull Bitmap picture, int pictureCount) {
        PictureGridViewHolder vh;
        if (convertView == null) {
            vh = new PictureGridViewHolder(parent);
            convertView = vh.itemView;
            convertView.setTag(vh);

            final int screenWidth = App.getInstance(this).getScreenWidthIgnoreOrientation();
            final int dp_20 = DensityUtils.dp2px(this, 20f);

            ViewGroup.LayoutParams lp = convertView.getLayoutParams();
            lp.height = lp.width = com.liuzhenlin.texturevideoview.utils.
                    Utils.roundFloat((screenWidth - dp_20 * 1.5f) / 3f);

            ViewGroup.LayoutParams plp = parent.getLayoutParams();
            plp.width = screenWidth - dp_20;
            plp.height = lp.height;
        } else {
            vh = (PictureGridViewHolder) convertView.getTag();
        }
        vh.pictureImage.setImageBitmap(picture);
        vh.pictureText.setVisibility(position != pictureCount - 1 ? View.GONE : View.VISIBLE);
        mPictureCountIndicator.setText(getString(R.string.pictureCount, pictureCount - 1));
        return vh;
    }

    private static final class PictureGridViewHolder extends IFeedbackView.PictureGridViewHolder {
        final ImageView pictureImage;
        final TextView pictureText;

        PictureGridViewHolder(ViewGroup adapterView) {
            super(LayoutInflater.from(adapterView.getContext())
                    .inflate(R.layout.item_picture_grid, adapterView, false));
            pictureImage = itemView.findViewById(R.id.image_picture);
            pictureText = itemView.findViewById(R.id.text_picture);
        }
    }

    @Override
    public void showPicturePreviewDialog(@NonNull List<Bitmap> pictures, int currentItem) {
        if (mPicturePreviewDialog == null) {
            mPicturePreviewDialog = new PicturePreviewDialog(pictures, currentItem);
            mPicturePreviewDialog.show();
        }
    }

    @Override
    public void hidePicturePreviewDialog() {
        if (mPicturePreviewDialog != null) {
            mPicturePreviewDialog.dismiss();
        }
    }

    private final class PicturePreviewDialog extends Dialog implements DialogInterface.OnDismissListener,
            View.OnClickListener, View.OnLongClickListener {
        final Context mContext = FeedbackActivity.this;
        final Window mParentWindow;
        final Window mWindow;

        final GalleryViewPager mGalleryViewPager;
        final GalleryPagerAdapter<ImageView> mGalleryPagerAdapter;
        final FrameLayout mDeleteFrame;

        Dialog mConfirmDeletePictureDialog;

        RotationObserver mRotationObserver;
        OnOrientationChangeListener mOnOrientationChangeListener;
        boolean mIsRotationEnabled;
        int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;

        boolean mIsNotchSupport;
        boolean mIsNotchSupportOnMIUI;
        boolean mIsNotchSupportOnEMUI;
        boolean mIsNotchHidden;
        int mNotchHeight;
        ScreenNotchSwitchObserver mNotchSwitchObserver;

        PicturePreviewDialog(List<Bitmap> pictures, int currentItem) {
            super(FeedbackActivity.this, R.style.DialogStyle_PicturePreview);
            setOnDismissListener(this);

            mParentWindow = FeedbackActivity.this.getWindow();
            mWindow = getWindow();

            //noinspection ConstantConditions
            View view = View.inflate(mContext,
                    R.layout.dialog_picture_preview,
                    mWindow.getDecorView().findViewById(Window.ID_ANDROID_CONTENT));

            int pictureCount = pictures.size();
            List<ImageView> images = new ArrayList<>(pictureCount);
            for (int i = 0; i < pictureCount; i++) {
                ImageView image = (ImageView) View.inflate(
                        mContext, R.layout.item_gallery_view_pager, null);
                image.setImageBitmap(pictures.get(i));
                image.setOnClickListener(this);
                image.setOnLongClickListener(this);
                images.add(image);
            }
            mGalleryPagerAdapter = new GalleryPagerAdapter<>(images);
            mGalleryViewPager = view.findViewById(R.id.galley_view_pager);
            mGalleryViewPager.setAdapter(mGalleryPagerAdapter);
            mGalleryViewPager.setItemCallback(mGalleryPagerAdapter);
            mGalleryViewPager.setCurrentItem(currentItem);
            mGalleryViewPager.setPageMargin(DensityUtils.dp2px(mContext, 25f));

            mDeleteFrame = view.findViewById(R.id.frame_btn_delete);
            mDeleteFrame.setOnClickListener(this);
            view.findViewById(R.id.btn_delete).setOnClickListener(this);

            mWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }

        @Override
        public void show() {
            if (isShowing()) {
                return;
            }

            setLayoutInDisplayCutout();
            mWindow.getDecorView().setOnSystemUiVisibilityChangeListener(
                    visibility -> {
                        // FIXME: 对于Dialog, 在API24（Android 7.0）及以下不起作用
                        SystemBarUtils.showSystemBars(mWindow, false);
                    });
            super.show();

            mRotationObserver = new RotationObserver(
                    mParentWindow.getDecorView().getHandler(), mContext) {
                @Override
                public void onRotationChange(boolean selfChange, boolean enabled) {
                    mIsRotationEnabled = enabled;
                }
            };
            mOnOrientationChangeListener = new OnOrientationChangeListener(
                    mContext, mScreenOrientation) {
                @Override
                public void onOrientationChange(int orientation) {
                    if (orientation != SCREEN_ORIENTATION_REVERSE_PORTRAIT) {
                        if (!mIsRotationEnabled &&
                                !(orientation != SCREEN_ORIENTATION_PORTRAIT
                                        && mScreenOrientation != SCREEN_ORIENTATION_PORTRAIT)) {
                            setOrientation(mScreenOrientation);
                            return;
                        }
                        mScreenOrientation = orientation;
                        setRequestedOrientation(orientation);
                    }
                }
            };
            mRotationObserver.startObserver();
            mOnOrientationChangeListener.setEnabled(true);
        }

        @SuppressLint("SourceLockedOrientationActivity")
        @Override
        public void onDismiss(DialogInterface dialog) {
            mPicturePreviewDialog = null;
            if (mNotchSwitchObserver != null) {
                mNotchSwitchObserver.stopObserver();
            }
            mOnOrientationChangeListener.setEnabled(false);
            mRotationObserver.stopObserver();
            if (mScreenOrientation != SCREEN_ORIENTATION_PORTRAIT) {
                setRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
            }
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.image_picture:
                    if (mDeleteFrame.getVisibility() == View.VISIBLE) {
                        mDeleteFrame.setVisibility(View.GONE);
                        break;
                    }
                    cancel();
                    break;

                case R.id.frame_btn_delete:
                case R.id.btn_delete:
                    View view = View.inflate(mContext, R.layout.dialog_message, null);
                    view.<TextView>findViewById(R.id.text_message)
                            .setText(R.string.areYouSureToDeleteThisPicture);
                    view.findViewById(R.id.btn_cancel).setOnClickListener(this);
                    view.findViewById(R.id.btn_ok).setOnClickListener(this);
                    mConfirmDeletePictureDialog = new AppCompatDialog(
                            mContext, R.style.DialogStyle_MinWidth_NoTitle);
                    mConfirmDeletePictureDialog.setContentView(view);
                    mConfirmDeletePictureDialog.show();
                    break;

                case R.id.btn_cancel:
                    mConfirmDeletePictureDialog.cancel();
                    mConfirmDeletePictureDialog = null;
                    break;
                case R.id.btn_ok:
                    mConfirmDeletePictureDialog.cancel();
                    mConfirmDeletePictureDialog = null;

                    final int currentItem = mGalleryViewPager.getCurrentItem();
                    mGalleryPagerAdapter.views.remove(currentItem);
                    mGalleryPagerAdapter.notifyDataSetChanged();
                    mPresenter.removePictureAt(currentItem);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (v.getId()) {
                case R.id.image_picture:
                    if (mDeleteFrame.getVisibility() != View.VISIBLE) {
                        mDeleteFrame.setVisibility(View.VISIBLE);
                        return true;
                    }
                    break;
            }
            return false;
        }

        // 使View占用刘海区（如果有）
        void setLayoutInDisplayCutout() {
            View parentDecorView = mParentWindow.getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                DisplayCutout dc = parentDecorView.getRootWindowInsets().getDisplayCutout();
                if (dc != null) {
                    mIsNotchSupport = true;
                    if (OSHelper.isEMUI()) {
                        mIsNotchSupportOnEMUI = true;
                    } else if (OSHelper.isMIUI()) {
                        mIsNotchSupportOnMIUI = true;
                    }
                    mNotchHeight = dc.getSafeInsetTop();
                    DisplayCutoutUtils.setLayoutInDisplayCutoutSinceP(mWindow, true);
                }
            } else if (OSHelper.isEMUI()) {
                if (DisplayCutoutUtils.hasNotchInScreenForEMUI(mContext)) {
                    mIsNotchSupport = mIsNotchSupportOnEMUI = true;
                    mNotchHeight = DisplayCutoutUtils.getNotchSizeForEMUI(mContext)[1];
                    DisplayCutoutUtils.setLayoutInDisplayCutoutForEMUI(mWindow, true);
                }
            } else if (OSHelper.isColorOS()) {
                if (DisplayCutoutUtils.hasNotchInScreenForColorOS(mContext)) {
                    mIsNotchSupport = true;
                    mNotchHeight = DisplayCutoutUtils.getNotchSizeForColorOS()[1];
                }
            } else if (OSHelper.isFuntouchOS()) {
                if (DisplayCutoutUtils.hasNotchInScreenForFuntouchOS(mContext)) {
                    mIsNotchSupport = true;
                    mNotchHeight = DisplayCutoutUtils.getNotchHeightForFuntouchOS(mContext);
                }
            } else if (OSHelper.isMIUI()) {
                if (DisplayCutoutUtils.hasNotchInScreenForMIUI()) {
                    mIsNotchSupport = mIsNotchSupportOnMIUI = true;
                    mNotchHeight = DisplayCutoutUtils.getNotchHeightForMIUI(mContext);
                    DisplayCutoutUtils.setLayoutInDisplayCutoutForMIUI(mWindow, true);
                }
            }
            if (mIsNotchSupportOnEMUI || mIsNotchSupportOnMIUI) {
                mNotchSwitchObserver = new ScreenNotchSwitchObserver(
                        parentDecorView.getHandler(), mContext,
                        mIsNotchSupportOnEMUI, mIsNotchSupportOnMIUI) {
                    boolean first = true;

                    @Override
                    public void onNotchChange(boolean selfChange, boolean hidden) {
                        if (first || mIsNotchHidden != hidden) {
                            first = false;
                            mIsNotchHidden = hidden;
                            adjustVisibleRegion();
                        }
                    }
                };
                mNotchSwitchObserver.startObserver();
            } else {
                adjustVisibleRegion();
            }
        }

        void adjustVisibleRegion() {
            if (!mIsNotchSupport) {
                return;
            }
            switch (mScreenOrientation) {
                case SCREEN_ORIENTATION_PORTRAIT:
                    mGalleryViewPager.setPadding(0, mNotchHeight, 0, 0);
                    break;
                case SCREEN_ORIENTATION_LANDSCAPE:
                    mGalleryViewPager.setPadding(
                            mIsNotchSupportOnEMUI && mIsNotchHidden ? 0 : mNotchHeight, 0,
                            0, 0);
                    break;
                case SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    mGalleryViewPager.setPadding(0, 0,
                            mIsNotchSupportOnEMUI && mIsNotchHidden ? 0 : mNotchHeight, 0);
                    break;
            }
        }
    }
}
