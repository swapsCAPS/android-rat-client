package com.safe.myapp;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import org.apache.ftpserver.FtpServer;

public class SafeHeartbeat extends Thread {
    private static final int HEART_RATE = 10000;
    private boolean running;
    private SafeCommunications comms;
    private SafeLogger logger;

    public SafeHeartbeat(SafeLogger logger) {
        this.logger = logger;
        this.running = true;
    }

    public void setRunning(boolean running) {
        logger.write("Set heartbeat running to " + running);
        this.running = running;
    }

    @Override
    public void run() {
        Looper.prepare();
        try {
            logger.write("Heartbeat thread started");
            while (true) {
                Thread.sleep(HEART_RATE);
                if(running == true && comms.sending == false) {
                    Message msg = new Message();
                    msg.arg1 = 2;
                    comms.mHandler.sendMessage(msg);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.write("Heartbeat thread stopped");
    }
/*
    */

}
