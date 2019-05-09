package com.edusoho.videoplayer.media;

/**
 * Created by suju on 16/12/13.
 */

public class M3U8Stream {

    private long bandwidth;

    private String name;

    private String url;

    public long getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "M3U8Stream{" +
                "bandwidth=" + bandwidth +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
