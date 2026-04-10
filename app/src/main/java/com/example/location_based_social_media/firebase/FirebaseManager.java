package com.example.location_based_social_media.firebase;

import android.content.Context;
import android.location.Location;

import com.example.location_based_social_media.Notifications.NotificationHelper;
import com.example.location_based_social_media.data.Comment;
import com.example.location_based_social_media.data.Like;
import com.example.location_based_social_media.data.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class FirebaseManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private Location currentLocation;
    private float radiusInMeters = 1000f;

    public String getUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    public void addPost(Post post) {
        DocumentReference ref = db.collection("posts").document();
        post.id = ref.getId();
        ref.set(post);
    }

    public void getPosts(final PostsCallback callback) {
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    List<Post> posts = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Post post = doc.toObject(Post.class);
                            if (post != null) posts.add(post);
                        }
                    }
                    callback.onCallback(posts);
                });
    }

    public void addComment(Comment comment) {
        DocumentReference ref = db.collection("comments").document();
        comment.id = ref.getId();
        ref.set(comment);
    }

    public void getComments(String postId, final CommentsCallback callback) {
        db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    List<Comment> comments = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Comment c = doc.toObject(Comment.class);
                            if (c != null) comments.add(c);
                        }
                    }
                    callback.onCallback(comments);
                });
    }

    public void addLike(String postId, String userId) {
        DocumentReference ref = db.collection("likes").document();
        Like like = new Like(postId, userId);
        like.id = ref.getId();
        ref.set(like);
    }

    public void removeLike(String postId, String userId) {
        db.collection("likes")
                .whereEqualTo("postId", postId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            doc.getReference().delete();
                        }
                    }
                });
    }

    public void getLikes(String postId, final LikesCallback callback) {
        db.collection("likes")
                .whereEqualTo("postId", postId)
                .addSnapshotListener((snapshot, error) -> {
                    List<Like> likes = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Like l = doc.toObject(Like.class);
                            if (l != null) likes.add(l);
                        }
                    }
                    callback.onCallback(likes);
                });
    }

    public void listenForNearbyPosts(Context context) {
        db.collection("posts")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Post post = dc.getDocument().toObject(Post.class);
                            if (post != null) checkRadiusAndNotify(context, post);
                        }
                    }
                });
    }

    private void checkRadiusAndNotify(Context context, Post post) {
        if (currentLocation == null) return;
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(),
                currentLocation.getLongitude(),
                post.latitude,
                post.longitude,
                results
        );
        if (results[0] <= radiusInMeters) {
            NotificationHelper.show(context, "New post nearby", post.text);
        }
    }



    public interface PostsCallback { void onCallback(List<Post> posts); }
    public interface CommentsCallback { void onCallback(List<Comment> comments); }
    public interface LikesCallback { void onCallback(List<Like> likes); }
}