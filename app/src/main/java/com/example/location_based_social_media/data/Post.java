package com.example.location_based_social_media.data;

public class Post {
    public String id;          // Firestore document ID
    public String userId;      // who created the post
    public String text;
    public String imageUri;    // optional image
    public double latitude;
    public double longitude;
    public long timestamp;     // creation time

    // Firestore requires an empty constructor
    public Post() {}

    public Post(String userId, String text, String imageUri, double latitude, double longitude, long timestamp) {
        this.userId = userId;
        this.text = text;
        this.imageUri = imageUri;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
}