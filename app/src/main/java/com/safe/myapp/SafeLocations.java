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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

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
    private static final String STR_NAME_KML = "path_";
    private static final String STR_EXT_KML = ".kml";
    private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_z", Locale.US);

    private static LocationManager locationManager;
    private static LocationListener listener;
    public static Location lastLocation = null;
    public static ArrayList<Location> locationList = new ArrayList<Location>();

    public static SafeCommunications comms;
    public static SafeLogger logger;
    private Context context;
    private String simpleID;

    public SafeLocations(Context context, SafeCommunications comms, SafeLogger logger, String simpleID) {
        this.context = context;
        this.comms = comms;
        this.logger = logger;
        this.simpleID = simpleID;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
    }

    public void startLocations() {
        if (!isLocationServiceRunning()) {
            Intent i = new Intent(context, SafeLocationService.class);
            context.startService(i);
        } else {
            commInterface("Location service already started");
        }
    }

    public void stopLocations() {
        if (isLocationServiceRunning()) {
            // syncGet the recorded locations from the service
            locationList = SafeLocationService.locationList;
            Intent i = new Intent(context, SafeLocationService.class);
            context.stopService(i);
            comms.upload(kmlFile());
            commInterface("Location service stopped");
            locationList.clear();
            // set end time here and in SafeLocationService
            // because it may be stopped by the system
            // always set start time in Service
            SafeService.lLocEnd = Calendar.getInstance().getTimeInMillis();
        } else {
            commInterface("Location service was not started");
        }
    }

    public void getSingleLocation() {
        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, context.getMainLooper());
    }

    public String getLastKnownAddress() {
        // check if we have a recent location:
        if (lastLocation == null) {
            return "No recent location found. Have you activated Location recording?";
        } else {
            // we have last location
            Geocoder gcd;
            StringBuilder sb = null;
            List<Address> addresses;
            Address address;
            try {
                gcd = new Geocoder(context.getApplicationContext(),
                        Locale.getDefault());
                // retrieve human readable stuff from coords
                addresses = gcd.getFromLocation(
                        lastLocation.getLatitude(),
                        lastLocation.getLongitude(), 1);
                if (addresses.size() > 0) {
                    address = addresses.get(0);
                    sb = new StringBuilder();
                    sb.append("Country name:   ").append(address.getCountryName());
                    sb.append("\n");
                    sb.append("Admin area:     ").append(address.getAdminArea());
                    sb.append("\n");
                    sb.append("Sub admin area: ").append(address.getSubAdminArea());
                    sb.append("\n");
                    sb.append("Locality:       ").append(address.getLocality());
                    sb.append("\n");
                    sb.append("Sub locality:   ").append(address.getSubLocality());
                    sb.append("\n");
                    sb.append("Postal code:    ").append(address.getPostalCode());
                    sb.append("\n");
                    sb.append("Thorough fare:  ").append(address.getThoroughfare());
                    sb.append("\n");
                    sb.append("Sub thrgh fare: ").append(address.getSubThoroughfare());
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.write(Log.getStackTraceString(e));
                return null;
            }
            if (sb != null) {
                return "\nAddress based on last known coords:\n" + sb.toString().trim();
            } else return "";
        }
    }

    // create and save the .kml file
    private File kmlFile() {
        // create filename
        String strFileName = STR_NAME_KML
                + formatter.format(Calendar.getInstance().getTime())
                + STR_EXT_KML;
        try {
            // write to disk
            FileOutputStream outputStream = context.openFileOutput(strFileName, Context.MODE_PRIVATE);
            outputStream.write(kmlContent().getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            logger.write(Log.getStackTraceString(e));
        }
        // return file location
        return new File(context.getFilesDir(), strFileName);
    }

    private String kmlContent() {
        Date calStart;
        calStart = dateFromMillis(SafeService.lLocStart);
        Date calEnd = dateFromMillis(SafeService.lLocEnd);
        if (locationList.size() <= 0 || locationList == null) {
            return "";
        }
        SimpleDateFormat kmlDate = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ", Locale.US);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        sb.append("    <Document>\n");
        sb.append("        <name>").append(simpleID).append("</name>\n");
        sb.append("        <Style id=\"redLine\">\n");
        sb.append("            <LineStyle>\n");
        sb.append("                <color>7f0000ff</color>\n");
        sb.append("                <width>5</width>\n");
        sb.append("                <gx:labelVisibility>1</gx:labelVisibility>\n");
        sb.append("            </LineStyle>\n");
        sb.append("        </Style>\n");
        sb.append("        <Placemark>\n");
        sb.append("            <name>").append(simpleID).append("</name>\n");
        sb.append("            <TimeSpan>\n");
        sb.append("                <begin>").append(kmlDate.format(calStart)).append("</begin>\n");
        sb.append("                <end>").append(kmlDate.format(calEnd)).append("</end>\n");
        sb.append("            </TimeSpan>\n");
        sb.append("            <styleUrl>#redLine</styleUrl>\n");
        sb.append("            <LineString>\n");
        sb.append("                <coordinates>\n");
        for (Location loc : locationList) {
            sb.append("                    ");
            sb.append(loc.getLongitude());
            sb.append(",");
            sb.append(loc.getLatitude());
            sb.append("\n");
        }
        sb.append("                </coordinates>\n");
        sb.append("            </LineString>\n");
        sb.append("        </Placemark>\n");
        sb.append("        <Placemark>\n");
        sb.append("            <name>Start</name>\n");
        sb.append("            <TimeStamp>\n");
        sb.append("                <when>").append(kmlDate.format(calStart)).append("</when>\n");
        sb.append("            </TimeStamp>\n");
        sb.append("            <Point>\n");
        sb.append("                <altitudeMode>clampToGround</altitudeMode>\n");
        sb.append("                <coordinates>");
        sb.append(locationList.get(0).getLongitude());
        sb.append(",");
        sb.append(locationList.get(0).getLatitude());
        sb.append("</coordinates>\n");
        sb.append("            </Point>\n");
        sb.append("        </Placemark>\n");
        sb.append("        <Placemark>\n");
        sb.append("            <name>End</name>\n");
        sb.append("            <TimeStamp>\n");
        sb.append("                <when>").append(kmlDate.format(calEnd)).append("</when>\n");
        sb.append("            </TimeStamp>\n");
        sb.append("            <Point>\n");
        sb.append("                <altitudeMode>clampToGround</altitudeMode>\n");
        sb.append("                <coordinates>");
        sb.append(locationList.get(locationList.size() - 1).getLongitude());
        sb.append(",");
        sb.append(locationList.get(locationList.size() - 1).getLatitude());
        sb.append("</coordinates>\n");
        sb.append("            </Point>\n");
        sb.append("        </Placemark>\n");
        sb.append("    </Document>\n");
        sb.append("</kml>");
        logger.write("" + kmlDate.format(calStart));
        return sb.toString();
    }

    private Date dateFromMillis(long millis){
        return new Date(millis);
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.safe.myapp.SafeLocationService".equals(service.service.getClassName())) {
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
            commInterface(locString.toString());
            lastLocation = loc;
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
            logger.write("ANDROID VERSION >= 24");
        }
    }
}
