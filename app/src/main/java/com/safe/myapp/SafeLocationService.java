package com.safe.myapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Calendar;

/*
We should look into how to bind a Service
That is way more graceful and on top of that I believe, that you can bind to
the service again even if the app is restarted and the service is still running
Although if you invoke stopLocations in our SafeLocations 'wrapper' class right now
I think it'll find the service and return a list of locations
 */
public class SafeLocationService extends Service {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;

    private Context context;
    private SafeLogger logger;
    private SafeCommunications comms;
    public static ArrayList<Location> locationList = new ArrayList<Location>();

    int counter = 0;

    @Override
    public void onCreate() {
        logger = SafeLocations.logger;
        comms = SafeLocations.comms;
        SafeService.lLocStart = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SafeService.setbLocationStarted(true);
        commInterface("Location service started");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(listener);
        SafeService.lLocEnd = Calendar.getInstance().getTimeInMillis();
        SafeService.setbLocationStarted(false);
    }

    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            if (isBetterLocation(loc, previousBestLocation)) {
                // add every reported location to the array, we can then iterate through to create the kml path
                locationList.add(loc);
                commInterface("lat " + loc.getLatitude() + " lng " + loc.getLongitude());
                SafeLocations.lastLocation = loc;
            }
        }

        public void onProviderDisabled(String provider) {
            commInterface("Location provider " + provider + " disabled by the user");
        }

        public void onProviderEnabled(String provider) {
            commInterface("Location provider " + provider + " disabled by the user");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            String strStatus;
            switch (status) {
                case 0:
                    strStatus = "OUT_OF_SERVICE";
                    break;
                case 1:
                    strStatus = "TEMPORARILY_UNAVAILABLE";
                    break;
                case 2:
                    strStatus = "AVAILABLE";
                    break;
                default:
                    strStatus = "null";
            }
            commInterface("LocationListener status changed to " + strStatus);
        }
    }

    private void commInterface(String str) {
        if(comms != null) {
            comms.say(str);
        }
    }
}