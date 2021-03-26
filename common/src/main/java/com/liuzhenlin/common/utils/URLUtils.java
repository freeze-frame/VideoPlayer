/*
 * Created on 2020-2-24 1:59:31 PM.
 * Copyright © 2020 刘振林. All rights reserved.
 */

package com.liuzhenlin.common.utils;

import java.util.regex.Pattern;

/**
 * @author 刘振林
 */
public class URLUtils {
    private URLUtils() {
    }

    private static final String IP_ADDRESS_STRING =
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))";

    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
            "\u00A0-\uD7FF" +
            "\uF900-\uFDCF" +
            "\uFDF0-\uFFEF" +
            "\uD800\uDC00-\uD83F\uDFFD" +
            "\uD840\uDC00-\uD87F\uDFFD" +
            "\uD880\uDC00-\uD8BF\uDFFD" +
            "\uD8C0\uDC00-\uD8FF\uDFFD" +
            "\uD900\uDC00-\uD93F\uDFFD" +
            "\uD940\uDC00-\uD97F\uDFFD" +
            "\uD980\uDC00-\uD9BF\uDFFD" +
            "\uD9C0\uDC00-\uD9FF\uDFFD" +
            "\uDA00\uDC00-\uDA3F\uDFFD" +
            "\uDA40\uDC00-\uDA7F\uDFFD" +
            "\uDA80\uDC00-\uDABF\uDFFD" +
            "\uDAC0\uDC00-\uDAFF\uDFFD" +
            "\uDB00\uDC00-\uDB3F\uDFFD" +
            "\uDB44\uDC00-\uDB7F\uDFFD" +
            "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    /**
     * Valid characters for IRI TLD defined in RFC 3987.
     */
    private static final String TLD_CHAR = "a-zA-Z" + UCS_CHAR;

    /**
     * RFC 1035 Section 2.3.4 limits the labels to a maximum 63 octets.
     */
    private static final String IRI_LABEL =
            "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "])?";

    /**
     * RFC 3492 references RFC 1034 and limits Punycode algorithm output to 63 characters.
     */
    private static final String PUNYCODE_TLD = "xn\\-\\-[\\w\\-]{0,58}\\w";

    private static final String TLD = "(" + PUNYCODE_TLD + "|" + "[" + TLD_CHAR + "]{2,63}" + ")";

    private static final String HOST_NAME = "(" + IRI_LABEL + "\\.)+" + TLD;

    private static final String DOMAIN_NAME_STR = "(" + HOST_NAME + "|" + IP_ADDRESS_STRING + ")";

    private static final String PROTOCOL = "(?i:http|https|rtmp|rtsp)://";

    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";

    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";

    /* A word boundary or end of input. This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    /**
     * Regular expression pattern to match most part of RFC 3987 Internationalized URLs, aka IRIs.
     */
    public static final Pattern PATTERN_WEB_URL = Pattern.compile("("
            + "("
            + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?"
            + "(?:" + DOMAIN_NAME_STR + ")"
            + "(?:" + PORT_NUMBER + ")?"
            + ")"
            + "(" + PATH_AND_QUERY + ")?"
            + WORD_BOUNDARY
            + ")");

    /**
     * @return {@code true} if the url is an http: url.
     */
    public static boolean isHttpUrl(String url) {
        return url != null &&
                url.length() > 6 &&
                url.substring(0, 7).equalsIgnoreCase("http://");
    }

    /**
     * @return {@code true} if the url is an https: url.
     */
    public static boolean isHttpsUrl(String url) {
        return url != null &&
                url.length() > 7 &&
                url.substring(0, 8).equalsIgnoreCase("https://");
    }

    /**
     * @return {@code true} if the url is a rtmp: url.
     */
    public static boolean isRtmpUrl(String url) {
        return url != null &&
                url.length() > 6 &&
                url.substring(0, 7).equalsIgnoreCase("rtmp://");
    }

    /**
     * @return {@code true} if the url is a rtsp: url.
     */
    public static boolean isRtspUrl(String url) {
        return url != null &&
                url.length() > 6 &&
                url.substring(0, 7).equalsIgnoreCase("rtsp://");
    }

    /**
     * @return {@code true} if the url is a network url.
     */
    public static boolean isNetworkUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return isHttpUrl(url) || isHttpsUrl(url) || isRtmpUrl(url) || isRtspUrl(url);
    }
}
