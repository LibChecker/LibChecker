package com.absinthe.libchecker.java;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * Created by Whyn on 2017/8/28.
 */

public class FileTree extends Timber.Tree {
    private static final int MSG_RECORD_FILE_NAME = 0x00;
    private static final int MSG_RECORD_FILE_PATH = 0x01;
    private static final int MSG_RECORD_FILE_CONTENT = 0x03;

    private static final String SEPARATOR = ",";
    //all objects share one thread
    private static final HandlerThread sLogThread;
    private static int sNum = 0;

    static {
        sLogThread = new HandlerThread("File_Log_Thread");
        sLogThread.start();
    }

    private final Handler mHandler;

    public FileTree() {
        ++sNum;
        mHandler = new FileLogHandler(sLogThread.getLooper());
    }

    public FileTree storeAt(final String path) {
        mHandler.obtainMessage(MSG_RECORD_FILE_PATH, path).sendToTarget();
        return this;
    }

    public FileTree name(final String name) {
        mHandler.obtainMessage(MSG_RECORD_FILE_NAME, name).sendToTarget();
        return this;
    }

    public void release() {
        mHandler.removeCallbacksAndMessages(null);
        if (--sNum == 0)
            doRelease();
    }

    private void doRelease() {
        sLogThread.quit();
    }

    private <T> void checkNotNull(T target, String desc) {
        if (target == null)
            throw new IllegalArgumentException(desc);
    }

    @Override
    protected void log(final int priority, final String tag,
                       @NotNull final String message, final Throwable t) {
        mHandler.obtainMessage(MSG_RECORD_FILE_CONTENT,
                new LogMsgBean(priority, tag, message, t)).sendToTarget();
    }

    private class FileLogHandler extends Handler {

        private BufferedWriter mWriter;
        private String mLogFilePath;
        private String mLogFileName;

        public FileLogHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_RECORD_FILE_NAME:
                    mLogFileName = (String) msg.obj;
                    break;
                case MSG_RECORD_FILE_PATH:
                    mLogFilePath = (String) msg.obj;
                    break;
                case MSG_RECORD_FILE_CONTENT:
                    preparedWriter();
                    doWrite((LogMsgBean) msg.obj);
                    break;
            }
        }

        private void doWrite(@NonNull LogMsgBean msgBean) {
            try {
                mWriter.write(produceLogInfo(msgBean));
                mWriter.newLine();
                mWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mWriter = null;
                }
            }
        }

        private void preparedWriter() {
            if (mWriter == null) {
                checkNotNull(mLogFilePath, "please set log file path first");
                checkNotNull(mLogFileName, "please set log file name first");
                try {
                    mWriter = new BufferedWriter(
                            new FileWriter(new File(mLogFilePath, mLogFileName), true));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //2017.08.28 04:41:09.022,VERBOSE/MainActivity,watch the tag hehe
    private String produceLogInfo(@NonNull LogMsgBean msgBean) {
        return produceLogInfo(msgBean.priority, msgBean.tag, msgBean.message, msgBean.throwable);
    }

    //you can override this method to create your own log content
    protected String produceLogInfo(int priority, String tag, String message, Throwable t) {
        StringBuilder builder = new StringBuilder()
                .append(formatDate(new Date(System.currentTimeMillis())))
                .append(SEPARATOR)
                .append(getLogLevel(priority))
                .append("/")
                .append(tag == null ? "" : tag)
                .append(SEPARATOR)
                .append(message);
        return builder.toString();
    }

    private String getLogLevel(int priority) {
        String level;
        switch (priority) {
            case Log.VERBOSE:
                level = "VERBOSE";
                break;
            case Log.INFO:
                level = "INFO";
                break;
            case Log.DEBUG:
                level = "DEBUG";
                break;
            case Log.WARN:
                level = "WARN";
                break;
            case Log.ERROR:
                level = "ERROR";
                break;
            case Log.ASSERT:
                level = "ASSERT";
                break;
            default:
                level = "unknown";
        }
        return level;
    }

    private final ThreadLocal<SimpleDateFormat> sDateFormat = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS", Locale.UK);
        }
    };

    @Nullable
    private String formatDate(Date date) {
        SimpleDateFormat format = sDateFormat.get();
        if (format == null) {
            return null;
        }
        return format.format(date);
    }

    private static class LogMsgBean {
        final int priority;
        final String tag;
        final String message;
        final Throwable throwable;

        public LogMsgBean(final int priority, final String tag,
                          final String message, final Throwable throwable) {
            this.priority = priority;
            this.tag = tag;
            this.message = message;
            this.throwable = throwable;
        }
    }
}
