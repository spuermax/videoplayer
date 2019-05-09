package com.edusoho.videoplayer.util;

import android.content.Context;

import com.edusoho.videoplayer.media.M3U8Stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by suju on 16/12/13.
 */

public class M3U8Util {

    private static Pattern M3U8_STREAM_PAT = Pattern.compile(
            "#EXT-X-STREAM-INF:PROGRAM-ID=(\\d+),BANDWIDTH=(\\d+),NAME=\"?(\\w+)\"?", Pattern.DOTALL);

    public static final String TEMP_FILE     = "temp_file";
    public static final String TEMP_FILE_DIR = "temp_file_dir";

    private static String completeUrlPath(String hostPath, String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return String.format("%s/%s", hostPath, url);
    }

    /*
        解析m3u8列表
    */
    private static List<M3U8Stream> parseM3u8ListFromFile(String parentPath, BufferedReader reader) {
        List<M3U8Stream> m3U8Streams = new ArrayList<>();
        M3U8Stream currentItem = null;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = M3U8_STREAM_PAT.matcher(line);
                if (matcher.find()) {
                    M3U8Stream item = new M3U8Stream();
                    item.setBandwidth(Integer.parseInt(matcher.group(2)));
                    item.setName(matcher.group(3));
                    m3U8Streams.add(item);
                    currentItem = item;
                    continue;
                }
                if (currentItem != null) {
                    currentItem.setUrl(completeUrlPath(parentPath, line));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return m3U8Streams;
    }

    public static List<M3U8Stream> getM3U8StreamListFromPath(String parentPath, String filePath) {
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            return parseM3u8ListFromFile(parentPath, new BufferedReader(new InputStreamReader(fileInputStream)));
        } catch (Exception e) {
        }
        return new ArrayList<>();
    }

    public static String downloadM3U8File(Context context, String m3u8Url) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(m3u8Url);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/vnd.edusoho.v2+json");
            File tempDir = context.getDir(TEMP_FILE_DIR, Context.MODE_PRIVATE);
            File tempFile = new File(tempDir, DigestUtils.md5(m3u8Url));
            OutputStream outputStream = new FileOutputStream(tempFile);
            FileUtils.copyFile(urlConnection.getInputStream(), outputStream);
            outputStream.flush();
            outputStream.close();
            return tempFile.getAbsolutePath();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null)
                urlConnection.disconnect();
        }

        return null;
    }
}
