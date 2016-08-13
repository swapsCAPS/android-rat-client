package com.safe.myapp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class SafeLogger {

    private static final int MAX_LOG_FILE_SIZE = 2 * 1024 * 1024;
    private static final String LOG = "SafeService";
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss", Locale.US);

    private Context context;
    private File file;
    private String filename = "error.log";

    public SafeLogger(Context context) {
        this.context = context;
        // TOdo CHeck if external storage is available
        // And then use it
        // file = new File(Environment.getExternalStorageDirectory(), filename);
        file = new File(context.getFilesDir(), filename);
    }

    public void write(String string) {
        if (SafeService.BOOL_DEBUG) {
            Log.d(LOG, string);
            try {
                String timestamp = formatter.format(Calendar.getInstance().getTime());
                string = timestamp + "\r\n" + string + "\r\n\r\n";
                FileOutputStream outputStream;
                // TODO Fix this!
                // Extremely ugly way to keep logfile small
                if (file.length() > MAX_LOG_FILE_SIZE) {
                    file.delete();
                }
                // append line to file
                outputStream = context.openFileOutput(filename, Context.MODE_APPEND);
                outputStream.write(string.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public File logfile() {
        return this.file;
    }

}
