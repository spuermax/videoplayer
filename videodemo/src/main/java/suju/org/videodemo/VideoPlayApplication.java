package suju.org.videodemo;

import android.app.Application;

/**
 * Created by suju on 17/1/5.
 */

public class VideoPlayApplication extends Application {

    public static VideoPlayApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }
}
