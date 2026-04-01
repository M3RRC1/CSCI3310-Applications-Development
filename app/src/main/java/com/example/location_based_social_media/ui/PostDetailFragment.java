package com.example.location_based_social_media.ui;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.room.Room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.AppDatabase;
import com.example.location_based_social_media.data.Post;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PostDetailFragment extends BottomSheetDialogFragment {
    private int postId;   // store only the ID
    private boolean liked;

    // Pass only postId
    public static PostDetailFragment newInstance(int postId) {
        PostDetailFragment fragment = new PostDetailFragment();
        Bundle args = new Bundle();
        args.putInt("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_detail, container, false);

        TextView textView = view.findViewById(R.id.post_text);
        ImageView imageView = view.findViewById(R.id.post_image);
        TextView likes = view.findViewById(R.id.like_count);
        ImageButton likeBtn = view.findViewById(R.id.button_like);
        ImageButton commentBtn = view.findViewById(R.id.button_comment);

        // Retrieve postId from arguments
        if (getArguments() != null) {
            postId = getArguments().getInt("postId", -1);
        }

        // Query the database for the Post
        AppDatabase db = Room.databaseBuilder(
                requireContext(),
                AppDatabase.class,
                "post-database"
        ).allowMainThreadQueries().build();

        Post post = db.postDao().getPostById(postId);

        if (post != null) {
            textView.setText(post.text);
            likes.setText("0"); // or post.likeCount if you add that field

            if (post.imageUri != null) {
                Glide.with(this)
                        .load(post.imageUri)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(imageView);
            }
        }

        likeBtn.setOnClickListener(v -> {
            if (liked) {
                liked = false;
                likeBtn.setBackgroundColor(Color.DKGRAY);
            } else {
                liked = true;
                likeBtn.setBackgroundColor(Color.RED);
            }
        });

        commentBtn.setOnClickListener(v -> {
            if (postId != -1) {
                CommentFragment fragment = CommentFragment.newInstance(postId);
                fragment.show(getParentFragmentManager(), "comments");
            } else {
                Toast.makeText(getContext(), "Invalid post ID", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }
}