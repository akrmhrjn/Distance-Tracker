package com.akrmhrjn.trackerapp.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akrmhrjn.trackerapp.Location.LocationService;
import com.akrmhrjn.trackerapp.R;
import com.akrmhrjn.trackerapp.TrackerApp;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    @ViewById
    Toolbar toolbar;

    @ViewById
    RelativeLayout loader;

    @ViewById
    TextView tvTrack;

    MapFragment mapFragment;

    Location location;
    LocationManager locationManager;

    double toLat, toLong, fromLat, fromLong;
    MarkerOptions toMarker, fromMarker;

    GoogleMap map;


    public final static double AVERAGE_RADIUS_OF_EARTH = 6371;

    @AfterViews
    void init() {
        initToolbar();
        mapFragment = (MapFragment) this.getFragmentManager().findFragmentById(R.id.map);

        toMarker = new MarkerOptions();
        fromMarker = new MarkerOptions();

        //To check whether Play Services are available or not
        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS is Enabled on your device", Toast.LENGTH_SHORT).show();
        } else {
            showGPSDisabledAlertToUser();
        }

        map = mapFragment.getMap();
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMyLocationEnabled(false);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        getCurrentLocation();

    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS is disabled on your device. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Enable GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                //Intent that shows interface to start GPS in user's device
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        });
        alertDialogBuilder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    //get Address from longitude and latitude
    private String getAddress(double latitude, double longitude) {
        StringBuilder result = new StringBuilder();
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                if (address.getSubLocality() != null)
                    result.append(address.getSubLocality()).append(", "); //Gets Sublocality
                result.append(address.getLocality()).append(", "); //gets Locality
                result.append(address.getCountryName()); //Gets country name
            }
        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        } catch (NullPointerException err) {
            Log.e("tag", err.getMessage());
        }
        return result.toString();
    }

    //Method to get current Location of user
    public void getCurrentLocation() {
        map.clear();
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            location = locationManager.getLastKnownLocation(bestProvider);

            if (location != null) {
                setAddress(location.getLatitude(), location.getLongitude());
            } else {
                Toast.makeText(this, "Waiting for location..", Toast.LENGTH_LONG).show();
                locationManager.requestLocationUpdates(bestProvider, 3000, 0, this);   //new location is updated after every 3000 milliseconds
            }
        }
    }

    //Method used to set address once coordinates accessed
    private void setAddress(double lat, double lng) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            utilizeLocation(lat, lng, geocoder);
        } catch (NullPointerException err) {
            //gives null value when app is run for the first time
        }
    }


    private void utilizeLocation(double lat, double lng, Geocoder geocoder) {
        StringBuilder result = new StringBuilder();
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);

        } catch (IOException e) {
            Log.e("tag", e.getMessage());
        } catch (NullPointerException err) {
            Log.e("tag", err.getMessage());
        }

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            if (address.getSubLocality() != null) {
                result.append(address.getSubLocality()).append(", ");
            }
            result.append(address.getLocality()).append(", ");
            result.append(address.getCountryName());
        }

        String address = result.toString();
        LatLng latLng = new LatLng(lat, lng);
        MarkerOptions marker = new MarkerOptions();
        marker.position(latLng);
        marker.title(address);
        map.addMarker(marker);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {


    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        if (location != null) {
            setAddress(latitude, longitude);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }


    // Haversine formula used to calculate distance between two google coordinates
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

    @Click(R.id.btnStart)
    void startTrack() {
        if(TrackerApp.app.travelledPath != null) {
            map.clear();
            TrackerApp.app.travelledPath.clear();
            getCurrentLocation();
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (!TrackerApp.app.trackStatus) {
                Toast.makeText(this, "Tracking started.", Toast.LENGTH_SHORT).show();
                tvTrack.setVisibility(View.VISIBLE);
                TrackerApp.app.trackStatus = true;
                Intent serviceIntent = new Intent(MainActivity.this, LocationService.class);
                MainActivity.this.startService(serviceIntent);
            } else {
                Toast.makeText(this, "Tracking already started.", Toast.LENGTH_SHORT).show();
            }
        } else {
            showGPSDisabledAlertToUser();
        }
    }


    @Click(R.id.btnStop)
    void stopTrack() {
        if (TrackerApp.app.trackStatus) {
            Toast.makeText(this, "Tracking stopped.", Toast.LENGTH_SHORT).show();
            tvTrack.setVisibility(View.GONE);
            TrackerApp.app.trackStatus = false;
            Intent serviceIntent = new Intent(MainActivity.this, LocationService.class);
            MainActivity.this.stopService(serviceIntent);
        } else {
            Toast.makeText(this, "First start your tracking.", Toast.LENGTH_SHORT).show();
        }
    }

    @Click(R.id.btnCurLoc)
    void showCurrentLocation() {
        getCurrentLocation();
    }

    @Click(R.id.btnShow)
    void showTrack() {

        if (TrackerApp.app.travelledPath == null) {
            System.out.println("NOnon");
            Toast.makeText(this, "No tracking has been saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TrackerApp.app.trackStatus) {
            loader.setVisibility(View.VISIBLE);
            drawAndCalculate();

        } else {
            Toast.makeText(this, "First stop your tracking.", Toast.LENGTH_SHORT).show();
        }


    }

    @Background
    void drawAndCalculate() {
        int size = TrackerApp.app.travelledPath.size();
        toLat = TrackerApp.app.travelledPath.get(0).latitude;
        toLong = TrackerApp.app.travelledPath.get(0).longitude;

        fromLat = TrackerApp.app.travelledPath.get(size - 1).latitude;
        fromLong = TrackerApp.app.travelledPath.get(size - 1).longitude;

        double distanceTravelled = calculateDistance(fromLat, fromLong, toLat, toLong);
        afterDrawAndCalculate(distanceTravelled);
    }

    @UiThread
    void afterDrawAndCalculate(double distance) {
        getCurrentLocation();
        String toAddress = getAddress(toLat, toLong);
        String fromAddress = getAddress(fromLat, fromLong);

        LatLng fromPosition = new LatLng(fromLat, fromLong);
        LatLng toPosition = new LatLng(toLat, toLong);

        fromMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        fromMarker.position(fromPosition);
        fromMarker.draggable(false);
        fromMarker.title(fromAddress);
        map.addMarker(fromMarker);
        toMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        toMarker.position(toPosition);
        toMarker.draggable(false);
        toMarker.title(toAddress);
        map.addMarker(toMarker);

        //draw line
        PolylineOptions polyLineOptions = new PolylineOptions();
        polyLineOptions.addAll(TrackerApp.app.travelledPath);
        polyLineOptions.width(2);
        polyLineOptions.color(Color.BLUE);
        map.addPolyline(polyLineOptions);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle("Travel Info:")
                .setMessage("You travelled " + roundTwoDecimals(distance) + " m.\n\n" +
                        "Starting Position:\n" +
                        "Latitude: " + fromLat + "\n" +
                        "Longitude: " + fromLong + "\n\n" +
                        "Ending Position:\n" +
                        "Latitude: " + toLat + "\n" +
                        "Longitude: " + toLong + "\n")
                .setCancelable(true);
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
        loader.setVisibility(View.GONE);
    }


    double roundTwoDecimals(double d) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Double.valueOf(twoDForm.format(d));
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
}
