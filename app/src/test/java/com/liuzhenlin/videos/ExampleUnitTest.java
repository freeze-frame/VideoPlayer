package com.liuzhenlin.videos;

import com.liuzhenlin.common.utils.FileUtils;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void splitAndMergeFile() {
        String directory = "/Users/liuzhenlin/Android/projects/Videos/app/release";
        String fileName = "app-release";
        String extension = ".apk";
        File file = new File(directory, fileName + extension);
        long fileLength = file.length();
        String fileSha1 = FileUtils.getFileSha1(file);

        int splitCount = FileUtils.splitFile(directory, fileName, extension, 1000 * 1000);
        File[] splits = new File[splitCount];
        for (int i = 0; i < splitCount; i++) {
            splits[i] = new File(directory, fileName + (i + 1) + extension);
        }
        FileUtils.mergeFiles(splits, file, false);

        assertEquals(fileLength, file.length());
        assertEquals(fileSha1, FileUtils.getFileSha1(file));
        System.out.println("splitCount= " + splitCount + "\n"
                + "fileLength= " + fileLength + "\n"
                + "fileSha1= " + fileSha1);
    }
}