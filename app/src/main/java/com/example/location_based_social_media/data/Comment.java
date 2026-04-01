package com.example.location_based_social_media.data;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        foreignKeys = @ForeignKey(
                entity = Post.class,
                parentColumns = "id",
                childColumns = "postId",
                onDelete = CASCADE
        ),
        indices = {@Index("postId")}
)
public class Comment {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int postId;
    public String userId;    // who wrote the comment
    public String text;      // comment content
    public long timestamp;
}
