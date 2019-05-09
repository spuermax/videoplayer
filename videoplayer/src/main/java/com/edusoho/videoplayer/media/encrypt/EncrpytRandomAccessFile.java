package com.edusoho.videoplayer.media.encrypt;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by suju on 17/4/20.
 */

public class EncrpytRandomAccessFile extends RandomAccessFile {

    public EncrpytRandomAccessFile(File file, String mode) throws FileNotFoundException{
        super(file, mode);
    }

    @Override
    public int read() throws IOException {
        return super.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int length = super.read(buffer);
        byte[] realBytes = new ArrayEncrpyt().encrpytBuffer(buffer, length);
        if (realBytes != null) {
            length = realBytes.length;
            System.arraycopy(realBytes, 0, buffer, 0, 16);
        }
        return length;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int length = super.read(buffer, byteOffset, byteCount);
        byte[] realBytes = new ArrayEncrpyt().encrpytBuffer(buffer, length);
        if (realBytes != null) {
            length = realBytes.length;
            System.arraycopy(realBytes, 0, buffer, 0, 16);
            Log.d("EncrpytRandomAccessFile", new String(buffer));
        }

        return length;
    }
}
