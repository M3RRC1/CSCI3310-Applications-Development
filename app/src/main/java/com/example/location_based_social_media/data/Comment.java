package com.example.location_based_social_media.data;

public class Comment {
    public String id;          // Firestore document ID
    public String postId;      // associated post
    public String userId;      // who wrote
    public String text;        // comment text
    public long timestamp;

    public Comment() {}

    public Comment(String postId, String userId, String text, long timestamp) {
        this.postId = postId;
        this.userId = userId;
        this.text = text;
        this.timestamp = timestamp;
    }
}