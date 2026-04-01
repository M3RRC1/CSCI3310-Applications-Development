package com.example.location_based_social_media.ui;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.AppDatabase;
import com.example.location_based_social_media.data.Comment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;


public class CommentFragment extends BottomSheetDialogFragment {

    private static final String ARG_POST_ID = "post_id";
    private int postId;
    private AppDatabase db;
    private RecyclerView recyclerView;
    private CommentAdapter adapter;

    public static CommentFragment newInstance(int postId) {
        CommentFragment fragment = new CommentFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_POST_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comment, container, false);

        if (getArguments() != null) {
            postId = getArguments().getInt(ARG_POST_ID);
        }

        db = Room.databaseBuilder(
                requireContext(),
                AppDatabase.class,
                "post-database"
        ).allowMainThreadQueries().build();

        recyclerView = view.findViewById(R.id.recycler_comments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load initial comments
        List<Comment> comments = db.commentDao().getCommentsForPost(postId);
        adapter = new CommentAdapter(comments);
        recyclerView.setAdapter(adapter);

        EditText editComment = view.findViewById(R.id.edit_comment);
        Button addCommentButton = view.findViewById(R.id.button_add_comment);

        addCommentButton.setOnClickListener(v -> {
            String text = editComment.getText().toString().trim();
            if (!text.isEmpty()) {
                Comment comment = new Comment();
                comment.postId = postId;
                comment.userId = "hayden123"; // just sample, replace with real user ID later
                comment.text = text;
                comment.timestamp = System.currentTimeMillis();

                db.commentDao().insert(comment);

                // Refresh list
                List<Comment> updated = db.commentDao().getCommentsForPost(postId);
                adapter.updateComments(updated);

                editComment.setText("");
            }
        });

        return view;
    }
}