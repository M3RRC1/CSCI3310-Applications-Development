package com.example.location_based_social_media.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Post {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public String imageUri;

    public double latitude;
    public double longitude;

    public long timestamp;
    public int likes;
}