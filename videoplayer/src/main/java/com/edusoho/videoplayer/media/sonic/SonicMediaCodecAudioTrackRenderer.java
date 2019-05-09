package com.edusoho.videoplayer.media.sonic;

/**
 * Created by suju on 2017/10/10.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;

import java.nio.ByteBuffer;

public final class SonicMediaCodecAudioTrackRenderer extends MediaCodecAudioRenderer {

    private Sonic      sonic;
    private float      speed;
    private byte[]     inBuffer;
    private byte[]     outBuffer;
    private ByteBuffer bufferSonicOut;

    private int bufferIndex;

    public SonicMediaCodecAudioTrackRenderer(Context context,
                                             MediaCodecSelector mediaCodecSelector,
                                             DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                             boolean playClearSamplesWithoutKeys,
                                             Handler eventHandler,
                                             AudioRendererEventListener eventListener,
                                             AudioCapabilities audioCapabilities) {
        super(context, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, audioCapabilities);
        bufferIndex = -1;
        speed = 1.0f;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected final void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) throws ExoPlaybackException {
        super.onOutputFormatChanged(codec, outputFormat);
        int sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        int channelCount = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        int bufferSize = channelCount * 4096;//1024*4 4M 22.05 kHz/4096
        inBuffer = new byte[bufferSize];
        outBuffer = new byte[bufferSize];

        sonic = new Sonic(sampleRate, channelCount);
        bufferSonicOut = ByteBuffer.wrap(outBuffer, 0, 0);
        setSpeed(speed);
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip) throws ExoPlaybackException {
        if (bufferIndex == this.bufferIndex) {//bufferIndex: 0 ~ 14 / 0 ~ 3
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        } else {
            int sizeSonic;
            this.bufferIndex = bufferIndex;
            sizeSonic = buffer.remaining();
            buffer.get(inBuffer, 0, sizeSonic);
            sonic.writeBytesToStream(inBuffer, sizeSonic);
            sizeSonic = sonic.readBytesFromStream(outBuffer, outBuffer.length);
            bufferSonicOut.position(0);
            bufferSonicOut.limit(sizeSonic);
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, bufferSonicOut, bufferIndex, bufferFlags, bufferPresentationTimeUs, shouldSkip);
        }
    }

    public final void setSonicSpeed(float speed) {
        synchronized (this) {
            try {
                this.speed = speed;
                if (sonic != null) {
                    sonic.setSpeed(speed);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public float getSonicSpeed() {
        return sonic.getSpeed();
    }

    public final void setSonicPitch(float pitch) {
        synchronized (this) {
            try {
                if (sonic != null) {
                    sonic.setPitch(pitch);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }


    public final void setSonicRate(float rate) {
        synchronized (this) {
            try {
                if (sonic != null) {
                    sonic.setRate(rate);
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public void setPlaybackSpeed(float speed) {
        setSpeed(speed);
    }

    private void setSpeed(float speed) {
        this.speed = speed;
        setSonicSpeed(speed);
        setSonicPitch(1);
        setSonicRate(1);
    }

}