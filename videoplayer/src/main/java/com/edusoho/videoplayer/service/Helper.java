package com.edusoho.videoplayer.service;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.MainThread;

import java.util.ArrayList;

/**
 * Created by suju on 16/12/7.
 */

public class Helper {
    private ArrayList<Client.Callback> mFragmentCallbacks = new ArrayList<Client.Callback>();
    final private Client.Callback mActivityCallback;
    private Client mClient;
    protected IPlayerServcie mService;

    public Helper(Context context, Client.Callback activityCallback, Intent serviceIntent) {
        mClient = new Client(context, mClientCallback, serviceIntent);
        mActivityCallback = activityCallback;
    }

    @MainThread
    public void registerFragment(Client.Callback connectCb) {
        if (connectCb == null)
            throw new IllegalArgumentException("connectCb can't be null");
        mFragmentCallbacks.add(connectCb);
        if (mService != null)
            connectCb.onConnected(mService);

    }

    @MainThread
    public void unregisterFragment(Client.Callback connectCb) {
        if (mService != null)
            connectCb.onDisconnected();
        mFragmentCallbacks.remove(connectCb);
    }

    @MainThread
    public void onStart() {
        mClient.connect();
    }

    @MainThread
    public void onStop() {
        mClientCallback.onDisconnected();
        mClient.disconnect();
    }

    private final Client.Callback mClientCallback = new Client.Callback() {
        @Override
        public void onConnected(IPlayerServcie service) {
            mService = service;
            mActivityCallback.onConnected(service);
            for (Client.Callback connectCb : mFragmentCallbacks)
                connectCb.onConnected(mService);
        }

        @Override
        public void onDisconnected() {
            mService = null;
            mActivityCallback.onDisconnected();
            for (Client.Callback connectCb : mFragmentCallbacks)
                connectCb.onDisconnected();
        }
    };
}