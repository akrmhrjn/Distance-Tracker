package com.akrmhrjn.trackerapp.Location;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.akrmhrjn.trackerapp.TrackerApp;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;


public class LocationService extends Service implements IGpsHelper {

    private Thread traceThread;


    double lastLat = 0.0;
    double lastLng = 0.0;

    ArrayList<LatLng> path;

    GPS gps;


    public final static double AVERAGE_RADIUS_OF_EARTH = 6371;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("Service Started");

        path = new ArrayList<>();
        startLocationService();   // making the application work in background

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gps = new GPS(LocationService.this);   //start GPS Services to continously access user location coordinates

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("Service Stopped");
        gps.stopGPS();

    }

    private void startLocationService() {
        final LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locManager == null) {
            Toast.makeText(this, "No location service found!",
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    @Override
    public void locationChanged(double longitude, double latitude) {
        Log.d("Service location", "lat: " + latitude);

        double lat = latitude;
        double lng = longitude;

        double distance = calculateDistance(lastLat, lastLng, lat, lng);
        System.out.println("Service Distance: " + distance);

        if (distance > 5) {
            System.out.println("Service Location traced. Distance: " + distance);
            Location location = new Location("location");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            uploadTrace(lat, lng);

            lastLat = lat;
            lastLng = lng;
        }
    }

    @Override
    public void displayGPSSettingsDialog() {

    }

    private void uploadTrace(final double lat, final double lng) {
        Log.d("Service Trace", "uploadTrace start...");

        if (traceThread != null) {
            Log.d("Trace", "Old traceThread running.");
            return;
        }

        traceThread = new Thread() {
            public void run() {
                LatLng latLng = new LatLng(lat, lng);
                path.add(latLng);
                TrackerApp.app.travelledPath = path;
                traceThread = null;
            }
        };
        traceThread.start();
    }


    // Haversine formula to calulate distance
    public double calculateDistance(double Lat1, double Lng1,
                                    double Lat2, double Lng2) {

        double latDistance = Math.toRadians(Lat1 - Lat2);
        double lngDistance = Math.toRadians(Lng1 - Lng2);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(Lat1)) * Math.cos(Math.toRadians(Lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = AVERAGE_RADIUS_OF_EARTH * c * 1000; //returns in meter

        return d;
    }
}
