package com.example.location_based_social_media.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Like;
import com.example.location_based_social_media.data.Post;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

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

        shareBtn.setOnClickListener(v->sharePost(currentPost));
        return view;
    }

    private void sharePost(Post post) {
        if (post == null) return;

        // Share text
        String shareText = post.text;

        Uri imageUri = null;
        if (post.imageUri != null && !post.imageUri.isEmpty()) {
            imageUri = Uri.parse(post.imageUri);
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);

        if (imageUri != null) {
            // Share text + image
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            sendIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            sendIntent.setType("image/*");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            // Share text only
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            sendIntent.setType("text/plain");
        }

        // Show chooser
        Intent shareIntent = Intent.createChooser(sendIntent, "Share post via");
        startActivity(shareIntent);
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