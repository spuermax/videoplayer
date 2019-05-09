package com.edusoho.videoplayer.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.MainThread;

/**
 * Created by suju on 16/12/7.
 */

public class Client {
    public static final String TAG = "PlaybackService.Client";

    @MainThread
    public interface Callback {
        void onConnected(IPlayerServcie service);

        void onDisconnected();
    }

    private boolean mBound = false;
    private final Callback mCallback;
    private final Context mContext;
    private Intent mServiceIntent;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            if (!mBound)
                return;

            PlayServiceBinder playServiceBinder = (PlayServiceBinder) iBinder;
            IPlayerServcie servcie = playServiceBinder.getService();
            if (servcie != null)
                mCallback.onConnected(servcie);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mCallback.onDisconnected();
        }
    };

    private void startService(Context context) {
        context.startService(mServiceIntent);
    }

    public Client(Context context, Callback callback, Intent serviceIntent) {
        if (context == null || callback == null)
            throw new IllegalArgumentException("Context and callback can't be null");
        mContext = context;
        mCallback = callback;
        mServiceIntent = serviceIntent;
    }

    @MainThread
    public void connect() {
        if (mBound) {
            return;
        }

        startService(mContext);
        mBound = mContext.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @MainThread
    public void disconnect() {
        if (mBound) {
            mBound = false;
            mContext.unbindService(mServiceConnection);
        }
    }
}