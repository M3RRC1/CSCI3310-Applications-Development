package com.example.location_based_social_media.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.example.location_based_social_media.Notifications.NotificationHelper;
import com.example.location_based_social_media.location.LocationHelper;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseManager firebaseManager;
    private Location currentLocation;
    private float radiusInMeters = 1000f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseManager = new FirebaseManager();

        Button buttonAddPost = findViewById(R.id.buttonAddPost);
        buttonAddPost.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));

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
        mMap.setMinZoomPreference(18f);

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

                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Zoom camera to user
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));

                // draw radius circle
                mMap.addCircle(new CircleOptions()
                        .center(userLatLng)
                        .radius(radiusInMeters)
                        .strokeWidth(2f)
                        .strokeColor(0x550000FF)
                        .fillColor(0x220000FF));

                loadPosts();
            } else {
                Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show();
                loadPosts();
            }
        });
    }

    private void loadPosts() {
        firebaseManager.getPosts(posts -> {
            mMap.clear();
            for (Post post : posts) {
                float[] distance = new float[1];
                if (currentLocation != null) {
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                            post.latitude, post.longitude, distance);
                }

                if (distance[0] <= radiusInMeters || currentLocation == null) {
                    LatLng pos = new LatLng(post.latitude, post.longitude);
                    Marker marker = mMap.addMarker(new MarkerOptions().position(pos).title("Post").snippet(post.text));
                    marker.setTag(post);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPosts();
        firebaseManager.listenForNearbyPosts(this);
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