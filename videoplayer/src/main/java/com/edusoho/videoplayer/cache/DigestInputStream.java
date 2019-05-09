package com.edusoho.videoplayer.cache;

import android.text.TextUtils;

import com.edusoho.videoplayer.util.DigestUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by suju on 16/12/29.
 */

public class DigestInputStream extends InputStream {

    private InputStream mTargetInputStream;
    private int mCurrentDigestIndex;
    private byte[] mDigestKey;

    public DigestInputStream(InputStream target, String host) {
        initDigestKey(host, true);
        this.mTargetInputStream = target;
    }

    public DigestInputStream(InputStream target, String host, boolean isMd5) {
        initDigestKey(host, isMd5);
        this.mTargetInputStream = target;
    }

    private void initDigestKey(String host, boolean isMd5) {

        String digestStr = host;
        if (isMd5) {
            if (!TextUtils.isEmpty(host)) {
                digestStr = DigestUtils.md5(host);
            }
        }

        this.mCurrentDigestIndex = 0;
        this.mDigestKey = digestStr.getBytes();
    }

    @Override
    public int read() throws IOException {
        int length = mTargetInputStream.read();
        byte[] buffer = new byte[1];
        buffer[0] = (byte) length;
        processorByteArray(1, buffer);
        return buffer[0];
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int length = mTargetInputStream.read(buffer);
        processorByteArray(length, buffer);
        return length;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int length = mTargetInputStream.read(buffer, byteOffset, byteCount);
        processorByteArray(length, buffer);

        return length;
    }

    @Override
    public void close() throws IOException {
        super.close();
        mTargetInputStream.close();
    }

    private void processorByteArray(int length, byte[] buffer) {
        if (length <= 0 || this.mDigestKey.length == 0) {
            return;
        }

        int keyLength = mDigestKey.length - 1;
        for (int i = 0; i < length; i++) {
            byte b = buffer[i];
            mCurrentDigestIndex = mCurrentDigestIndex > keyLength ? 0 : mCurrentDigestIndex;
            b = (byte) (b ^ mDigestKey[mCurrentDigestIndex++]);
            buffer[i] = b;
        }
    }
}