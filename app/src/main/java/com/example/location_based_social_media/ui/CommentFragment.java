package com.example.location_based_social_media.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.location_based_social_media.R;
import com.example.location_based_social_media.data.Comment;
import com.example.location_based_social_media.firebase.FirebaseManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class CommentFragment extends BottomSheetDialogFragment {

    private String postId;
    private FirebaseManager firebaseManager;
    private CommentAdapter adapter;
    private RecyclerView recyclerView;
    private ListenerRegistration commentsListener;

    public static CommentFragment newInstance(String postId) {
        CommentFragment fragment = new CommentFragment();
        Bundle args = new Bundle();
        args.putString("postId", postId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_comment, container, false);

        firebaseManager = new FirebaseManager();

        if (getArguments() != null) postId = getArguments().getString("postId");

        recyclerView = view.findViewById(R.id.recycler_comments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CommentAdapter();
        recyclerView.setAdapter(adapter);

        EditText editComment = view.findViewById(R.id.edit_comment);
        Button addCommentButton = view.findViewById(R.id.button_add_comment);

        addCommentButton.setOnClickListener(v -> {
            String text = editComment.getText().toString().trim();
            if (!text.isEmpty()) {
                Comment comment = new Comment();
                comment.text = text;
                comment.userId = firebaseManager.getUserId();
                comment.postId = postId;
                comment.timestamp = System.currentTimeMillis();
                firebaseManager.addComment(comment, new FirebaseManager.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }
                        adapter.addComment(comment);
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        editComment.setText("");
                    }

                    @Override
                    public void onFailure(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to send comment: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        // Load comments
        commentsListener = firebaseManager.getComments(postId, comments -> adapter.setComments(comments));

        return view;
    }

    @Override
    public void onDestroyView() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
        super.onDestroyView();
    }
}