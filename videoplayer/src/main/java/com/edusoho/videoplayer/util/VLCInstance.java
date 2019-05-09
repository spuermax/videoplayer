/*****************************************************************************
 * VLCInstance.java
 * ****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.edusoho.videoplayer.util;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.edusoho.videoplayer.BuildConfig;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.VLCUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VLCInstance {
    public final static String TAG = "VLC/UiTools/VLCInstance";

    private static LibVLC sLibVLC = null;

    public static void linkCompatLib(Context context) {
        final File outDir = new File(context.getFilesDir(), "compat");
        if (!outDir.exists())
            outDir.mkdir();
        final File outFile = new File(outDir, "libcompat.7.so");

        /* The file may had been already copied from the asset, try to load it */
        if (outFile.exists()) {
            try {
                System.load(outFile.getPath());
                return;
            } catch (UnsatisfiedLinkError ule) {
                /* the file can be invalid, try to copy it again */
            }
        }

        /* copy libcompat.7.so from assert to a data dir */
        InputStream is = null;
        FileOutputStream fos = null;
        boolean success = false;
        try {
            is = context.getAssets().open("libcompat.7.so");
            fos = new FileOutputStream(outFile);
            final byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1)
                fos.write(buffer, 0, read);
            success = true;
        } catch (IOException e) {
        } finally {
            Util.close(is);
            Util.close(fos);
        }

        /* load the lib coming from the asset */
        if (success) {
            try {
                System.load(outFile.getPath());
            } catch (UnsatisfiedLinkError ule) {
            }
        }
    }

    /**
     * A set of utility functions for the VLC application
     */
    public synchronized static LibVLC get(final Context context) throws IllegalStateException {
        if (sLibVLC == null) {
            Thread.setDefaultUncaughtExceptionHandler(new VLCCrashHandler());

            if (!VLCUtil.hasCompatibleCPU(context)) {
                Log.d(TAG, "LibVLC initialisation failed: " + VLCUtil.getErrorMsg());
                return null;
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                Log.w(TAG, "linking with true compat lib...");
                linkCompatLib(context);
            }

            sLibVLC = new LibVLC(VLCOptions.getLibOptions(context));
            sLibVLC.setUserAgent("player", "kuozhi-Android-vlc-player-" + BuildConfig.VERSION_NAME);
            LibVLC.setOnNativeCrashListener(new LibVLC.OnNativeCrashListener() {
                @Override
                public void onNativeCrash() {
                    Log.d(TAG, "PID:" + android.os.Process.myPid());
                }
            });

            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "copy lua");
                    String destinationFolder = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY +
                            "/Android/data/" + context.getPackageName() + "/lua";
                    AssetManager am = context.getResources().getAssets();
                    FileUtils.copyAssetFolder(am, "lua", destinationFolder);
                }
            });
        }
        return sLibVLC;
    }

    public static synchronized void restart(Context context) throws IllegalStateException {
        if (sLibVLC != null) {
            sLibVLC.release();
            sLibVLC = new LibVLC(VLCOptions.getLibOptions(context));
        }
    }

    public static synchronized boolean testCompatibleCPU(Context context) {
        if (sLibVLC == null && !VLCUtil.hasCompatibleCPU(context)) {
            Log.d(TAG, "not support cpu");
            return false;
        } else
            return true;
    }
}
