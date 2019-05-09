package com.edusoho.videoplayer.media.encrypt;

import android.text.TextUtils;
import android.util.Log;

import com.edusoho.videoplayer.util.DigestUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by suju on 17/4/8.
 */

public class DigestInputStream extends RandomAccessFile {

    private int mCurrentDigestIndex;
    private byte[] mDigestKey;
    private String fileName;

    public DigestInputStream(File file, String mode, String host) throws FileNotFoundException {
        super(file, mode);
        this.fileName = file.getPath();
        initDigestKey(host, true);
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
        int length = super.read();
        byte[] buffer = new byte[1];
        buffer[0] = (byte) length;
        processorByteArray(1, buffer);
        return buffer[0];
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int length = super.read(buffer);
        processorByteArray(length, buffer);
        return length;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int length = super.read(buffer, byteOffset, byteCount);
        processorByteArray(length, buffer);

        return length;
    }

    @Override
    public void close() throws IOException {
        super.close();
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