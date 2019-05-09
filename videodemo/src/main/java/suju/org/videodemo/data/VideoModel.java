package suju.org.videodemo.data;

/**
 * Created by suju on 17/2/6.
 */

public interface VideoModel {

    void release();

    void searchVideos(String search, ResultCallback resultCallback);

    void getVideoInfo(String videoId, ResultCallback resultCallback);
}
