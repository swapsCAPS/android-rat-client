package com.safe.myapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/*We should look into how to bind a Service
That is way more graceful and on top of that I believe, that you can bind to
the service again even if the app is restarted and the service is still running
Although if you invoke stopLocations in our SafeLocations 'wrapper' class right now
I think it'll find the service and return a list of locations*/


public class SafeLocationService extends Service {
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;

    private Context context;
    private SafeCommunications comms;
    public static ArrayList<Location> locationList = new ArrayList<Location>();


    @Override
    public void onCreate() {
        comms = SafeLocations.comms;
        context = getApplicationContext();
        SafeService.lLocStart = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SafeService.setbLocationStarted(true);
        commInterface("Location service started");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2500, 10, listener);
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


    /*Checks whether two providers are the same*/
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
        Geocoder gcd;
        StringBuilder locString;
        List<Address> addresses;

        int previousStatus = -1;

        public void onLocationChanged(final Location loc) {
            if (isBetterLocation(loc, previousBestLocation)) {
                // Add every reported location to the array, we can then iterate through to create the kml path
                locationList.add(loc);
                // Create a message for the server containing coords and a human readable address
                locString = new StringBuilder();
                locString.append("lat ").append(loc.getLatitude()).append(" lng ").append(loc.getLongitude());
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
                commInterface(locString.toString());
                SafeLocations.lastLocation = loc;
                previousBestLocation = loc;
            }
        }

        public void onProviderDisabled(String provider) {
            commInterface("Location provider " + provider + " disabled by the user");
        }

        public void onProviderEnabled(String provider) {
            commInterface("Location provider " + provider + " enabled by the user");
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
                commInterface("LocationListener status changed to " + provider + " " + strStatus);
            }
        }
    }

    // NetworkOnMainThread on Nougat...
    private void commInterface(String str) {
        if(Build.VERSION.SDK_INT < 24) {
            comms.say(str);
        } else {
            SafeLocations.logger.write("ANDROID VERSION >= 24");
        }
    }

}
