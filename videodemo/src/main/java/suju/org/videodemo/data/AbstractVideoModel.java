package suju.org.videodemo.data;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by suju on 17/2/7.
 */

public abstract class AbstractVideoModel implements VideoModel {

    private OkHttpClient mClient;
    private Map<String, Request> mRequestTask;

    public AbstractVideoModel() {
        mRequestTask = new WeakHashMap<>();
        mClient = new OkHttpClient();
    }

    protected void sendGetRequest(String url, final ResultCallback resultCallback) {
        Request.Builder builder = new Request.Builder().url(url);
        Request request = builder.build();
        mRequestTask.put(request.httpUrl().toString(), request);
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                resultCallback.onResponse(null);
                removeTask(request);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                resultCallback.onResponse(response.body().string());
                removeTask(response.request());
            }
        });
    }

    @Override
    public void release() {
        for (String name : mRequestTask.keySet()) {
            removeTask(mRequestTask.get(name));
        }
        mRequestTask.clear();
        mClient = null;
    }

    protected void removeTask(Request request) {
        if (request != null) {
            mClient.cancel(request.tag());
        }
    }
}
