/*****************************************************************************
 * AudioUtil.java
 *****************************************************************************
 * Copyright © 2011-2012 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package com.edusoho.videoplayer.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.RequiresPermission;
import android.util.Log;
import android.widget.Toast;

import com.edusoho.videoplayer.BuildConfig;
import com.edusoho.videoplayer.util.AndroidDevices;

import org.videolan.libvlc.util.AndroidUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;

public class AudioUtil {
    public final static String TAG = "VLC/AudioUtil";

    /**
     * Cache directory (/sdcard/Android/data/...)
     */
    public static String CACHE_DIR = null;
    /**
     * VLC embedded art storage location
     */
    public static String ART_DIR = null;
    /**
     * Cover caching directory
     */
    public static String COVER_DIR = null;
    /**
     * User-defined playlist storage directory
     */
    public static String PLAYLIST_DIR = null;


    @SuppressLint("NewApi")
    public static void prepareCacheFolder(Context context) {
        try {
            if (AndroidUtil.isFroyoOrLater() && AndroidDevices.hasExternalStorage() && context.getExternalCacheDir() != null)
                CACHE_DIR = context.getExternalCacheDir().getPath();
            else
                CACHE_DIR = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + "/Android/data/" + BuildConfig.APPLICATION_ID + "/cache";
        } catch (Exception e) { // catch NPE thrown by getExternalCacheDir()
            CACHE_DIR = AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY + "/Android/data/" + BuildConfig.APPLICATION_ID + "/cache";
        }
        ART_DIR = CACHE_DIR + "/art/";
        COVER_DIR = CACHE_DIR + "/covers/";
        PLAYLIST_DIR = CACHE_DIR + "/playlists/";

        for(String path : Arrays.asList(ART_DIR, COVER_DIR)) {
            File file = new File(path);
            if (!file.exists())
                file.mkdirs();
        }
    }
}
