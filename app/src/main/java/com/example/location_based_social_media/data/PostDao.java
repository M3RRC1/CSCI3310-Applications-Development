package com.example.location_based_social_media.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PostDao {

    @Insert
    void insert(Post post);

    @Query("SELECT * FROM Post")
    List<Post> getAllPosts();
}