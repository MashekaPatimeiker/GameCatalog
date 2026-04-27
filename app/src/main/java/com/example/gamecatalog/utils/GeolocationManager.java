package com.example.gamecatalog.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class GeolocationManager {
    private static final String TAG = "GeolocationManager";
    private static final long MIN_TIME_MS = 5000;
    private static final float MIN_DISTANCE_M = 10;

    private final Context context;
    private LocationManager locationManager;
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private LocationListener locationListener;
    private boolean isListening = false;

    public GeolocationManager(Context context) {
        this.context = context.getApplicationContext();
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public void requestLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted");
            return;
        }

        if (isListening) {
            Log.d(TAG, "Already listening for location updates");
            return;
        }

        try {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.d(TAG, "Location changed: " + location.getLatitude() + ", " + location.getLongitude());
                    currentLocation.postValue(location);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {
                    Log.w(TAG, "Location provider disabled");
                }
            };

            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        locationListener
                );
                isListening = true;
                Log.d(TAG, "Requested GPS location updates");
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        locationListener
                );
                isListening = true;
                Log.d(TAG, "Requested Network location updates");
            }

            Location lastLocation = getLastKnownLocation();
            if (lastLocation != null) {
                currentLocation.postValue(lastLocation);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error requesting location updates: " + e.getMessage());
        }
    }

    public void stopLocationUpdates() {
        if (locationListener != null && isListening) {
            try {
                locationManager.removeUpdates(locationListener);
                isListening = false;
                Log.d(TAG, "Stopped location updates");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception while stopping updates: " + e.getMessage());
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates: " + e.getMessage());
            }
        }
    }

    private Location getLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }

        Location bestLocation = null;
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (gpsLocation != null && (bestLocation == null ||
                        gpsLocation.getTime() > bestLocation.getTime())) {
                    bestLocation = gpsLocation;
                }
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null && (bestLocation == null ||
                        networkLocation.getTime() > bestLocation.getTime())) {
                    bestLocation = networkLocation;
                }
            }

            if (bestLocation != null) {
                Log.d(TAG, "Last known location: " + bestLocation.getLatitude() + ", " + bestLocation.getLongitude());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting last known location: " + e.getMessage());
        }

        return bestLocation;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}