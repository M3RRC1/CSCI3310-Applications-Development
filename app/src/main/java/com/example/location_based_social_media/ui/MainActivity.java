package com.example.location_based_social_media.ui;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.example.location_based_social_media.Notifications.NotificationHelper;
import com.example.location_based_social_media.location.LocationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String PREFS_NAME = "location_settings";
    private static final String KEY_RADIUS_METERS = "radius_meters";
    private static final int MIN_RADIUS_METERS = 100;
    private static final int MAX_RADIUS_METERS = 2000;
    private static final int RADIUS_STEP_METERS = 100;

    private GoogleMap mMap;
    private FirebaseManager firebaseManager;
    private Location currentLocation;
    private float radiusInMeters = 100f;
    private Circle detectionRadiusCircle;
    private TextView textRadius;
    private SeekBar seekRadius;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        radiusInMeters = prefs.getInt(KEY_RADIUS_METERS, MIN_RADIUS_METERS);

        firebaseManager = new FirebaseManager();
        firebaseManager.setRadiusInMeters(radiusInMeters);

        Button buttonAddPost = findViewById(R.id.buttonAddPost);
        Button buttonSignOut = findViewById(R.id.buttonSignOut);
        textRadius = findViewById(R.id.textRadius);
        seekRadius = findViewById(R.id.seekRadius);

        seekRadius.setMax((MAX_RADIUS_METERS - MIN_RADIUS_METERS) / RADIUS_STEP_METERS);
        seekRadius.setProgress(progressFromRadius((int) radiusInMeters));
        updateRadiusLabel();
        seekRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                radiusInMeters = radiusFromProgress(progress);
                firebaseManager.setRadiusInMeters(radiusInMeters);
                updateRadiusLabel();
                updateDetectionRadiusCircle();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt(KEY_RADIUS_METERS, (int) radiusInMeters).apply();
                if (mMap != null) {
                    loadPosts();
                }
            }
        });

        buttonAddPost.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
        buttonSignOut.setOnClickListener(v -> {
            firebaseManager.stopNearbyPostsListener();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        if (!LocationHelper.hasLocationPermission(this)) {
            LocationHelper.requestLocationPermission(this);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        NotificationHelper.requestNotificationPermission(this);
        NotificationHelper.createChannel(this);
        firebaseManager.listenForNearbyPosts(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setMinZoomPreference(12f);

        // Marker click
        mMap.setOnMarkerClickListener(marker -> {
            Post post = (Post) marker.getTag();
            if (post != null) {
                PostDetailFragment.newInstance(post.id)
                        .show(getSupportFragmentManager(), "post_detail");
            }
            return true;
        });

        LocationHelper.getLastLocation(this, location -> {
            if (location != null) {
                currentLocation = location;
                firebaseManager.setCurrentLocation(location);

                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Zoom camera to user
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));

                // draw radius circle
                updateDetectionRadiusCircle();

                loadPosts();
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
                loadPosts();
            }
        });
    }

    private void loadPosts() {
        if (mMap == null) return;
        firebaseManager.getPosts(posts -> {
            mMap.clear();
            detectionRadiusCircle = null;
            updateDetectionRadiusCircle();
            for (Post post : posts) {
                float[] distance = new float[1];
                if (currentLocation != null) {
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                            post.latitude, post.longitude, distance);
                }

                if (distance[0] <= radiusInMeters || currentLocation == null) {
                    LatLng pos = new LatLng(post.latitude, post.longitude);
                    String userLabel = post.userId != null && post.userId.length() > 8
                            ? post.userId.substring(0, 8) + "..."
                            : (post.userId == null || post.userId.isEmpty() ? "Unknown" : post.userId);
                    String snippetText = post.text == null ? "" : post.text;
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title("User: " + userLabel)
                            .snippet(snippetText));
                    marker.setTag(post);
                }
            }
        }, error -> Toast.makeText(this, "Load posts failed: " + error, Toast.LENGTH_LONG).show());
    }

    private void updateDetectionRadiusCircle() {
        if (mMap == null || currentLocation == null) {
            return;
        }

        if (detectionRadiusCircle != null) {
            detectionRadiusCircle.remove();
        }

        detectionRadiusCircle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))
                .radius(radiusInMeters)
                .strokeWidth(6f)
                .strokeColor(0x550000FF)
                .fillColor(0x220000FF));
    }

    private void updateRadiusLabel() {
        textRadius.setText(getString(R.string.detection_radius_label, (int) radiusInMeters));
    }

    private int radiusFromProgress(int progress) {
        return MIN_RADIUS_METERS + (progress * RADIUS_STEP_METERS);
    }

    private int progressFromRadius(int radiusMeters) {
        int clamped = Math.max(MIN_RADIUS_METERS, Math.min(MAX_RADIUS_METERS, radiusMeters));
        return (clamped - MIN_RADIUS_METERS) / RADIUS_STEP_METERS;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            loadPosts();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseManager.stopNearbyPostsListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST)
            recreate();
        else if (requestCode == 1001) {
            // handle notification permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Notifications allowed
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
            }
        }
    }





}