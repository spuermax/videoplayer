package com.edusoho.videoplayer.helper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.format.Formatter;
import android.util.Log;
import net.lingala.zip4j.core.ZipFile;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by suju on 17/1/17.
 */

public class LibUpdateHelper {

    static String DOWNLOAD_URL = "http://download.edusoho.com/vlc-%slib-main-1.3.2.zip";

    private ProgressDialog mProgressDialog;
    private LibUpdateListener mLibUpdateListener;
    private Context mContext;
    private AsyncTask<Integer, Integer, Boolean> mUpdateTask;
    private String mLibType;

    public LibUpdateHelper(Activity activity) {
        mContext = activity.getApplicationContext();
        mProgressDialog = new ProgressDialog(activity);
    }

    private File getFileFromNet(String url, Context context, DownProcessListener listener) {
        File target = null;
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        FileOutputStream zipFileOutput = null;
        try {
            zipFileOutput = context.openFileOutput("libso.zip", Context.MODE_PRIVATE);
            HttpResponse response = client.execute(httpGet);
            int len = -1;
            byte[] buffer = new byte[8192];
            int downLenth = 0;
            int total = (int) response.getEntity().getContentLength();
            InputStream inputStream = response.getEntity().getContent();
            while ((len = inputStream.read(buffer)) != -1) {
                listener.update(total, downLenth += len);
                zipFileOutput.write(buffer, 0, len);
            }

            zipFileOutput.close();
            target = context.getFileStreamPath("libso.zip");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpGet.abort();
            client.getConnectionManager().shutdown();
            try {
                if (zipFileOutput != null) {
                    zipFileOutput.close();
                }
            } catch (Exception e) {
                //nothing
            }
        }

        return target;
    }

    public boolean downSoLib(String type, Context context, DownProcessListener listener) {
        try {
            File target = getFileFromNet(String.format(DOWNLOAD_URL, type), context, listener);
            if (target == null) {
                return false;
            }

            File libDir = context.getDir("lib", Context.MODE_PRIVATE);
            ZipFile zipFile = new ZipFile(target.getAbsoluteFile());
            zipFile.extractAll(libDir.getAbsolutePath());
            Log.d("SoLibManager", "zip success");
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public void setLibUpdateListener(LibUpdateListener libUpdateListener) {
        this.mLibUpdateListener = libUpdateListener;
    }

    public void stop() {
        if (mUpdateTask != null) {
            mUpdateTask.cancel(true);
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    public void update(String libType, LibUpdateListener listener) {
        mProgressDialog.show();
        this.mLibType = libType;
        setLibUpdateListener(listener);
        mUpdateTask = new AsyncTask<Integer, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Integer... params) {
                DownProcessListener listener = new DownProcessListener() {
                    @Override
                    public void update(int count, int process) {
                        publishProgress(count, process);
                    }
                };
                return downSoLib(mLibType, mContext, listener);
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.setMessage(String.format(
                            "正在下载解码包:%s/%s"
                            , Formatter.formatFileSize(mContext, values[1]),
                            Formatter.formatFileSize(mContext, values[0])
                    ));
                }
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                if (aBoolean) {
                    mProgressDialog.cancel();
                    Log.d("SoLibManager", "done");
                    if (mLibUpdateListener != null) {
                        mLibUpdateListener.onInstalled();
                    }
                    return;
                }if (mLibUpdateListener != null) {
                    mLibUpdateListener.onFail();
                }
                stop();
            }
        };
        mUpdateTask.execute(0);
    }

    public interface LibUpdateListener {
        void onInstalled();

        void onFail();
    }

    interface DownProcessListener {
        void update(int count, int process);
    }
}
