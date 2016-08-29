package com.safe.myapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;

public class SafeService extends Service {

    public static final boolean BOOL_DEBUG = true;
    public static final String VERSION = "0.7";
    public static final String HTTP_SERVER = "http://92.111.66.145/";
    public static final int HTTP_PORT = 13002;
    private static String simpleID;

    private static SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WowSuchSharedPreferencesVery1337";

    private SafeStatus status;
    private SafeLocations locs;
    private SafeCommunications comms;
    private SafeFTPServer ftpServer;
    private SafeCommands commands;

    private SafeAudio audio;
    private SafeLogger logger;

    private boolean pollServer;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if(BOOL_DEBUG){
            simpleID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID) + "DEBUG";
        } else {
            simpleID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }

        // Initialize our objects
        status = new SafeStatus();
        logger = new SafeLogger(getApplicationContext());
        comms = new SafeCommunications(getApplicationContext(), status, logger, simpleID);
        locs = new SafeLocations(getApplicationContext(), status, comms, logger, simpleID);
        audio = new SafeAudio(getApplicationContext(), status, comms, logger);
        ftpServer = new SafeFTPServer(getApplicationContext(), comms, logger);

        pollServer = true;

        logger.write("onCreate finished");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.write("onStartCommand called");
        new Thread(){
            public void run(){
                Looper.prepare();
                while(pollServer) {
                    comms.pollServer();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        audio.stopRecording();
        // locs.stopLocations();
        savePrefs();
    }

    private void savePrefs() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.apply();
    }

    private void loadSavedPrefs() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
    }
}
