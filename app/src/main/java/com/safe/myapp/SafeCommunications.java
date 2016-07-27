package com.safe.myapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class SafeCommunications {

    private Context context;
    private DataOutputStream out;
    private SafeLogger logger;
    private String simpleID;

    public SafeCommunications(Context context, SafeLogger logger, DataOutputStream out, String simpleID) {
        this.context = context;
        this.logger = logger;
        this.out = out;
        this.simpleID = simpleID;
    }

    public void handShake() {
        WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid.equals("<unknown ssid>")) {
            ssid += " probably on mobile data";
        } else {
            ssid += " state " + wifiInfo.getSupplicantState();
        }

        // the server expects a handshake with simple strings delimited by \r\n
        StringBuilder sb = new StringBuilder();
        sb.append(simpleID);
        if (SafeService.BOOL_DEBUG) sb.append("DEBUG");
        sb.append("\r\n");
        sb.append(SafeService.VERSION);
        sb.append("\r\n");
        sb.append(context.getApplicationContext().getPackageName());
        sb.append("\r\n");
        sb.append(ssid);
        sb.append("\r\n");
        sb.append(SafeService.isbAudioStarted());
        sb.append("\r\n");
        sb.append(SafeService.isbLocationStarted());
        sb.append("\r\n");
        String handshake = sb.toString();

        try {
            synchronized (out) {
                logger.write("handshaking");
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
            synchronized (out) { // wait for any other uploads or messages
                try {
                    // notify server we are sending a message
                    out.writeInt(SafeService.MESSAGE);
                    // notify server of the size of the message
                    out.writeInt(message.getBytes().length);
                    out.flush();

                    // convert message to bytes and send
                    byte[] messageBytes = message.getBytes();
                    out.write(messageBytes, 0, messageBytes.length);
                    out.flush();
                    logger.write("Sent message " + message);
                } catch (IOException e) {
                    logger.write("IOException, could not send message: " + message);
                }
            }
        }
    }

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
                        say("sending " + file + " " + file.length());
                        // if it is a directory add all files in it to our list
                    } else if (file.isDirectory()) {
                        say(file + " is a directory, sending all files within it, skipping folders");
                        for (File f : file.listFiles()) {
                            if (f.isFile()) {
                                files.add(f);
                                say("sending " + f + " " + f.length());
                            }
                        }
                    } else {
                        say(fileLoc + " is not a valid file");
                    }
                } else {
                    say(fileLoc + "does not exist");
                }
            }
            // upload files to server
            upload(files.toArray(new File[files.size()]));
        } catch (NullPointerException e) {
            say("w00ps");
        }
    }

    public void upload(File... files) {
        for (final File file : files) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        synchronized (out) {
                            if (!file.isFile()) {
                                say(file + " is not a file");
                                return;
                            }
                            // get bytes from file (filename has been prepended already)
                            byte[] bFile = fileToByte(file);
                            if (bFile == null){
                                return;
                            }
                            // notify server we are sending a file
                            out.writeInt(SafeService.FILE);
                            // notify server of the size
                            out.writeInt(bFile.length);
                            out.flush();
                            // send content
                            byte[] fileBytes = fileToByte(file);
                            out.write(fileBytes, 0, fileBytes.length);
                            out.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        logger.write(Log.getStackTraceString(e));
                    }
                }
            }).start();
        }
    }

    private byte[] fileToByte(File file) throws IOException {
        int HEADER_NAME_SIZE = 128;
        // check file size
        int fileSize = (int) file.length();
        if(fileSize > 128 * 1024 * 1024){
            logger.write("File too large " + fileSize + " bytes " + (fileSize / 1024 / 1024) + "MB");
            say("File too large "  + fileSize + " bytes " + (fileSize / 1024 / 1024) + "MB");
            return null;
        }
        // the fileContent bytes will contain our file contents
        byte[] fileContent = new byte[(int) file.length()];
        // read bytes from file
        BufferedInputStream bin;
        bin = new BufferedInputStream(new FileInputStream(file)); // opens connection to the file in the file system
        bin.read(fileContent);
        bin.close();

        // read filename to bytes, we will add this as a header
        byte[] bFileName = file.getName().getBytes("UTF-8");

        // check if filename is not too large
        if (bFileName.length > HEADER_NAME_SIZE) {
            logger.write("Filename too long, sending file without name");
            say("Filename too long, sending file without name");
            return fileContent;
        }

        // copy bytes to header with empty space
        byte[] headerFileName = new byte[HEADER_NAME_SIZE];
        System.arraycopy(bFileName, 0, headerFileName, 0, bFileName.length);

        // create a space holder for our headers and our content, 8 bytes + 128 bytes + content
        byte[] builtFile = new byte[headerFileName.length + fileContent.length];
        // copy name header to start of builtFile with a size of 128
        System.arraycopy(headerFileName, 0, builtFile, 0, headerFileName.length);
        // then copy content to built file after the header
        System.arraycopy(fileContent, 0, builtFile, headerFileName.length, fileContent.length);
        return builtFile;
    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }
}
