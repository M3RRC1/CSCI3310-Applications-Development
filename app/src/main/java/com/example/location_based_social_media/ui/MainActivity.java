package com.example.location_based_social_media.ui;

import android.Manifest;
import android.content.Intent;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.location_based_social_media.data.Post;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.location_based_social_media.R;
import com.example.location_based_social_media.location.LocationHelper;
import androidx.room.Room;
import com.example.location_based_social_media.data.AppDatabase;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private AppDatabase db;
    private float radiusInMeters = 1000; // 1km (can change later)
    private Location currentLocation;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonAddPost = findViewById(R.id.buttonAddPost);
        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "post-database"
        ).allowMainThreadQueries().build();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request permission
        if (!LocationHelper.hasLocationPermission(this)) {
            LocationHelper.requestLocationPermission(this);
        }

        // Initialize map
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        buttonAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });
        // Just for my testing
//        Post testPost = new Post();
//        testPost.text = "Hello world!";
//        testPost.latitude = 0;
//        testPost.longitude = 0;
//        testPost.timestamp = System.currentTimeMillis();
//
//        db.postDao().insert(testPost);

        List<Post> posts = db.postDao().getAllPosts();
        Log.d("DB_TEST", "Posts count: " + posts.size());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {

                    if (location != null) {
                        currentLocation = location;

                        LatLng userLatLng = new LatLng(
                                location.getLatitude(),
                                location.getLongitude()
                        );

                        // Move camera
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));

                        // Add circle indicator
                        mMap.addCircle(new CircleOptions()
                                .center(userLatLng)
                                .radius(radiusInMeters)
                                .strokeWidth(2f));

                        // Add marker
                        mMap.addMarker(new MarkerOptions()
                                .position(userLatLng)
                                .title("You are here"));

                        loadPostsOnMap();

                    } else {
                        Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate(); // reload activity
            }
        }
    }
    private void loadPostsOnMap() {
        if (currentLocation == null) return;

        List<Post> posts = db.postDao().getAllPosts();

        for (Post post : posts) {

            float[] results = new float[1];

            Location.distanceBetween(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    post.latitude,
                    post.longitude,
                    results
            );

            if (results[0] <= radiusInMeters) {
                LatLng position = new LatLng(post.latitude, post.longitude);

                mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Post")
                        .snippet(post.text));
            }
        }
    }



}