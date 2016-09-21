package com.safe.myapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import cz.msebera.android.httpclient.Header;

public class SafeCommunications {

    private Context context;
    private DataOutputStream out;
    private SafeLogger logger;
    private String simpleID;
    public boolean sending;

    public SafeCommunications(Context context, SafeLogger logger, DataOutputStream out, String simpleID) {
        this.context = context;
        this.logger = logger;
        this.out = out;
        this.simpleID = simpleID;
        sending = false;
    }

    public void handShake() {
        // the server expects a handshake with simple strings delimited by \r\n
        StringBuilder sb = new StringBuilder();
        sb.append(simpleID);
        sb.append("\r\n");
        sb.append(SafeService.VERSION);
        sb.append("\r\n");
        sb.append(context.getApplicationContext().getPackageName());
        sb.append("\r\n");
        String handshake = sb.toString();

        try {
            synchronized (out) {
                logger.write("Handshaking");
                // notify the server we are going to send a handshake
                out.writeInt(SafeService.HANDSHAKE);
                // notify of size
                out.writeInt(handshake.getBytes().length);
                // send handshake data
                out.write(handshake.getBytes(), 0, handshake.getBytes().length);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
        }
    }

    public void say(String message) {
        if (out != null) {
            sending = true;
            synchronized (out) { // wait for any other uploads or messages
                try {
                    // notify server we are sending a message
                    out.writeInt(SafeService.MESSAGE);
                    // notify server of the size of the message
                    out.writeInt(message.getBytes().length);

                    // convert message to bytes and send
                    byte[] messageBytes = message.getBytes();
                    out.write(messageBytes, 0, messageBytes.length);
                    out.flush();
                    logger.write("Sent message " + message);
                } catch (IOException e) {
                    logger.write("IOException, could not send message: " + message);
                }
            }
            sending = false;
        }
    }

    public void downloadLast(int amount, String fileLoc) {
        // list of files to be uploaded to server
        ArrayList<File> files = new ArrayList<>();
        try {
            // Get a file object from each string
            File file = new File(Environment.getExternalStorageDirectory(), fileLoc);
            // Check if the file exists
            if (file.exists()) {
                if (file.isDirectory()) {
                    say(file + " is a directory. Sending last [" + amount + "] files sorted on creation date. Skipping folders");

                    // Sort all files and dirs on last modified date
                    File[] sorted = file.listFiles();
                    Arrays.sort(sorted, new Comparator<File>() {
                        public int compare(File f1, File f2) {
                            return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                        }
                    });

                    // Get all files skip dirs
                    for (File f : sorted) {
                        if (f.isFile()) {
                            files.add(f);
                        }
                    }

                    // Check amount to download against amount of files in dir
                    if (amount > files.size()) {
                        amount = files.size();
                        say("Amount to download greater than files in directory, sending all files");
                    }

                    // Get last 'amount' of files
                    ArrayList<File> filesToSend = new ArrayList<>();
                    StringBuilder ls = new StringBuilder();
                    ls.append("\r\n");
                    for (int i = files.size() - amount; i < files.size(); i++) {
                        File f = files.get(i);
                        filesToSend.add(f);
                        ls.append(f);
                        ls.append("\r\n");
                    }

                    // Upload files to server
                    say("Sending " + ls.toString());
                    upload(filesToSend.toArray(new File[filesToSend.size()]));
                } else {
                    say(fileLoc + " is not a directory");
                }
            } else {
                say(fileLoc + " does not exist");
            }
        } catch (NullPointerException e) {
            say("NullPointerException when trying to download file");
        }
    }

    public void download(String fileLoc) {
        // list of files to be uploaded to server
        ArrayList<File> files = new ArrayList<>();
        try {
            // Get a file object from each string
            File file = new File(Environment.getExternalStorageDirectory(), fileLoc);
            // Check if the file exists
            if (file.exists()) {
                // if it exists and is a file add the file to our list
                if (file.isFile()) {
                    files.add(file);
                    // say("Sending " + file + " " + file.length());
                    // if it is a directory add all files in it to our list
                } else if (file.isDirectory()) {
                    say(file + " is a directory. Sending all files within it. Skipping folders");
                    StringBuilder ls = new StringBuilder();
                    ls.append("\r\n");
                    for (File f : file.listFiles()) {
                        if (f.isFile()) {
                            files.add(f);
                            ls.append(f);
                            ls.append("\r\n");
                        }
                    }
                    // say("Sending " + ls.toString());
                    // upload files to server
                    upload(files.toArray(new File[files.size()]));
                } else {
                    say(fileLoc + " is not a valid file");
                }
            } else {
                say(fileLoc + " does not exist");
            }
        } catch (NullPointerException e) {
            say("NullPointerException when trying to download file");
        }
    }

    public void upload(final File... files) {
        new Thread() {
            @Override
            public void run() {
                // Check if file server is online
                say("Upload started");
                SafeRestClient.syncGet("/serverStatus", null, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        // File server is online. Upload the files
                        for (final File file : files) {
                            RequestParams fileCheck = new RequestParams();
                            fileCheck.put("fileName", file.getName());
                            fileCheck.put("clientPath", file.getParentFile().getPath());
                            fileCheck.put("clientId", simpleID);
                            fileCheck.put("fileSize", file.length());
                            // Check if the server wants this file
                            SafeRestClient.syncGet("/acceptFile/" + simpleID + "/", fileCheck, new AsyncHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                    // Server wants the file. Upload it
                                    RequestParams params = new RequestParams();
                                    say(file.getName() + " Does not exist on server. Uploading...");
                                    try {
                                        params.put("clientPath", file.getParentFile().getPath());
                                        params.put("clientId", simpleID);
                                        params.put("tehAwesomeFile", file);
                                    } catch (FileNotFoundException e) {
                                        say("Could not find file: " + file.getName());
                                    }
                                    SafeRestClient.syncPost("/postFile/" + simpleID + "/", params, new AsyncHttpResponseHandler() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                            // Server has received the file
                                            try {
                                                if (responseBody != null) {
                                                    say(new String(responseBody, "UTF-8") + " Received");
                                                }
                                            } catch (UnsupportedEncodingException e) {
                                                e.printStackTrace();
                                            }
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                            // Upload failed, try to notify control server
                                    /*try {
                                        if (responseBody != null) {
                                            say(new String(responseBody, "UTF-8"));
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }*/
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                    // The server did not accept the file
                                    try {
                                        if (responseBody != null) {
                                            //say(new String(responseBody, "UTF-8"));
                                            logger.write(new String(responseBody, "UTF-8"));
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                        // All files were uploaded
                        say("Upload completed");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        // File server was not online. Try to notify the control server
                        say("Could not reach file server at: " + SafeService.HTTP_SERVER + ":" + SafeService.HTTP_PORT);
                    }
                });
            }
        }.start();

    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }
}
