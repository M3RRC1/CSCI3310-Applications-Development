package com.example.location_based_social_media.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationServices;

public class LocationHelper {

    public static final int LOCATION_PERMISSION_REQUEST = 100;

    public static boolean hasLocationPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST
        );
    }

    public static void getLastLocation(Context context, OnLocationReceived callback) {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient((Activity) context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        client.getLastLocation().addOnSuccessListener(location -> callback.onReceived(location));
    }

    public static LocationCallback startLocationUpdates(Context context, OnLocationReceived callback) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient((Activity) context);
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000L)
                .setMinUpdateIntervalMillis(2000L)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) {
                    return;
                }
                Location location = result.getLastLocation();
                if (location != null) {
                    callback.onReceived(location);
                }
            }
        };

        client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        return locationCallback;
    }

    public static void stopLocationUpdates(Context context, LocationCallback locationCallback) {
        if (locationCallback == null) {
            return;
        }
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient((Activity) context);
        client.removeLocationUpdates(locationCallback);
    }

    public interface OnLocationReceived {
        void onReceived(Location location);
    }
}