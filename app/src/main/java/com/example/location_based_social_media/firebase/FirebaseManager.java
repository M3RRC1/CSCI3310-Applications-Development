package com.example.location_based_social_media.firebase;

import android.content.Context;
import android.location.Location;
import android.net.Uri;

import com.example.location_based_social_media.Notifications.NotificationHelper;
import com.example.location_based_social_media.data.Comment;
import com.example.location_based_social_media.data.Like;
import com.example.location_based_social_media.data.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FirebaseManager {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private ListenerRegistration nearbyPostsListener;
    private Location currentLocation;
    private float radiusInMeters = 100f;

    public void setCurrentLocation(Location location) {
        this.currentLocation = location;
    }

    public void setRadiusInMeters(float radiusInMeters) {
        this.radiusInMeters = radiusInMeters;
    }

    public String getUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    public void addPost(Post post) {
        DocumentReference ref = db.collection("posts").document();
        post.id = ref.getId();
        ref.set(post);
    }

    public void addPostWithOptionalImage(Post post, Uri localImageUri, final OperationCallback callback) {
        ensureSignedIn(new OperationCallback() {
            @Override
            public void onSuccess() {
                if (post.userId == null || post.userId.trim().isEmpty()) {
                    post.userId = getUserId();
                }
                if (localImageUri == null) {
                    addPostWithCallback(post, callback);
                    return;
                }

                StorageReference imageRef = storage.getReference()
                        .child("post_images/" + UUID.randomUUID() + ".jpg");

                imageRef.putFile(localImageUri)
                        .continueWithTask(task -> {
                            if (!task.isSuccessful()) {
                                throw task.getException() != null
                                        ? task.getException()
                                        : new RuntimeException("Upload task failed");
                            }
                            return imageRef.getDownloadUrl();
                        })
                        .addOnSuccessListener(downloadUri -> {
                            post.imageUri = downloadUri.toString();
                            addPostWithCallback(post, callback);
                        })
                        .addOnFailureListener(e -> callback.onFailure("Image upload failed: " + safeErrorMessage(e)));
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    private void addPostWithCallback(Post post, final OperationCallback callback) {
        DocumentReference ref = db.collection("posts").document();
        post.id = ref.getId();
        ref.set(post)
                .addOnSuccessListener(v -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Post save failed: " + safeErrorMessage(e)));
    }

    private void ensureSignedIn(final OperationCallback callback) {
        if (auth.getCurrentUser() != null) {
            callback.onSuccess();
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(result -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Authentication failed: " + safeErrorMessage(e)));
    }

    private String safeErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "Unknown error";
        }
        String message = e.getMessage().trim();
        if (message.length() > 180) {
            return message.substring(0, 180) + "...";
        }
        return message;
    }

    public ListenerRegistration getPosts(final PostsCallback callback, final ErrorCallback errorCallback) {
        return db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        if (errorCallback != null) {
                            errorCallback.onError(error.getMessage() != null ? error.getMessage() : "Failed to load posts");
                        }
                        return;
                    }
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
        addComment(comment, null);
    }

    public void addComment(Comment comment, OperationCallback callback) {
        DocumentReference ref = db.collection("comments").document();
        comment.id = ref.getId();
        ref.set(comment)
                .addOnSuccessListener(v -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure("Comment save failed: " + safeErrorMessage(e));
                    }
                });
    }

    public ListenerRegistration getComments(String postId, final CommentsCallback callback) {
        return db.collection("comments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        return;
                    }

                    List<Comment> comments = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Comment c = doc.toObject(Comment.class);
                        if (c != null) comments.add(c);
                    }
                    callback.onCallback(comments);
                });
    }

    public void addLike(String postId, String userId) {
        DocumentReference ref = db.collection("likes").document(likeDocumentId(postId, userId));
        Like like = new Like(postId, userId);
        like.id = ref.getId();
        ref.set(like);
    }

    public void removeLike(String postId, String userId) {
        String likeDocId = likeDocumentId(postId, userId);
        db.collection("likes").document(likeDocId).delete();

        // Cleanup legacy duplicates created before deterministic IDs.
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

    public ListenerRegistration getLikes(String postId, final LikesCallback callback) {
        return db.collection("likes")
                .whereEqualTo("postId", postId)
                .addSnapshotListener((snapshot, error) -> {
                    List<Like> likes = new ArrayList<>();
                    Set<String> seenUsers = new HashSet<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Like l = doc.toObject(Like.class);
                            if (l == null || l.userId == null || l.userId.trim().isEmpty()) {
                                continue;
                            }
                            if (seenUsers.add(l.userId)) {
                                likes.add(l);
                            }
                        }
                    }
                    callback.onCallback(likes);
                });
    }

    private String likeDocumentId(String postId, String userId) {
        return postId + "_" + userId;
    }

    public void listenForNearbyPosts(Context context) {
        stopNearbyPostsListener();
        nearbyPostsListener = db.collection("posts")
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

    public void stopNearbyPostsListener() {
        if (nearbyPostsListener != null) {
            nearbyPostsListener.remove();
            nearbyPostsListener = null;
        }
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
    public interface ErrorCallback { void onError(String error); }
    public interface CommentsCallback { void onCallback(List<Comment> comments); }
    public interface LikesCallback { void onCallback(List<Like> likes); }
    public interface OperationCallback {
        void onSuccess();
        void onFailure(String error);
    }
}