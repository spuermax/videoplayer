package com.edusoho.videoplayer.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by suju on 17/4/17.
 */

public class MessageBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.edusoho.videoplayer.message";

    private Callback mCallback;

    public MessageBroadcastReceiver(Callback callback) {
        this.mCallback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mCallback != null) {
            String type = intent.getStringExtra("type");
            String message = intent.getStringExtra("message");
            mCallback.onReceive(type, message);
        }
    }

    public static Intent getIntent(String type, String message) {
        Intent intent = new Intent(ACTION);
        intent.putExtra("type", type);
        intent.putExtra("message", message);
        return intent;
    }

    public interface Callback {
        void onReceive(String type, String mesasge);
    }
}
