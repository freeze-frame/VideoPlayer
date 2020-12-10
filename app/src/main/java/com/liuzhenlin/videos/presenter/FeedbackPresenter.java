/*
 * Created on 2020-12-10 2:11:34 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.presenter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import com.liuzhenlin.texturevideoview.utils.BitmapUtils;
import com.liuzhenlin.videos.Consts;
import com.liuzhenlin.videos.R;
import com.liuzhenlin.videos.dao.FeedbackSavedPrefs;
import com.liuzhenlin.videos.utils.BitmapUtils2;
import com.liuzhenlin.videos.utils.Executors;
import com.liuzhenlin.videos.utils.MailUtil;
import com.liuzhenlin.videos.utils.NetworkUtil;
import com.liuzhenlin.videos.view.activity.IFeedbackView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 刘振林
 */
class FeedbackPresenter extends Presenter<IFeedbackView> implements IFeedbackPresenter {

    private FeedbackSavedPrefs mFeedbackSPs;
    private String mSavedFeedbackText = Consts.EMPTY_STRING;
    private String mSavedContactWay = Consts.EMPTY_STRING;
    private List<String> mSavedPicturePaths;

    private static final String PREFIX_MAIL_SUBJECT = "[视频反馈] ";

    private static final String KEY_SAVED_FEEDBACK_TEXT = "ksft";
    private static final String KEY_SAVED_CONTACT_WAY = "kscw";
    private static final String KEY_SAVED_PICTURE_PATHS = "kspp";

    private static final String KEY_FILLED_FEEDBACK_TEXT = "kfft";
    private static final String KEY_FILLED_CONTACT_WAY = "kfcw";
    private static final String KEY_FILLED_PICTURE_PATHS = "kfpp";

    private PictureGridAdapter mGridAdapter;

    @Override
    public void attachToView(@NonNull IFeedbackView view) {
        super.attachToView(view);
        mFeedbackSPs = new FeedbackSavedPrefs(mContext);
        mGridAdapter = new PictureGridAdapter(mThemedContext);
    }

    @Override
    public void restoreData(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            cacheCurrData(mFeedbackSPs.getText(), mFeedbackSPs.getContactWay(),
                    mFeedbackSPs.getPicturePaths());
            if (mView != null) {
                mView.refreshCurrTexts(mSavedFeedbackText, mSavedContactWay);
            }
            if (mSavedPicturePaths != null) {
                List<String> invalidPaths = null;
                for (String path : mSavedPicturePaths) {
                    // 有可能该路径下的图片已被删除：如果删除了，从sp文件中移除该保存的路径
                    if (!new File(path).exists()) {
                        if (invalidPaths == null)
                            invalidPaths = new LinkedList<>();
                        invalidPaths.add(path);
                        continue;
                    }
                    addPicture(path);
                }
                if (invalidPaths != null) {
                    mSavedPicturePaths.removeAll(invalidPaths);
                    mFeedbackSPs.savePicturePaths(mSavedPicturePaths);
                }
            }
        } else {
            String[] savedPicturePaths = (String[])
                    savedInstanceState.getSerializable(KEY_SAVED_PICTURE_PATHS);
            cacheCurrData(savedInstanceState.getString(KEY_SAVED_FEEDBACK_TEXT, Consts.EMPTY_STRING),
                    savedInstanceState.getString(KEY_SAVED_CONTACT_WAY, Consts.EMPTY_STRING),
                    savedPicturePaths == null ? null : Arrays.asList(savedPicturePaths));
            if (mSavedPicturePaths != null) {
                Iterator<String> it = mSavedPicturePaths.iterator();
                while (it.hasNext()) {
                    if (!new File(it.next()).exists()) {
                        it.remove();
                    }
                }
            }

            if (mView != null) {
                mView.refreshCurrTexts(
                        savedInstanceState.getString(KEY_FILLED_FEEDBACK_TEXT, Consts.EMPTY_STRING),
                        savedInstanceState.getString(KEY_FILLED_CONTACT_WAY, Consts.EMPTY_STRING));
            }
            String[] picturePaths = (String[])
                    savedInstanceState.getSerializable(KEY_FILLED_PICTURE_PATHS);
            if (picturePaths != null) {
                for (String path : picturePaths) {
                    if (!new File(path).exists()) {
                        continue;
                    }
                    addPicture(path);
                }
            }
        }
    }

    @Override
    public void saveData(@NonNull Bundle outState, @NonNull String text, @NonNull String contactWay) {
        outState.putString(KEY_SAVED_FEEDBACK_TEXT, mSavedFeedbackText);
        outState.putString(KEY_SAVED_CONTACT_WAY, mSavedContactWay);
        if (mSavedPicturePaths != null) {
            outState.putSerializable(KEY_SAVED_PICTURE_PATHS,
                    mSavedPicturePaths.toArray(Consts.EMPTY_STRING_ARRAY));
        }

        outState.putString(KEY_FILLED_FEEDBACK_TEXT, text);
        outState.putString(KEY_FILLED_CONTACT_WAY, contactWay);
        if (!mGridAdapter.mPicturePaths.isEmpty()) {
            outState.putSerializable(KEY_FILLED_PICTURE_PATHS,
                    mGridAdapter.mPicturePaths.toArray(Consts.EMPTY_STRING_ARRAY));
        }
    }

    @Override
    public void persistentlySaveUserFilledData(
            @NonNull String text, @NonNull String contactWay, boolean toastResultIfSaved) {
        if (hasDataChanged(text, contactWay)) {
            cacheCurrData(text, contactWay,
                    mGridAdapter.mPicturePaths.isEmpty() ?
                            null : new ArrayList<>(mGridAdapter.mPicturePaths));

            mFeedbackSPs.saveText(mSavedFeedbackText);
            mFeedbackSPs.saveContactWay(mSavedContactWay);
            mFeedbackSPs.savePicturePaths(mSavedPicturePaths);

            if (toastResultIfSaved && mView != null) {
                mView.toastResultOnUserFilledDataSaved();
            }
        }
    }

    private void cacheCurrData(String text, String contactWay, List<String> picturePaths) {
        mSavedFeedbackText = text;
        mSavedContactWay = contactWay;
        mSavedPicturePaths = picturePaths;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean hasDataChanged(@NonNull String text, @NonNull String contactWay) {
        if (!(text.equals(mSavedFeedbackText) && contactWay.equals(mSavedContactWay)))
            return true;

        boolean arraysAreNull =
                mGridAdapter.mPicturePaths == null && mSavedPicturePaths == null;
        if (arraysAreNull) return false;

        boolean arraysAreNonnull =
                !(mGridAdapter.mPicturePaths == null || mSavedPicturePaths == null);
        if (arraysAreNonnull) {
            //@formatter:off
            return !(mGridAdapter.mPicturePaths.isEmpty() && mSavedPicturePaths.isEmpty()
                    || Arrays.equals(
                            mGridAdapter.mPicturePaths.toArray(Consts.EMPTY_STRING_ARRAY),
                            mSavedPicturePaths.toArray(Consts.EMPTY_STRING_ARRAY))); //@formatter:on
        } else {
            return !(mGridAdapter.mPicturePaths != null && mGridAdapter.mPicturePaths.isEmpty()
                    || mSavedPicturePaths != null && mSavedPicturePaths.isEmpty());
        }
    }

    @Override
    public void sendFeedback(@NonNull String text, @NonNull String contactWay) {
        if (mThemedContext != null && NetworkUtil.isNetworkConnected(mContext)) {
            MailUtil.sendMail(
                    mThemedContext,
                    PREFIX_MAIL_SUBJECT + contactWay,
                    text,
                    null,
                    mGridAdapter.mPicturePaths.isEmpty() ?
                            null : mGridAdapter.mPicturePaths.toArray(Consts.EMPTY_STRING_ARRAY));

            // 提交反馈后，清除sp文件保存的数据
            mFeedbackSPs.clear();

            // 重设临时缓存的数据
            mSavedFeedbackText = Consts.EMPTY_STRING;
            mSavedContactWay = Consts.EMPTY_STRING;
            // 刷新TextView的显示
            if (mView != null) {
                mView.refreshCurrTexts(mSavedFeedbackText, mSavedContactWay);
            }

            // 清空PictureGridAdapter的数据
            if (!mGridAdapter.mPicturePaths.isEmpty()) {
                if (mSavedPicturePaths != null) {
                    mSavedPicturePaths.clear();
                }

                Bitmap temp = mGridAdapter.mPictures.get(mGridAdapter.mPictures.size() - 1);
                Iterator<Bitmap> it = mGridAdapter.mPictures.iterator();
                while (it.hasNext()) {
                    Bitmap bitmap = it.next();
                    if (temp == bitmap) continue;
                    bitmap.recycle();
                    it.remove();
                }
                mGridAdapter.mPicturePaths.clear();
                mGridAdapter.mLoadedPicturePaths.clear();
                // 刷新GridView
                mGridAdapter.notifyDataSetChanged();
            }
        } else {
            if (mView != null) {
                mView.showToast(mContext, R.string.noNetworkConnection, Toast.LENGTH_SHORT);
            }
            persistentlySaveUserFilledData(text, contactWay, false);
        }
    }

    @Override
    public void addPicture(final String path) {
        final PictureGridAdapter gridAdapter = mGridAdapter;
        final List<String> picturePaths = gridAdapter.mPicturePaths;
        if (path != null && !picturePaths.contains(path)) {
            picturePaths.add(path);
            Executors.SERIAL_EXECUTOR.execute(() -> {
                if (mView == null) {
                    return;
                }
                final Bitmap bitmap = BitmapUtils2.decodeRotatedBitmapFormFile(path);
                if (bitmap != null) {
                    Executors.MAIN_EXECUTOR.post(() -> {
                        if (mView != null && picturePaths.contains(path)) {
                            List<String> loadedPicturePaths = gridAdapter.mLoadedPicturePaths;
                            List<Bitmap> pictures = gridAdapter.mPictures;
                            loadedPicturePaths.add(loadedPicturePaths.size(), path);
                            pictures.add(pictures.size() - 1, bitmap);
                            gridAdapter.notifyDataSetChanged();
                        } else {
                            if (!gridAdapter.mPictures.contains(bitmap)) {
                                bitmap.recycle();
                            }
                        }
                    });
                }
            });
        }
    }

    @Override
    public void removePictureAt(int index) {
        // 图片全部被删除时，销毁对话框
        if (mView != null && mGridAdapter.mPictures.size() == 2) {
            mView.hidePicturePreviewDialog();
        }
        mGridAdapter.mPictures.get(index).recycle();
        mGridAdapter.mPictures.remove(index);
        mGridAdapter.mPicturePaths.remove(mGridAdapter.mLoadedPicturePaths.remove(index));
        mGridAdapter.notifyDataSetChanged();
    }

    @Override
    public void recyclePictures() {
        for (Bitmap bitmap : mGridAdapter.mPictures) {
            bitmap.recycle();
        }
        mGridAdapter.mPictures.clear();
    }

    @NonNull
    @Override
    public <T extends BaseAdapter & AdapterView.OnItemClickListener> T getPictureGridAdapter() {
        //noinspection unchecked
        return (T) mGridAdapter;
    }

    private final class PictureGridAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

        final List<Bitmap> mPictures = new ArrayList<>(MAX_COUNT_UPLOAD_PICTURES + 1);
        final List<String> mPicturePaths = new LinkedList<>();
        final List<String> mLoadedPicturePaths = new LinkedList<>();

        static final int MAX_COUNT_UPLOAD_PICTURES = 3;

        PictureGridAdapter(@NonNull Context context) {
            //noinspection ConstantConditions
            mPictures.add(BitmapUtils.drawableToBitmap(
                    AppCompatResources.getDrawable(context, R.drawable.ic_add_photo_gray_36dp)));
        }

        @Override
        public int getCount() {
            int count = mPictures.size();
            return Math.min(count, MAX_COUNT_UPLOAD_PICTURES);
        }

        @Override
        public Object getItem(int position) {
            return mPictures.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mView.getPictureGridViewHolder(position, convertView, parent,
                    mPictures.get(position), mPictures.size()).itemView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int pictureCount = mPictures.size();
            if (position == pictureCount - 1) {
                if (mPicturePaths.size() < PictureGridAdapter.MAX_COUNT_UPLOAD_PICTURES) {
                    mView.pickPicture();
                }
            } else {
                mView.showPicturePreviewDialog(mPictures.subList(0, pictureCount - 1), position);
            }
        }
    }
}
