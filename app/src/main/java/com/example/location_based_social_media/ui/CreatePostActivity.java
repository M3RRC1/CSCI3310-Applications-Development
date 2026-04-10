package com.example.location_based_social_media.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.google.android.gms.location.Priority;

public class CreatePostActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 2001;
    private static final long POST_TIMEOUT_MS = 20000;

    private EditText editTextPost;
    private ImageView imagePreview;
    private Uri imageUri;
    private Button buttonPost;
    private boolean isPosting = false;
    private int activePostRequestId = 0;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable postingTimeoutRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        editTextPost = findViewById(R.id.editTextPost);
        imagePreview = findViewById(R.id.imagePreview);
        Button buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonPost = findViewById(R.id.buttonPost);

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
        if (isPosting) return;

        String text = editTextPost.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter post text", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        int requestId = ++activePostRequestId;
        setPostingState(true);
        startPostingTimeout(requestId);

        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(location -> {
                if (requestId != activePostRequestId) return;
                if (location != null) {
                    submitPost(text, location, requestId);
                } else {
                    requestCurrentLocationAndSubmit(text, requestId);
                }
            })
            .addOnFailureListener(e -> {
                if (requestId != activePostRequestId) return;
                requestCurrentLocationAndSubmit(text, requestId);
            });
    }

    private void requestCurrentLocationAndSubmit(String text, int requestId) {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener(location -> {
                if (requestId != activePostRequestId) return;
                if (location != null) {
                    submitPost(text, location, requestId);
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                    setPostingState(false);
                }
            })
            .addOnFailureListener(e -> {
                if (requestId != activePostRequestId) return;
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_LONG).show();
                setPostingState(false);
            });
    }

    private void submitPost(String text, Location location, int requestId) {
        Post post = new Post();
        post.text = text;
        post.imageUri = null;
        post.latitude = location.getLatitude();
        post.longitude = location.getLongitude();
        post.timestamp = System.currentTimeMillis();
        post.userId = firebaseManager.getUserId();

        firebaseManager.addPostWithOptionalImage(post, imageUri, new FirebaseManager.OperationCallback() {
            @Override
            public void onSuccess() {
                if (requestId != activePostRequestId) return;
                Toast.makeText(CreatePostActivity.this, "Post created!", Toast.LENGTH_SHORT).show();
                setPostingState(false);
                finish();
            }

            @Override
            public void onFailure(String error) {
                if (requestId != activePostRequestId) return;
                Toast.makeText(CreatePostActivity.this, error, Toast.LENGTH_LONG).show();
                setPostingState(false);
            }
        });
    }

    private void startPostingTimeout(int requestId) {
        if (postingTimeoutRunnable != null) {
            uiHandler.removeCallbacks(postingTimeoutRunnable);
        }
        postingTimeoutRunnable = () -> {
            if (requestId == activePostRequestId && isPosting) {
                Toast.makeText(this, "Posting timed out. Check network and try again.", Toast.LENGTH_LONG).show();
                setPostingState(false);
            }
        };
        uiHandler.postDelayed(postingTimeoutRunnable, POST_TIMEOUT_MS);
    }

    private void setPostingState(boolean posting) {
        isPosting = posting;
        buttonPost.setEnabled(!posting);
        buttonPost.setText(posting ? "Posting..." : "Post");
        if (!posting && postingTimeoutRunnable != null) {
            uiHandler.removeCallbacks(postingTimeoutRunnable);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            imagePreview.setImageURI(imageUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPost();
            } else {
                Toast.makeText(this, "Location permission is required to create a post", Toast.LENGTH_SHORT).show();
            }
        }
    }
}