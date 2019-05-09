package suju.org.videodemo.data.entity;

import java.util.List;

/**
 * Created by suju on 17/2/7.
 */

public class NetEaseVideoInfoEntity {

    private DataEntity data;

    public DataEntity getData() {
        return data;
    }

    public void setData(DataEntity data) {
        this.data = data;
    }

    public class DataEntity {

        private String largeImgurl;

        private List<VideoEntity> videoList;

        private int code;

        public String getLargeImgurl() {
            return largeImgurl;
        }

        public void setLargeImgurl(String largeImgurl) {
            this.largeImgurl = largeImgurl;
        }

        public List<VideoEntity> getVideoList() {
            return videoList;
        }

        public void setVideoList(List<VideoEntity> videoList) {
            this.videoList = videoList;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }
}
