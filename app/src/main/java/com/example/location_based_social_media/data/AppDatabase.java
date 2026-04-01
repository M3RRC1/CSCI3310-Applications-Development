package com.example.location_based_social_media.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Post.class, Comment.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PostDao postDao();
    public abstract CommentDao commentDao();
}