package com.safe.myapp;

public class SafeStatus {
    boolean isAudioStarted;
    boolean isLocationStarted;

    public boolean isLocationStarted() {
        return isLocationStarted;
    }

    public void setLocationStarted(boolean locationStarted) {
        isLocationStarted = locationStarted;
    }

    public boolean isAudioStarted() {
        return isAudioStarted;
    }

    public void setAudioStarted(boolean audioStarted) {
        isAudioStarted = audioStarted;
    }


}
