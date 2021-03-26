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
    public void test() {
        final String directory = "/Users/liuzhenlin/AppData/AndroidStudio/projects/Videos/app/release";
        final String fileName = "app-release";
        final String extension = ".apk";

        final int splitCount = FileUtils.splitFile(directory, fileName, extension, 1000 * 1000);

        File[] files = new File[splitCount];
        for (int i = 0; i < splitCount; i++) {
            files[i] = new File(directory, fileName + (i + 1) + extension);
        }

        File dstFile = new File(directory, fileName + extension);

        FileUtils.mergeFiles(files, dstFile, false);

        System.out.println(dstFile.length());
        System.out.println(FileUtils.getFileSha1(dstFile));
    }
}