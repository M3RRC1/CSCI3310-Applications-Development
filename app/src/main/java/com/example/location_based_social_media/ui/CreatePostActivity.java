package com.example.location_based_social_media.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;

    private EditText editTextPost;
    private ImageView imagePreview;
    private Uri imageUri;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        editTextPost = findViewById(R.id.editTextPost);
        imagePreview = findViewById(R.id.imagePreview);
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        Button buttonPost = findViewById(R.id.buttonPost);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        firebaseManager = new FirebaseManager();

        // Select Image
        buttonSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE);
        });

        // Create Post
        buttonPost.setOnClickListener(v -> createPost());
    }

    private void createPost() {
        String text = editTextPost.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter post text", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Post post = new Post();
                post.text = text;
                post.imageUri = imageUri != null ? imageUri.toString() : null;
                post.latitude = location.getLatitude();
                post.longitude = location.getLongitude();
                post.timestamp = System.currentTimeMillis();
                post.userId = firebaseManager.getUserId();

                firebaseManager.addPost(post);
                Toast.makeText(this, "Post created!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
        }
    }
}