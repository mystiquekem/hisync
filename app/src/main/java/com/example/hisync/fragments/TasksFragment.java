package com.example.hisync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hisync.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class TasksFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutPending, layoutDone;
    private FirebaseFirestore db;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        swipeRefresh  = view.findViewById(R.id.swipeRefreshTasks);
        layoutPending = view.findViewById(R.id.layoutPendingTasks);
        layoutDone    = view.findViewById(R.id.layoutDoneTasks);

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadTasks);

        loadTasks();
    }

    private void loadTasks() {
        if (user == null) return;
        db.collection("tasks")
                .whereEqualTo("assignedTo", user.getUid())
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!isAdded()) return;
                    layoutPending.removeAllViews();
                    layoutDone.removeAllViews();

                    for (QueryDocumentSnapshot doc : snaps) {
                        String title   = doc.getString("title");
                        String status  = doc.getString("status");
                        String session = doc.getString("sessionId");
                        boolean isDone = "done".equals(status);

                        View row = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_task_row, isDone ? layoutDone : layoutPending, false);
                        ((TextView) row.findViewById(R.id.tvTaskTitle))
                                .setText(title != null ? title : "Task");

                        View dot = row.findViewById(R.id.viewTaskDot);
                        if (isDone)                    dot.setBackgroundResource(R.drawable.dot_green);
                        else if ("rerecord".equals(status)) dot.setBackgroundResource(R.drawable.dot_amber);
                        else                           dot.setBackgroundResource(R.drawable.dot_purple);

                        if (isDone) layoutDone.addView(row);
                        else        layoutPending.addView(row);
                    }
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) swipeRefresh.setRefreshing(false);
                });
    }
}