package com.edusoho.videoplayer;

import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;

/**
 * Created by suju on 16/12/12.
 */

public class VideoPlayerUITest extends ActivityInstrumentationTestCase2<VideoDemoActivity> {

    public VideoPlayerUITest() {
        super(VideoDemoActivity.class);
    }

    @UiThreadTest
    public void testPlayVideo() throws Exception {
    }
}
