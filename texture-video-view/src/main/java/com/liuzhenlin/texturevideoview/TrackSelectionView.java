/*
 * Created on 2020-3-22 12:32:56 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.util.Synthetic;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.liuzhenlin.texturevideoview.bean.TrackInfo;
import com.liuzhenlin.texturevideoview.utils.ComparableSparseArray;
import com.liuzhenlin.texturevideoview.utils.Utils;

import java.util.List;

/**
 * A view to select tracks.
 *
 * @author 刘振林
 */
public class TrackSelectionView extends LinearLayout {

    private final ViewPager2 mViewPager;

    private TrackNameProvider mTrackNameProvider;

    @Synthetic IVideoPlayer mVideoPlayer;
    private final IVideoPlayer.OnPlaybackStateChangeListener mOnPlaybackStateChangeListener =
            (oldState, newState) -> {
                switch (newState) {
                    case IVideoPlayer.PLAYBACK_STATE_PREPARING:
                    case IVideoPlayer.PLAYBACK_STATE_PREPARED:
                    case IVideoPlayer.PLAYBACK_STATE_PLAYING:
                    case IVideoPlayer.PLAYBACK_STATE_PAUSED:
                    case IVideoPlayer.PLAYBACK_STATE_ERROR:
                    case IVideoPlayer.PLAYBACK_STATE_IDLE:
                        updateTrackGroups();
                        break;
                }
            };

    @Synthetic ComparableSparseArray<ComparableSparseArray<TrackInfo>> mTrackGroups =
            newTrackGroupSparseArray();

    private static final int TRACK_GROUP_COUNT = 3;

    private static ComparableSparseArray<ComparableSparseArray<TrackInfo>> newTrackGroupSparseArray() {
        ComparableSparseArray<ComparableSparseArray<TrackInfo>> trackGroups =
                new ComparableSparseArray<>(TRACK_GROUP_COUNT);
        trackGroups.put(TrackInfo.TRACK_TYPE_VIDEO, new ComparableSparseArray<>(2));
        trackGroups.put(TrackInfo.TRACK_TYPE_AUDIO, new ComparableSparseArray<>(2));
        trackGroups.put(TrackInfo.TRACK_TYPE_SUBTITLE, new ComparableSparseArray<>(1));
        for (int i = 0; i < TRACK_GROUP_COUNT; i++) {
            trackGroups.valueAt(i).put(IVideoPlayer.INVALID_TRACK_INDEX, null /* unused */);
        }
        return trackGroups;
    }

    public TrackSelectionView(Context context) {
        this(context, null);
    }

    public TrackSelectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackSelectionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundResource(R.color.bg_popup_dark);
        setOrientation(VERTICAL);
        View.inflate(context, R.layout.view_track_selection, this);

        mViewPager = findViewById(R.id.viewpager2);
        mViewPager.setAdapter(new PagerAdapter());
        mViewPager.setOffscreenPageLimit(TRACK_GROUP_COUNT - 1);
        new TabLayoutMediator(findViewById(R.id.tablayout), mViewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(context.getString(R.string.video));
                            break;
                        case 1:
                            tab.setText(context.getString(R.string.audio));
                            break;
                        case 2:
                            tab.setText(context.getString(R.string.subtitle));
                            break;
                    }
                }).attach();
    }

    @Synthetic TrackNameProvider getTrackNameProvider() {
        if (mTrackNameProvider == null) {
            mTrackNameProvider = new DefaultTrackNameProvider(getResources());
        }
        return mTrackNameProvider;
    }

    /**
     * Sets the {@link TrackNameProvider} used to generate the user visible name of each track and
     * updates the view with track names queried from the specified provider.
     *
     * @param trackNameProvider The {@link TrackNameProvider} to use.
     */
    public void setTrackNameProvider(@Nullable TrackNameProvider trackNameProvider) {
        mTrackNameProvider = trackNameProvider;
    }

    /**
     * Sets the {@link IVideoPlayer} from which to get the {@link TrackInfo}s and the indices of
     * the current selected video, audio and subtitle tracks. The VideoPlayer is also used to
     * select or deselect a related track from some track list in this view.
     *
     * @param videoPlayer The {@link IVideoPlayer} to use.
     */
    public void setVideoPlayer(@Nullable IVideoPlayer videoPlayer) {
        IVideoPlayer oldVideoPlayer = mVideoPlayer;
        if (videoPlayer != oldVideoPlayer) {
            mVideoPlayer = videoPlayer;

            if (ViewCompat.isAttachedToWindow(this)) {
                if (oldVideoPlayer != null) {
                    oldVideoPlayer.removeOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
                }

                updateTrackGroups();
                if (videoPlayer != null) {
                    videoPlayer.addOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
                }
            }
        }
    }

    private void updateTrackGroups() {
        mTrackGroups = newTrackGroupSparseArray();

        if (mVideoPlayer != null) {
            TrackInfo[] trackInfos = mVideoPlayer.getTrackInfos();
            for (int i = 0; i < trackInfos.length; i++) {
                TrackInfo trackInfo = trackInfos[i];

                int trackType = trackInfo.trackType;
                switch (trackType) {
                    case TrackInfo.TRACK_TYPE_VIDEO:
                    case TrackInfo.TRACK_TYPE_AUDIO:
                    case TrackInfo.TRACK_TYPE_SUBTITLE:
                        mTrackGroups.get(trackType).put(i, trackInfo);
                        break;
                }
            }
        }

        //noinspection ConstantConditions
        mViewPager.getAdapter().notifyItemRangeChanged(0, TRACK_GROUP_COUNT, mTrackGroups);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mVideoPlayer != null) {
            updateTrackGroups();
            mVideoPlayer.addOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mVideoPlayer != null) {
            mVideoPlayer.removeOnPlaybackStateChangeListener(mOnPlaybackStateChangeListener);
        }
    }

    private final class PagerAdapter extends RecyclerView.Adapter<PagerAdapter.ViewHolder> {
        PagerAdapter() {
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_viewpager_track_lists, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                TrackListAdapter adapter = (TrackListAdapter) holder.recyclerView.getAdapter();

                //noinspection ConstantConditions
                int oldTrackSelection = adapter.mTrackSelection;
                ComparableSparseArray<TrackInfo> oldTrackGroup = adapter.mTrackGroup;

                adapter.updateDataSet();

                if (adapter.mTrackGroup.equals(oldTrackGroup)) {
                    if (adapter.mTrackSelection != oldTrackSelection) {
                        adapter.notifyItemChanged(
                                oldTrackGroup.indexOfKey(oldTrackSelection),
                                TrackListAdapter.PAYLOAD_CHECKED_STATE_CHANGED);
                        adapter.notifyItemChanged(
                                oldTrackGroup.indexOfKey(adapter.mTrackSelection),
                                TrackListAdapter.PAYLOAD_CHECKED_STATE_CHANGED);
                    }
                } else {
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (holder.recyclerView.getAdapter() == null) {
                holder.recyclerView.setAdapter(new TrackListAdapter(mTrackGroups.keyAt(position)));
            }
        }

        @Override
        public int getItemCount() {
            return TRACK_GROUP_COUNT;
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView recyclerView;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                recyclerView = itemView.findViewById(R.id.rv_track_list);
                recyclerView.addItemDecoration(
                        new DividerItemDecoration(itemView.getContext(), DividerItemDecoration.VERTICAL));
            }
        }
    }

    private final class TrackListAdapter extends RecyclerView.Adapter<TrackListAdapter.ViewHolder>
            implements View.OnClickListener {

        /*@MonotonicNonNull*/ ComparableSparseArray<TrackInfo> mTrackGroup;
        int mTrackSelection;
        final int mTrackType;

        static final int PAYLOAD_CHECKED_STATE_CHANGED = 1;
        static final int PAYLOAD_TRACK_NAME_CHANGED = 1 << 1;

        TrackListAdapter(@TrackInfo.TrackType int trackType) {
            mTrackType = trackType;
            updateDataSet();
        }

        void updateDataSet() {
            mTrackGroup = mTrackGroups.get(mTrackType);
            mTrackSelection =
                    mVideoPlayer == null
                            ? IVideoPlayer.INVALID_TRACK_INDEX
                            : mVideoPlayer.getSelectedTrackIndex(mTrackType);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_rv_track_list, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                int payload = (int) payloads.get(0);
                if ((payload & PAYLOAD_CHECKED_STATE_CHANGED) != 0) {
                    holder.checkedText.setChecked(mTrackGroup.keyAt(position) == mTrackSelection);
                }
                if ((payload & PAYLOAD_TRACK_NAME_CHANGED) != 0) {
                    if (position != 0) {
                        holder.checkedText.setText(
                                getTrackNameProvider().getTrackName(mTrackGroup.valueAt(position)));
                    }
                }
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.checkedText.setTag(position);
            holder.checkedText.setChecked(mTrackGroup.keyAt(position) == mTrackSelection);
            if (position == 0) {
                holder.checkedText.setText(R.string.track_selection_none);
            } else {
                holder.checkedText.setText(
                        getTrackNameProvider().getTrackName(mTrackGroup.valueAt(position)));
            }
        }

        @Override
        public int getItemCount() {
            int count = mTrackGroup.size();
            return count == 1 ? 0 /* no such track available */ : count;
        }

        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            int oldTrackSelection = mTrackSelection;
            int trackSelection = mTrackGroup.keyAt(position);
            if (trackSelection != oldTrackSelection) {
                if (trackSelection == IVideoPlayer.INVALID_TRACK_INDEX) {
                    mVideoPlayer.deselectTrack(oldTrackSelection);
                } else {
                    mVideoPlayer.selectTrack(trackSelection);
                }
                mTrackSelection = mVideoPlayer.getSelectedTrackIndex(mTrackType);
                if (mTrackSelection == trackSelection) {
                    notifyItemChanged(mTrackGroup.indexOfKey(oldTrackSelection),
                            PAYLOAD_CHECKED_STATE_CHANGED);
                    notifyItemChanged(position, PAYLOAD_CHECKED_STATE_CHANGED);
                } else {
                    // Sometimes track selection falls back to -1 (no such track selected)
                    if (mTrackSelection != oldTrackSelection) {
                        notifyItemChanged(mTrackGroup.indexOfKey(oldTrackSelection),
                                PAYLOAD_CHECKED_STATE_CHANGED);
                        notifyItemChanged(mTrackGroup.indexOfKey(mTrackSelection),
                                PAYLOAD_CHECKED_STATE_CHANGED);
                    }
                    showMsgOnTrackSelectionFailed(((TextView) v).getText().toString());
                }
            }
        }

        void showMsgOnTrackSelectionFailed(String trackName) {
            int stringRes;
            switch (mTrackType) {
                case TrackInfo.TRACK_TYPE_VIDEO:
                    stringRes = R.string.selectVideoTrackFailed;
                    break;
                case TrackInfo.TRACK_TYPE_AUDIO:
                    stringRes = R.string.selectAudioTrackFailed;
                    break;
                case TrackInfo.TRACK_TYPE_SUBTITLE:
                    stringRes = R.string.selectSubtitleTrackFailed;
                    break;
                default:
                    return;
            }
            Utils.showUserCancelableSnackbar(
                    TrackSelectionView.this,
                    getResources().getString(stringRes, trackName),
                    Snackbar.LENGTH_SHORT);
        }

        final class ViewHolder extends RecyclerView.ViewHolder {
            final CheckedTextView checkedText;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                checkedText = itemView.findViewById(R.id.checkedtext_trackSelection);
                checkedText.setOnClickListener(TrackListAdapter.this);
            }
        }
    }
}
