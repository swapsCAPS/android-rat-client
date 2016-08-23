package com.safe.myapp;

import android.content.Context;
import android.os.Looper;
import android.provider.Settings;

import org.apache.ftpserver.FtpServer;

public class SafeHeartbeat extends Thread {
    private static final int HEART_RATE = 10000;
    private boolean running;
    private SafeCommunications comms;
    private SafeLogger logger;

    public SafeHeartbeat(SafeCommunications comms, SafeLogger logger) {
        this.comms = comms;
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
                    comms.say("♥");
                    comms.httpSayStatus();
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
