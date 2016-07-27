package com.safe.myapp;

import android.content.Context;
import android.provider.Settings;

import org.apache.ftpserver.FtpServer;

public class SafeHeartbeat extends Thread {
    private static final int HEART_RATE = 10000;
    private boolean running;
    private SafeCommunications comms;
    private SafeLogger logger;
    private Context context;
    private String simpleDeviceId;
    private FtpServer server;

    public SafeHeartbeat(SafeCommunications comms, SafeLogger logger, Context context) {
        this.comms = comms;
        this.logger = logger;
        this.context = context;
        this.running = true;
        simpleDeviceId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public void setRunning(boolean running) {
        logger.write("Setting running to " + running);
        this.running = running;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        try {
            while (running) {
                comms.say("♥");
                Thread.sleep(HEART_RATE);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.write("Heartbeat stopped");
    }
/*
    private void startServer() {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(3000);
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);
        serverFactory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());
        BaseUser anon = new BaseUser();
        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        anon.setName("anonymous");
        anon.setAuthorities(authorities);
        anon.setHomeDirectory("/");
        try {
            serverFactory.getUserManager().save(anon);
        } catch (FtpException e) {
            e.printStackTrace();
        }
        serverFactory.addListener("default", factory.createListener());
        server = serverFactory.createServer();
        try {
            server.start();
            ftpServerStarted = true;
        } catch (FtpException e) {
            e.printStackTrace();
            ftpServerStarted = false;
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            ftpServerStarted = false;
        }
    }*/

}
