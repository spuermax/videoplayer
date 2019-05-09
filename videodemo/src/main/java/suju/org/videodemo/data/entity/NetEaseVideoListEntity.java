package suju.org.videodemo.data.entity;

import java.util.List;

import suju.org.videodemo.data.VideoModel;

/**
 * Created by suju on 17/2/7.
 */

public class NetEaseVideoListEntity {

    private int cursor;

    private int code;

    private List<VideoEntity> data;

    public int getCursor() {
        return cursor;
    }

    public void setCursor(int cursor) {
        this.cursor = cursor;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<VideoEntity> getData() {
        return data;
    }

    public void setData(List<VideoEntity> data) {
        this.data = data;
    }
}
