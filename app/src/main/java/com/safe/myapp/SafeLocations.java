package com.safe.myapp;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SafeLocations {

    private static LocationManager locationManager;
    private static LocationListener listener;
    public static Location lastLocation = null;

    private SafeStatus status;
    private SafeCommunications comms;
    private SafeLogger logger;
    private Context context;
    private String simpleID;

    public SafeLocations(Context context, SafeStatus status, SafeCommunications comms, SafeLogger logger, String simpleID) {
        this.context = context;
        this.status = status;
        this.comms = comms;
        this.logger = logger;
        this.simpleID = simpleID;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
    }

    public void getSingleLocation() {
        logger.write("getSingleLocation");
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper());
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            String packageName = context.getApplicationContext().getPackageName() + ".SafeLocationService";
            if (packageName.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public class MyLocationListener implements LocationListener {
        Geocoder gcd;
        StringBuilder locString;
        List<Address> addresses;

        int previousStatus = -1;

        public void onLocationChanged(final Location loc) {
            // Create a message for the server containing coords and a human readable address
            locString = new StringBuilder();
            locString.append("lat ")
                    .append(loc.getLatitude())
                    .append(" lng ")
                    .append(loc.getLongitude())
                    .append(" mode: ")
                    .append(loc.getProvider());
            gcd = new Geocoder(context.getApplicationContext(),
                    Locale.getDefault());
            // Retrieve human readable stuff from coords
            try {
                addresses = gcd.getFromLocation(
                        loc.getLatitude(),
                        loc.getLongitude(), 1);
                locString.append(" ").append(addresses.get(0).getLocality());
                locString.append(" ").append(addresses.get(0).getPostalCode());
                locString.append(" ").append(addresses.get(0).getThoroughfare());
                locString.append(" ").append(addresses.get(0).getSubThoroughfare());
            } catch (IOException e) {
                e.printStackTrace();
            }
            logger.write(locString.toString());
            try {
                comms.sayLocation(loc);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            lastLocation = loc;
        }

        public void onProviderDisabled(String provider) {
            logger.write("Location provider " + provider + " disabled by the user");
        }

        public void onProviderEnabled(String provider) {
            logger.write("Location provider " + provider + " enabled by the user");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String strStatus;
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    strStatus = "OUT_OF_SERVICE";
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    strStatus = "TEMPORARILY_UNAVAILABLE";
                    break;
                case LocationProvider.AVAILABLE:
                    strStatus = "AVAILABLE";
                    break;
                default:
                    strStatus = "null";
            }
            // Only log if status actually changed. We are getting a lot of chatter otherwise.
            if(status != previousStatus) {
                previousStatus = status;
                logger.write("LocationListener status changed to " + provider + " " + strStatus);
            }
        }
    }
}
