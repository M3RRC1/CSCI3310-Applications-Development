package com.example.location_based_social_media.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CommentDao {
    @Insert
    void insert(Comment comment);

    @Query("SELECT * FROM Comment WHERE postId = :postId ORDER BY timestamp ASC")
    List<Comment> getCommentsForPost(int postId);

    @Delete
    void delete(Comment comment);
}