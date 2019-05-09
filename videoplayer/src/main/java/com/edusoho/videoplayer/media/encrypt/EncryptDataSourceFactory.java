package com.edusoho.videoplayer.media.encrypt;

import android.content.Context;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;

/**
 * Created by suju on 17/3/23.
 */

public class EncryptDataSourceFactory implements DataSource.Factory {

    private       String                               digestKey;
    private final Context                              context;
    private final TransferListener<? super DataSource> listener;
    private final DataSource.Factory                   baseDataSourceFactory;

    /**
     * @param context   A context.
     * @param userAgent The User-Agent string that should be used.
     */
    public EncryptDataSourceFactory(Context context, String userAgent) {
        this(context, null, userAgent, null);
    }

    /**
     * @param context   A context.
     * @param userAgent The User-Agent string that should be used.
     * @param listener  An optional listener.
     */
    public EncryptDataSourceFactory(Context context, String digestKey, String userAgent,
                                    TransferListener<? super DataSource> listener) {
        this(context, digestKey, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
    }

    /**
     * @param context               A context.
     * @param listener              An optional listener.
     * @param baseDataSourceFactory A {@link DataSource.Factory} to be used to create a base {@link DataSource}
     *                              for {@link DefaultDataSource}.
     * @see DefaultDataSource#DefaultDataSource(Context, TransferListener, DataSource)
     */
    public EncryptDataSourceFactory(Context context, String digestKey, TransferListener<? super DataSource> listener,
                                    DataSource.Factory baseDataSourceFactory) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.digestKey = digestKey;
        this.baseDataSourceFactory = baseDataSourceFactory;
    }

    @Override
    public DataSource createDataSource() {
        return new EncryptDataSource(context, digestKey, listener, baseDataSourceFactory.createDataSource());
    }
}
