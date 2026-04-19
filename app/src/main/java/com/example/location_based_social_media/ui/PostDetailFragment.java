package com.example.location_based_social_media.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Like;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class PostDetailFragment extends BottomSheetDialogFragment {

    private static final String TAG = "PostDetailFragment";

    private String postId;
    private boolean liked;
    private FirebaseManager firebaseManager;
    private TextView likesView;
    private Post currentPost;
    private ListenerRegistration postsListener;
    private ListenerRegistration likesListener;

    public static PostDetailFragment newInstance(String postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);

        firebaseManager = new FirebaseManager();

        TextView textView = view.findViewById(R.id.post_text);
        TextView userView = view.findViewById(R.id.post_user);
        ImageView imageView = view.findViewById(R.id.post_image);
        likesView = view.findViewById(R.id.like_count);
        ImageButton likeBtn = view.findViewById(R.id.button_like);
        ImageButton commentBtn = view.findViewById(R.id.button_comment);
        ImageButton shareBtn = view.findViewById(R.id.button_share);

        if (getArguments() != null) postId = getArguments().getString("postId");

        // Load post
        postsListener = firebaseManager.getPosts(posts -> {
            for (Post post : posts) {
                if (post.id.equals(postId)) {
                    currentPost = post;
                    textView.setText(post.text);
                    userView.setText("Posted by: " + formatUserLabel(post.userId));
                    if (post.imageUri != null) {
                        Glide.with(this).load(post.imageUri).into(imageView);
                    }
                    break;
                }
            }
        }, error -> {
            if (!isAdded() || !isVisible() || firebaseManager.getUserId().isEmpty()) {
                return;
            }
            if (isAdded()) {
                Log.e(TAG, "Load post failed: " + error);
                Toast.makeText(requireContext(), "Load post failed: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Load likes and keep local "liked" state in sync with backend data.
        likesListener = firebaseManager.getLikes(postId, likes -> {
            likesView.setText(String.valueOf(likes.size()));
            String uid = firebaseManager.getUserId();
            boolean userLiked = false;
            if (uid != null && !uid.trim().isEmpty()) {
                for (Like like : likes) {
                    if (uid.equals(like.userId)) {
                        userLiked = true;
                        break;
                    }
                }
            }
            liked = userLiked;
            likeBtn.setColorFilter(liked ? Color.RED : Color.WHITE);
        });

        likeBtn.setOnClickListener(v -> {
            String uid = firebaseManager.getUserId();
            if (uid == null || uid.trim().isEmpty()) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Please sign in before liking", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            if (!liked) {
                firebaseManager.addLike(postId, uid);
                liked = true;
                likeBtn.setColorFilter(Color.RED);
            } else {
                firebaseManager.removeLike(postId, uid);
                liked = false;
                likeBtn.setColorFilter(Color.DKGRAY);
            }
        });

        commentBtn.setOnClickListener(v -> {
            CommentFragment.newInstance(postId).show(getParentFragmentManager(), "comments");
        });

        shareBtn.setOnClickListener(v -> sharePost(currentPost));
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        if (likesListener != null) {
            likesListener.remove();
            likesListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
        if (likesListener != null) {
            likesListener.remove();
            likesListener = null;
        }
    }

    private void sharePost(Post post) {
        if (post == null) {
            if (isAdded()) {
                Log.w(TAG, "Post is still loading");
                Toast.makeText(requireContext(), "Post is still loading", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String shareText = post.text == null ? "" : post.text;

        if (post.imageUri == null || post.imageUri.isEmpty()) {
            shareTextOnly(shareText);
            return;
        }

        shareImageAndText(post.imageUri, shareText);
    }

    private void shareTextOnly(String shareText) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        launchShareChooser(sendIntent);
    }

    private void shareImageAndText(String imageUrl, String shareText) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        Toast.makeText(requireContext(), "Preparing image...", Toast.LENGTH_SHORT).show();

        try {
            File localFile = File.createTempFile("shared_image", ".jpg", requireContext().getCacheDir());
            storageRef.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> {
                        shareImageWithText(localFile, shareText);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Failed to download image", Toast.LENGTH_SHORT).show();
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareImageWithText(File imageFile, String shareText) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                imageFile
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share post via"));
    }


    private Uri copyRemoteImageToCache(String imageUrl) throws IOException, InterruptedException, java.util.concurrent.ExecutionException {
        Context context = requireContext().getApplicationContext();
        File cacheDir = new File(context.getCacheDir(), "shared_images");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            return null;
        }

        File sourceFile = Glide.with(context)
                .asFile()
                .load(imageUrl)
                .submit()
                .get();

        File targetFile = new File(cacheDir, "shared_post_" + System.currentTimeMillis() + ".jpg");
        try (InputStream inputStream = new FileInputStream(sourceFile);
             OutputStream outputStream = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }

        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", targetFile);
    }

    private void launchShareChooser(Intent sendIntent) {
        if (!isAdded()) {
            return;
        }

        try {
            Intent shareIntent = Intent.createChooser(sendIntent, "Share post via");
            if (shareIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(shareIntent);
            } else {
                Log.w(TAG, "No app available to share this content");
                Toast.makeText(requireContext(), "No app available to share this content", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Unable to open share menu", e);
            Toast.makeText(requireContext(), "Unable to open share menu", Toast.LENGTH_SHORT).show();
        }
    }

    private void postShareError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
                Log.e(TAG, message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String formatUserLabel(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return "Unknown user";
        }
        if (userId.length() <= 8) {
            return userId;
        }
        return userId.substring(0, 8) + "...";
    }
}