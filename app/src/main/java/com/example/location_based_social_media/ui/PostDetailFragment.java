package com.example.location_based_social_media.ui;

import android.graphics.Color;
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
        ImageView imageView = view.findViewById(R.id.post_image);
        likesView = view.findViewById(R.id.like_count);
        ImageButton likeBtn = view.findViewById(R.id.button_like);
        ImageButton commentBtn = view.findViewById(R.id.button_comment);

        if (getArguments() != null) postId = getArguments().getString("postId");

        // Load post
        firebaseManager.getPosts(posts -> {
            for (Post post : posts) {
                if (post.id.equals(postId)) {
                    textView.setText(post.text);
                    if (post.imageUri != null) {
                        Glide.with(this).load(post.imageUri).into(imageView);
                    }
                    break;
                }
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

        return view;
    }
}