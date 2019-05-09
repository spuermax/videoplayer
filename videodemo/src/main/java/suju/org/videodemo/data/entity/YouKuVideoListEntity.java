package suju.org.videodemo.data.entity;

import java.util.Map;

/**
 * Created by suju on 17/2/6.
 */

public class YouKuVideoListEntity {

    private int total;

    private Map<Integer, VideoEntity> items;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public Map<Integer, VideoEntity> getItems() {
        return items;
    }

    public void setItems(Map<Integer, VideoEntity> items) {
        this.items = items;
    }
}
