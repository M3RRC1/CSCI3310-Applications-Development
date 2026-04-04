package com.example.location_based_social_media.data;

public class Like {
    public String id;          // Firestore document ID
    public String postId;
    public String userId;

    public Like() {}

    public Like(String postId, String userId) {
        this.postId = postId;
        this.userId = userId;
    }
}