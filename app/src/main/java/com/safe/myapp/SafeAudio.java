package com.safe.myapp;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.PowerManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SafeAudio {
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_z", Locale.US);
    private static final String STR_EXT_AUDIO = ".3gp";
    private static final String STR_NAME_AUDIO = "rec_";

    private String AudioFileName = "";
    private MediaRecorder mRecorder = null;
    private Context context;
    private SafeStatus status;
    private SafeCommunications comms;
    private SafeLogger logger;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;

    public SafeAudio(Context context, SafeStatus status, SafeCommunications comms, SafeLogger logger) {
        this.context = context;
        this.status = status;
        this.comms = comms;
        this.logger = logger;
        status.setAudioStarted(false);
    }

    public void startRecording() {
        // Check mic availability and recorder status
        if (status.isAudioStarted()) {
            logger.write("Audio recording already started");
            return;
        }
        if (!micavailable(context)) {
            logger.write("Microphone already in use");
            return;
        }
        // Everything is fine. Init stuff for acquiring wakelock
        powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Audio");
        // Acquire wakelock, else system can terminate our service while recording. We don't want that;)
        if (wakeLock != null) {
            wakeLock.acquire();
        }
        status.setAudioStarted(true);
        logger.write("Audio recording started");
        // create filename using date
        AudioFileName = STR_NAME_AUDIO + formatter.format(Calendar.getInstance().getTime()) + STR_EXT_AUDIO;
        File file = new File(context.getFilesDir(), AudioFileName);

        // Recorder settings
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(file.toString());
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
            logger.write("Something went wrong preparing the audio recorder, cannot start recording");
        }
    }

    public void stopRecording() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mRecorder.release();
                mRecorder = null;
                File file = new File(context.getFilesDir(), AudioFileName);
                comms.upload(file);
                logger.write("Audio recording stopped");
                status.setAudioStarted(false);
                // Release wakelock when recording stops
                if (wakeLock != null) {
                    wakeLock.release();
                    wakeLock = null;
                }
            } catch (RuntimeException e) {
                logger.write("Could not stop MediaRecorder (RuntimeException)");
            }
        } else {
            logger.write("Was not recording");
        }
    }

    private boolean micavailable(Context context) {
        MediaRecorder tmpRecorder = new MediaRecorder();
        tmpRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        tmpRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        tmpRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        tmpRecorder.setOutputFile(new File(context.getCacheDir(), "MediaUtil#micAvailTestFile").getAbsolutePath());
        boolean available = true;
        try {
            tmpRecorder.prepare();
            tmpRecorder.start();
        } catch (RuntimeException | IOException e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
            available = false;
        }
        tmpRecorder.release();
        return available;
    }
}
