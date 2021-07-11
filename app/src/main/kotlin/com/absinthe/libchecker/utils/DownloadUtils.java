package com.absinthe.libchecker.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DownloadUtils {

    private volatile static DownloadUtils sDownloadUtil;
    private final OkHttpClient mOkHttpClient;

    private DownloadUtils() {
        mOkHttpClient = new OkHttpClient();
    }

    public static DownloadUtils get() {
        if (sDownloadUtil == null) {
            synchronized (DownloadUtils.class) {
                if (sDownloadUtil == null) {
                    sDownloadUtil = new DownloadUtils();
                }
            }
        }
        return sDownloadUtil;
    }

    /**
     * @param url      Download URL
     * @param file     File
     * @param listener Download callback
     */
    public void download(final Context context, final String url, final File file, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                listener.onDownloadFailed();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                InputStream is = null;
                byte[] buf = new byte[2048];
                int len;
                FileOutputStream fos = null;
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();

                try {
                    ResponseBody body = response.body();
                    if (body != null) {
                        is = body.byteStream();
                        long total = body.contentLength();

                        fos = new FileOutputStream(file);
                        long sum = 0;

                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / total * 100);
                            listener.onDownloading(progress);
                        }
                        fos.flush();
                        listener.onDownloadSuccess();
                    } else {
                        listener.onDownloadFailed();
                    }
                } catch (Exception e) {
                    listener.onDownloadFailed();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException ignore) {
                    }
                }
            }
        });
    }

    public interface OnDownloadListener {
        void onDownloadSuccess();

        void onDownloading(int progress);

        void onDownloadFailed();
    }
}
