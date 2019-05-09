package suju.org.videodemo.data.impl;

import java.util.HashMap;
import java.util.Map;

import suju.org.videodemo.data.AbstractVideoModel;
import suju.org.videodemo.data.ResultCallback;

/**
 * Created by suju on 17/2/7.
 */

public class NetEaseVideoModel extends AbstractVideoModel {

    private Map<String, Integer> tagIdMap;

    public NetEaseVideoModel() {
        super();
        tagIdMap = new HashMap<>();
        tagIdMap.put("心理", 23);
        tagIdMap.put("演讲", 5);
        tagIdMap.put("纪录片", 11);
        tagIdMap.put("经济", 19);
        tagIdMap.put("社会", 7);
        tagIdMap.put("物理", 27);
        tagIdMap.put("媒体", 21);
        tagIdMap.put("技能", 25);
        tagIdMap.put("管理", 24);
        tagIdMap.put("公开课", 35);
        tagIdMap.put("TED", 4);
    }

    @Override
    public void searchVideos(String search, ResultCallback resultCallback) {
        String url = String.format("http://c.open.163.com/mob/classify/newplaylist.do?type=5&id=%d&flag=1&cursor=", getSearchId(search));
        sendGetRequest(url, resultCallback);
    }

    @Override
    public void getVideoInfo(String videoId, ResultCallback resultCallback) {
        String url = String.format("http://c.open.163.com/mob/%s/getMoviesForAndroid.do", videoId);
        sendGetRequest(url, resultCallback);
    }

    private int getSearchId(String search) {
        if (!tagIdMap.containsKey(search)) {
            return 4;
        }
        return tagIdMap.get(search);
    }
}
