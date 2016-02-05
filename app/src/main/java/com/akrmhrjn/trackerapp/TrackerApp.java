package com.akrmhrjn.trackerapp;

import android.app.Application;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by akrmhrjn on 1/23/16.
 */
public class TrackerApp extends Application {

    public static TrackerApp app;
    public List<LatLng> travelledPath;
    public boolean trackStatus = false;


    @Override
    public void onCreate() {
        super.onCreate();

        app = this;
    }
}
