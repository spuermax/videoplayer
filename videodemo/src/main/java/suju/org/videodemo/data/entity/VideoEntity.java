package suju.org.videodemo.data.entity;

/**
 * Created by suju on 17/2/6.
 */

public class VideoEntity {

    private String title;

    private String picUrl;

    private String plid;

    private int viewcount;

    private String mp4SdUrl;

    private String mp4HdUrl;

    private String mp4ShdUrl;

    private String m3u8SdUrl;

    private String m3u8HdUrl;

    private String m3u8ShdUrl;

    public String getMp4SdUrl() {
        return mp4SdUrl;
    }

    public void setMp4SdUrl(String mp4SdUrl) {
        this.mp4SdUrl = mp4SdUrl;
    }

    public String getMp4HdUrl() {
        return mp4HdUrl;
    }

    public void setMp4HdUrl(String mp4HdUrl) {
        this.mp4HdUrl = mp4HdUrl;
    }

    public String getMp4ShdUrl() {
        return mp4ShdUrl;
    }

    public void setMp4ShdUrl(String mp4ShdUrl) {
        this.mp4ShdUrl = mp4ShdUrl;
    }

    public String getM3u8SdUrl() {
        return m3u8SdUrl;
    }

    public void setM3u8SdUrl(String m3u8SdUrl) {
        this.m3u8SdUrl = m3u8SdUrl;
    }

    public String getM3u8HdUrl() {
        return m3u8HdUrl;
    }

    public void setM3u8HdUrl(String m3u8HdUrl) {
        this.m3u8HdUrl = m3u8HdUrl;
    }

    public String getM3u8ShdUrl() {
        return m3u8ShdUrl;
    }

    public void setM3u8ShdUrl(String m3u8ShdUrl) {
        this.m3u8ShdUrl = m3u8ShdUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPicUrl() {
        return picUrl;
    }

    public void setPicUrl(String picUrl) {
        this.picUrl = picUrl;
    }

    public String getPlid() {
        return plid;
    }

    public void setPlid(String plid) {
        this.plid = plid;
    }

    public int getViewcount() {
        return viewcount;
    }

    public void setViewcount(int viewcount) {
        this.viewcount = viewcount;
    }
}
