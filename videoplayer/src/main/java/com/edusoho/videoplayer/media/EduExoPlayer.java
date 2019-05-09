package com.edusoho.videoplayer.media;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.edusoho.videoplayer.media.sonic.SonicMediaCodecAudioTrackRenderer;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

/**
 * Created by suju on 2017/10/10.
 */

public class EduExoPlayer extends SimpleExoPlayer {

    private static SonicMediaCodecAudioTrackRenderer mSonicMediaCodecAudioTrackRenderer;

    public EduExoPlayer(Context context,
                        TrackSelector trackSelector,
                        LoadControl loadControl,
                        @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager) {
        super(new DefaultRenderersFactory(context) {
            @Override
            protected void buildAudioRenderers(Context context,
                                               @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                               AudioProcessor[] audioProcessors,
                                               Handler eventHandler,
                                               AudioRendererEventListener eventListener,
                                               int extensionRendererMode,
                                               ArrayList<Renderer> out) {
                super.buildAudioRenderers(context, drmSessionManager, audioProcessors, eventHandler, eventListener, extensionRendererMode, out);
                handle(context, drmSessionManager, eventHandler, eventListener, out);
            }
        }, trackSelector, loadControl, drmSessionManager);
    }

    private static void handle(Context context,
                               @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                               Handler eventHandler,
                               AudioRendererEventListener eventListener,
                               ArrayList<Renderer> out) {
        int size = out.size();
        for (int i = 0; i < size; i++) {
            Renderer render = out.get(i);
            if (render instanceof MediaCodecAudioRenderer) {
                if (Util.SDK_INT < 23) {
                    out.remove(i);
                    mSonicMediaCodecAudioTrackRenderer = new SonicMediaCodecAudioTrackRenderer(context,
                            MediaCodecSelector.DEFAULT, drmSessionManager, true,
                            eventHandler, eventListener, AudioCapabilities.getCapabilities(context));
                    out.add(mSonicMediaCodecAudioTrackRenderer);
                }
                return;
            }
        }
    }

    public void setRate(float speed) {
        if (Util.SDK_INT < 23) {
            mSonicMediaCodecAudioTrackRenderer.setPlaybackSpeed(speed);
        } else {
            this.setPlaybackParameters(new PlaybackParameters(speed));
        }
    }
}
