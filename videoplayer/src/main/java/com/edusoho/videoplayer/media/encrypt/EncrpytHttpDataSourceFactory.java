package com.edusoho.videoplayer.media.encrypt;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

/**
 * Created by suju on 17/3/23.
 */

public class EncrpytHttpDataSourceFactory
        extends HttpDataSource.BaseFactory {

    private final String userAgent;
    private final TransferListener<? super DataSource> listener;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final boolean allowCrossProtocolRedirects;

    public EncrpytHttpDataSourceFactory(String userAgent) {
        this(userAgent, null);
    }

    public EncrpytHttpDataSourceFactory(
            String userAgent, TransferListener<? super DataSource> listener) {
        this(userAgent, listener, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, false);
    }

    public EncrpytHttpDataSourceFactory(String userAgent,
                                        TransferListener<? super DataSource> listener, int connectTimeoutMillis,
                                        int readTimeoutMillis, boolean allowCrossProtocolRedirects) {
        this.userAgent = userAgent;
        this.listener = listener;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
    }

//    @Override
//    protected HttpDataSource createDataSourceInternal() {
//        return new EncrpytDefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis,
//                readTimeoutMillis, allowCrossProtocolRedirects);
//    }

    @Override
    protected HttpDataSource createDataSourceInternal(HttpDataSource.RequestProperties defaultRequestProperties) {
        return new EncrpytDefaultHttpDataSource(userAgent, null, listener, connectTimeoutMillis,
                readTimeoutMillis, allowCrossProtocolRedirects);
    }
}
