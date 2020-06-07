/*
 * Created on 2018/09/05.
 * Copyright © 2018 刘振林. All rights reserved.
 */

package com.liuzhenlin.videos.utils;

import androidx.annotation.NonNull;

import java.util.LinkedList;

/**
 * @author 刘振林
 */
public class AlgorithmUtil {
    private AlgorithmUtil() {
    }

    /**
     * 求解str1 和 str2 的最长公共子序列（忽略字母大小写）
     */
    @NonNull
    public static String lcs(@NonNull String str1, @NonNull String str2, boolean ignoreCase) {
        if (ignoreCase) {
            str1 = str1.toLowerCase();
            str2 = str2.toLowerCase();
        }

        final char[] chars1 = str1.toCharArray();
        final char[] chars2 = str2.toCharArray();
        // 此处的棋盘长度要比字符串长度多加1，需要多存储一行0和一列0
        final int[][] array = new int[chars1.length + 1][chars2.length + 1];

        for (int i = 0; i < array[0].length; i++) { // 第0行第i列全部赋值为0
            array[0][i] = 0;
        }
        for (int j = 0; j < array.length; j++) { // 第j行，第0列全部为0
            array[j][0] = 0;
        }
        for (int i = 1; i < array.length; i++) { // 利用动态规划将数组赋满值
            for (int j = 1; j < array[i].length; j++) {
                if (chars1[i - 1] == chars2[j - 1]) {
                    array[i][j] = array[i - 1][j - 1] + 1; // 动态规划公式一
                } else {
                    array[i][j] = Math.max(array[i - 1][j], array[i][j - 1]); // 动态规划公式二
                }
            }
        }

        LinkedList<Character> stack = new LinkedList<>();
        for (int i = chars1.length - 1, j = chars2.length - 1; i >= 0 && j >= 0; ) {
            if (chars1[i] == chars2[j]) { // 字符串从后开始遍历，如若相等，则存入栈中
                stack.push(chars1[i]);
                i--;
                j--;
            } else {
                // 如果字符串的字符不同，则在数组中找相同的字符，
                // 注意：数组的行列要比字符串中字符的个数大1，因此i和j要各加1
                if (array[i + 1][j] > array[i][j + 1]) {
                    j--;
                } else {
                    i--;
                }
            }
        }

        StringBuilder result = new StringBuilder();
        while (!stack.isEmpty()) {
            result.append(stack.pop());
        }
        return result.toString();
    }
}
