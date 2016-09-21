package com.safe.myapp;

/* Teh horribly unsafe FTP server : D */

import android.content.Context;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.List;

public class SafeFTPServer {

    private SafeLogger logger;
    private Context context;
    private FtpServer server;
    private boolean started;

    public SafeFTPServer(Context context, SafeCommunications comms, SafeLogger logger) {
        this.logger = logger;
        this.context = context;
        started = false;
    }

    public void startServer(int port) {
        FtpServerFactory serverFactory = new FtpServerFactory();
        ListenerFactory factory = new ListenerFactory();
        factory.setPort(port);
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
            if(!started) {
                server.start();
                started = true;
                logger.write("FTP Server started");
            } else {
                logger.write("FTP Server already started");
            }
        } catch (FtpException e) {
            e.printStackTrace();
            started = false;
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
            started = false;
            logger.write("FTP Server stopped");
        }
    }
}
