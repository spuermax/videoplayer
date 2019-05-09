package com.edusoho.videoplayer.util;

/**
 * Created by suju on 16/12/29.
 */

public class NumberUtil {

    public static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
        }
        return 0;
    }
}
