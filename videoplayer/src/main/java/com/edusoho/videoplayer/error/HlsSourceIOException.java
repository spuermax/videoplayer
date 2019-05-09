package com.edusoho.videoplayer.error;

import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;

/**
 * Created by suju on 17/4/20.
 */

public class HlsSourceIOException extends IOException {

    private DataSpec mDataSpec;

    public HlsSourceIOException(DataSpec dataSpec) {
        this.mDataSpec = dataSpec;
    }

    public DataSpec getDataSpec() {
        return mDataSpec;
    }
}
