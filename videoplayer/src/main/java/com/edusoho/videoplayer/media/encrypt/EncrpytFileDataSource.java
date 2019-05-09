package com.edusoho.videoplayer.media.encrypt;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.edusoho.videoplayer.broadcast.MessageBroadcastReceiver;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by suju on 17/4/5.
 */

public class EncrpytFileDataSource implements DataSource {

    /**
     * Thrown when IOException is encountered during local file read operation.
     */
    public static class FileDataSourceException extends IOException {

        public FileDataSourceException(IOException cause) {
            super(cause);
        }
    }

    private final TransferListener<? super EncrpytFileDataSource> listener;

    private RandomAccessFile file;
    private Uri uri;
    private String digestKey;
    private long bytesRemaining;
    private boolean opened;
    private Context mContext;

    public EncrpytFileDataSource() {
        this(null, null, null);
    }

    /**
     * @param listener An optional listener.
     */
    public EncrpytFileDataSource(Context context, String digestKey, TransferListener<? super EncrpytFileDataSource> listener) {
        this.mContext = context;
        this.digestKey = digestKey;
        this.listener = listener;
    }

    @Override
    public long open(DataSpec dataSpec) throws EncrpytFileDataSource.FileDataSourceException {
        try {
            uri = dataSpec.uri;
            file = getRealRandomAccessFile(uri);
            file.seek(dataSpec.position);
            bytesRemaining = dataSpec.length == C.LENGTH_UNSET ? file.length() - dataSpec.position
                    : dataSpec.length;
            if (bytesRemaining < 0) {
                throw new EOFException();
            }
        } catch (IOException e) {
            mContext.sendBroadcast(MessageBroadcastReceiver.getIntent("FileDataSourceException", uri.toString()));
            throw new EncrpytFileDataSource.FileDataSourceException(e);
        }

        opened = true;
        if (listener != null) {
            listener.onTransferStart(this, dataSpec);
        }

        return bytesRemaining;
    }

    private RandomAccessFile getRealRandomAccessFile(Uri uri) throws FileNotFoundException {
        if (uri.getPath().endsWith(".m3u8")) {
            return new RandomAccessFile(new File(uri.getPath()), "r");
        }
        if (uri.getPath().contains("ext_x_key")) {
            return new EncrpytRandomAccessFile(new File(uri.getPath()), "r");
        }
        return new DigestInputStream(new File(uri.getPath()), "r", digestKey);
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws EncrpytFileDataSource.FileDataSourceException {
        if (readLength == 0) {
            return 0;
        } else if (bytesRemaining == 0) {
            return C.RESULT_END_OF_INPUT;
        } else {
            int bytesRead;
            try {
                bytesRead = file.read(buffer, offset, (int) Math.min(bytesRemaining, readLength));
            } catch (IOException e) {
                throw new EncrpytFileDataSource.FileDataSourceException(e);
            }

            if (bytesRead > 0) {
                bytesRemaining -= bytesRead;
                if (listener != null) {
                    listener.onBytesTransferred(this, bytesRead);
                }
            }

            return bytesRead;
        }
    }

    @Override
    public Uri getUri() {
        return uri;
    }

    @Override
    public void close() throws EncrpytFileDataSource.FileDataSourceException {
        uri = null;
        try {
            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            throw new EncrpytFileDataSource.FileDataSourceException(e);
        } finally {
            file = null;
            if (opened) {
                opened = false;
                if (listener != null) {
                    listener.onTransferEnd(this);
                }
            }
        }
    }
}