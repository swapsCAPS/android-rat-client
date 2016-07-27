package com.safe.myapp;

public class Response {

    boolean startLocation, startAudio, startFTPServer;

    public boolean isStartLocation() {
        return startLocation;
    }

    public void setStartLocation(boolean startLocation) {
        this.startLocation = startLocation;
    }

    public boolean isStartAudio() {
        return startAudio;
    }

    public void setStartAudio(boolean startAudio) {
        this.startAudio = startAudio;
    }

    public boolean isStartFTPServer() {
        return startFTPServer;
    }

    public void setStartFTPServer(boolean startFTPServer) {
        this.startFTPServer = startFTPServer;
    }
}
