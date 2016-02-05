package com.akrmhrjn.trackerapp.Location;

public interface IGpsHelper {
    public void locationChanged(double longitude, double latitude);
    public void displayGPSSettingsDialog();
}