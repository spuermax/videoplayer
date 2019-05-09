package suju.org.videodemo.data.impl;

import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import suju.org.videodemo.data.ResultCallback;
import suju.org.videodemo.data.VideoModel;
import suju.org.videodemo.util.YouKuSignUtil;

/**
 * Created by suju on 17/2/6.
 */

public abstract class YouKuVideoModel implements VideoModel {

    private OkHttpClient mClient;
    private Map<String, Request> mRequestTask;

    public YouKuVideoModel() {
        mRequestTask = new WeakHashMap<>();
    }

    @Override
    public void release() {
        for (String name : mRequestTask.keySet()) {
            removeTask(mRequestTask.get(name));
        }
        mRequestTask.clear();
    }

    public void getVideoUrl(String url) {
        mClient = new OkHttpClient();
        Request.Builder builder = new Request.Builder().url("https://api.47ks.com/config/webmain.php");

        FormEncodingBuilder formEncodingBuilder = new FormEncodingBuilder();
        formEncodingBuilder.add("token", "fH0B39c7RhYsATXgGy2MT80ZqU9U5s8fjJiNfJv1++auNqe3lFoezRrx9UsLTXty1JHcIMU6CZzO6K5axk/mKLgkI1ZUlyOib0dBhWIsgCTjCqMLl62m70VfnyU+r9AN1c+i6B+gZv+y2km76KW3RsjK7rVSFtJW7eNNHvTOhqzh4rfEWPsBItacxPDc0gg7oPCg4up+bNvskgcYVqIOwk0ozjEEaowNoIhXfhiMx0I=");
        formEncodingBuilder.add("v", "http://v.youku.com/v_show/id_XMjQ5ODU0MzIyOA==.html?spm=a2hww.20023042.m_223465.5~5~5~5~5~5~A");
        formEncodingBuilder.add("from", "http://2yungou.cc/");
        formEncodingBuilder.add("t", "phone");
        formEncodingBuilder.add("up", "0");

        builder.method("POST", formEncodingBuilder.build());
        Request request = builder.build();
        mRequestTask.put(request.httpUrl().toString(), request);
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                //resultCallback.onResponse(null);
                removeTask(request);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                Log.d("onResponse", response.body().string());
                //resultCallback.onResponse(response.body().string());
                removeTask(response.request());
            }
        });
    }

    @Override
    public void searchVideos(String search, final ResultCallback resultCallback) {
        mClient = new OkHttpClient();

        TreeMap<String, Object> params = new TreeMap<>();
        params.put("action", "youku.search.video.keyword.get");
        params.put("keyword", search);

        StringBuilder query = new StringBuilder();
        try {
            params = YouKuSignUtil.get_sign(params, "ad6533776a5c413f", "13d2c962fffc10278192962ab03eef97");
            query.append("?");
            for (String key : params.keySet()) {
                query.append(key).append("=").append(params.get(key)).append("&");
            }
            query.deleteCharAt(query.length() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Request request = new Request.Builder().url("https://openapi.youku.com/router/rest.json" + query.toString()).build();
        mRequestTask.put(request.httpUrl().toString(), request);
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                resultCallback.onResponse(null);
                removeTask(request);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                Log.d("onResponse", "" + Thread.currentThread());
                resultCallback.onResponse(response.body().string());
                removeTask(response.request());
            }
        });
    }

    protected void removeTask(Request request) {
        if (request != null) {
            mClient.cancel(request.tag());
        }
    }
}
