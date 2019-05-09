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
package org.videolan.vlc.gui.helpers;

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

import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.media.MediaUtils;
import org.videolan.vlc.media.MediaWrapper;
import org.videolan.vlc.util.AndroidDevices;
import org.videolan.vlc.util.MurmurHash;
import org.videolan.vlc.util.Permissions;
import org.videolan.vlc.util.Util;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import suju.org.videodemo.BuildConfig;
import suju.org.videodemo.R;

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

    public static void clearCacheFolders() {
        for(String path : Arrays.asList(ART_DIR, COVER_DIR)) {
            File file = new File(path);
            if (file.exists())
                deleteContent(file, false);
        }
    }

    private static void deleteContent(File dir, boolean deleteDir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    deleteContent(file, true);
                }
            }
        }
        if (deleteDir)
            dir.delete();
    }

    private static String getCoverFromMediaStore(Context context, MediaWrapper media) {
        final String album = media.getAlbum();
        if (album == null)
            return null;
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, new String[]{
                        MediaStore.Audio.Albums.ALBUM,
                        MediaStore.Audio.Albums.ALBUM_ART},
                MediaStore.Audio.Albums.ALBUM + " LIKE ?",
                new String[]{album}, null);
        if (cursor == null) {
            // do nothing
        } else if (!cursor.moveToFirst()) {
            // do nothing
            cursor.close();
        } else {
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
            String albumArt = cursor.getString(titleColumn);
            cursor.close();
            return albumArt;
        }
        return null;
    }

    private static String getCoverFromVlc(Context context, MediaWrapper media) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String artworkURL = media.getArtworkURL();
        if (artworkURL != null && artworkURL.startsWith("file://")) {
            return Uri.decode(artworkURL).replace("file://", "");
        } else if(artworkURL != null && artworkURL.startsWith("attachment://")) {
            // Decode if the album art is embedded in the file
            String mArtist = MediaUtils.getMediaArtist(context, media);
            String mAlbum = MediaUtils.getMediaAlbum(context, media);

            /* Parse decoded attachment */
            if( mArtist.length() == 0 || mAlbum.length() == 0 ||
                    mArtist.equals(VLCApplication.getAppContext().getString(R.string.unknown_artist)) ||
                    mAlbum.equals(VLCApplication.getAppContext().getString(R.string.unknown_album)) )
            {
                /* If artist or album are missing, it was cached by title MD5 hash */
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] binHash = md.digest((artworkURL + media.getTitle()).getBytes("UTF-8"));
                /* Convert binary hash to normal hash */
                BigInteger hash = new BigInteger(1, binHash);
                String titleHash = hash.toString(16);
                while(titleHash.length() < 32) {
                    titleHash = "0" + titleHash;
                }
                /* Use generated hash to find art */
                artworkURL = ART_DIR + "/arturl/" + titleHash + "/art.png";
            } else {
                /* Otherwise, it was cached by artist and album */
                artworkURL = ART_DIR + "/artistalbum/" + mArtist + "/" + mAlbum + "/art.png";
            }

            return artworkURL;
        }
        return null;
    }


    private static String getCoverCachePath(Context context, MediaWrapper media, int width) {
        final int hash = MurmurHash.hash32(MediaUtils.getMediaArtist(context, media) + MediaUtils.getMediaAlbum(context, media));
        return COVER_DIR + (hash >= 0 ? "" + hash : "m" + (-hash)) + "_" + width;
    }

    public static Bitmap getCoverFromMemCache(Context context, MediaWrapper media, int width) {
        if (media != null && media.getArtist() != null && media.getAlbum() != null) {
            final BitmapCache cache = BitmapCache.getInstance();
            return cache.getBitmapFromMemCache(getCoverCachePath(context, media, width));
        } else
            return null;
    }

    private static void writeBitmap(Bitmap bitmap, String path) throws IOException {
        OutputStream out = null;
        try {
            File file = new File(path);
            if (file.exists() && file.length() > 0)
                return;
            out = new BufferedOutputStream(new FileOutputStream(file), 4096);
            if (bitmap != null)
                bitmap.compress(CompressFormat.JPEG, 90, out);
        } catch (Exception e) {
            Log.e(TAG, "writeBitmap failed : "+ e.getMessage());
        } finally {
            Util.close(out);
        }
    }

}
