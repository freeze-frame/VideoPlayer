/*
 * Created on 2019/5/31 11:52 PM.
 * Copyright © 2019 刘振林. All rights reserved.
 */

package com.liuzhenlin.texturevideoview.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;

//import org.mp4parser.muxer.Movie;
//import org.mp4parser.muxer.Track;
//import org.mp4parser.muxer.builder.DefaultMp4Builder;
//import org.mp4parser.muxer.container.mp4.MovieCreator;
//import org.mp4parser.muxer.tracks.ClippedTrack;

import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author 刘振林
 */
public class VideoUtils {
    private VideoUtils() {
    }

    @NonNull
    public static File clip(@NonNull String srcPath, @NonNull String destPath, long fromMs, long toMs)
            throws IOException, IllegalArgumentException, UnsupportedOperationException {
        if (TextUtils.isEmpty(srcPath)) {
            throw new IllegalArgumentException("Path of the Source file cannot be null or empty");
        }
        if (TextUtils.isEmpty(destPath)) {
            throw new IllegalArgumentException("Path of the destination file cannot be null or empty");
        }
        if (fromMs >= toMs) {
            throw new IllegalArgumentException("fromMs >= toMs");
        }

        File srcFile = new File(srcPath);
        if (!srcFile.exists()) {
            throw new IllegalArgumentException("The source file does not exist");
        }

        Movie movie = MovieCreator.build(srcPath);
        List<Track> tracks = movie.getTracks();
        // Removes all tracks from which we will create new ones
        movie.setTracks(new LinkedList<>());

        double startTime = fromMs / 1000d;
        double endTime = toMs / 1000d;

        boolean timeCorrected = false;
        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample, we SHOULD make sure that the start of the new fragment is exactly
        // such a frame.
        for (Track track : tracks) {
            final long[] syncSamples = track.getSyncSamples();
            if (syncSamples != null && syncSamples.length > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a single movie containing
                    // multiple qualities of the same video (Microsoft Smooth Streaming file).
                    throw new UnsupportedOperationException("Unsupported. " +
                            "The startTime has already been corrected by another track with sync samples.");
                }
                startTime = correctTimeToSyncSample(track, startTime, false);
                endTime = correctTimeToSyncSample(track, endTime, true);
                timeCorrected = true;
            }
        }

        for (Track track : tracks) {
            double lastTime = -1;
            double currentTime = 0;
            long currentSample = 0;
            long startSample = -1;
            long endSample = -1;

            final long timeScale = track.getTrackMetaData().getTimescale();
            for (long sampleDuration : track.getSampleDurations()) {
                if (currentTime > lastTime && currentTime <= startTime) {
                    // Current sample is still before the start time
                    startSample = currentSample;
                }
                if (currentTime > lastTime && currentTime <= endTime) {
                    // Current sample is after the start time and still before the end time
                    endSample = currentSample;
                }
                lastTime = currentTime;
                currentTime += (double) sampleDuration / (double) timeScale;
                currentSample++;
            }

            movie.addTrack(new CroppedTrack(track, startSample, endSample));
        }

        File destFile = new File(destPath);
        File destParentFile = destFile.getParentFile();
        if (destParentFile != null && !destParentFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destParentFile.mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            new DefaultMp4Builder().build(movie).writeContainer(fos.getChannel());
        } catch (Throwable t) {
            //noinspection ResultOfMethodCallIgnored
            destFile.delete();
            // Deliver the cause to the caller
            throw t;
        }
        return destFile;
    }

    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        final long[] syncSamples = track.getSyncSamples();
        final double[] timeOfSyncSamples = new double[syncSamples.length];
        final long timeScale = track.getTrackMetaData().getTimescale();

        long currentSample = 0;
        double currentTime = 0;
        for (long sampleDuration : track.getSampleDurations()) {
            currentSample++; // Samples always start with 1

            final int index = Arrays.binarySearch(syncSamples, currentSample);
            if (index >= 0) {
                timeOfSyncSamples[index] = currentTime;
            }

            currentTime += (double) sampleDuration / (double) timeScale;
        }

        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample == cutHere) {
                return timeOfSyncSample;
            } else if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }

        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }
}
