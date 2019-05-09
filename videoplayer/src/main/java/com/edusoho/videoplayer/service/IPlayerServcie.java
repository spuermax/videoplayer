package com.edusoho.videoplayer.service;

import org.videolan.libvlc.IVLCVout;

/**
 * Created by suju on 16/12/18.
 */

public interface IPlayerServcie {

    long getTime();

    long getLength();

    IVLCVout getVLCVout();
}
