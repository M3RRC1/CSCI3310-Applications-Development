package com.example.location_based_social_media.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Like;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Executors;

public class PostDetailFragment extends BottomSheetDialogFragment {

    private String postId;
    private boolean liked;
    private FirebaseManager firebaseManager;
    private TextView likesView;
    private Post currentPost;

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
        firebaseManager.getPosts(posts -> {
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
            if (isAdded()) {
                Toast.makeText(requireContext(), "Load post failed: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Load likes
        firebaseManager.getLikes(postId, likes -> likesView.setText(String.valueOf(likes.size())));

        likeBtn.setOnClickListener(v -> {
            String uid = firebaseManager.getUserId();
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

    private void sharePost(Post post) {
        if (post == null) {
            if (isAdded()) {
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
        if (!isAdded()) {
            return;
        }

        Toast.makeText(requireContext(), "Preparing image...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Uri sharedImageUri = copyRemoteImageToCache(imageUrl);
                if (sharedImageUri == null) {
                    postShareError("Unable to prepare image for sharing");
                    return;
                }

                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.putExtra(Intent.EXTRA_STREAM, sharedImageUri);
                sendIntent.setType("image/*");
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                new Handler(Looper.getMainLooper()).post(() -> launchShareChooser(sendIntent));
            } catch (Exception e) {
                postShareError("Share failed: " + e.getMessage());
            }
        });
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
                Toast.makeText(requireContext(), "No app available to share this content", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open share menu", Toast.LENGTH_SHORT).show();
        }
    }

    private void postShareError(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (isAdded()) {
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