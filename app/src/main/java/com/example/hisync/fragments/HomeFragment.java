package com.example.hisync.fragments;

import android.content.Intent;
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

import com.example.hisync.LoginActivity;
import com.example.hisync.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvAvatar, tvUserName, tvUserEmail;
    private TextView tvSessionDate, tvSessionSong, tvEmptySession;
    private LinearLayout cardNextSession, layoutTasks, tvEmptyTasks;

    private FirebaseFirestore db;
    private FirebaseUser user;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db   = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        swipeRefresh   = view.findViewById(R.id.swipeRefresh);
        tvAvatar       = view.findViewById(R.id.tvAvatar);
        tvUserName     = view.findViewById(R.id.tvUserName);
        tvUserEmail    = view.findViewById(R.id.tvUserEmail);
        tvSessionDate  = view.findViewById(R.id.tvSessionDate);
        tvSessionSong  = view.findViewById(R.id.tvSessionSong);
        cardNextSession = view.findViewById(R.id.cardNextSession);
        layoutTasks    = view.findViewById(R.id.layoutTasks);
        tvEmptySession = view.findViewById(R.id.tvEmptySession);
        tvEmptyTasks   = view.findViewById(R.id.tvEmptyTasks);

        // Sign out button
        MaterialButton btnSignOut = view.findViewById(R.id.btnSignOut);
        btnSignOut.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // SwipeRefresh color
        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadData);

        populateHeader();
        loadData();
    }

    private void populateHeader() {
        if (user == null) return;
        String email = user.getEmail() != null ? user.getEmail() : "user";
        String name  = user.getDisplayName();

        if (name != null && !name.isEmpty()) {
            tvUserName.setText(name);
            tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        } else {
            String derived = email.split("@")[0];
            tvUserName.setText(derived);
            tvAvatar.setText(String.valueOf(derived.charAt(0)).toUpperCase());
        }
        tvUserEmail.setText(email);
    }

    private void loadData() {
        if (user == null) return;
        loadNextSession();
        loadTasks();
    }

    private void loadNextSession() {
        Date now = new Date();
        db.collection("sessions")
                .whereGreaterThanOrEqualTo("date", new com.google.firebase.Timestamp(now))
                .orderBy("date", Query.Direction.ASCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!isAdded()) return;
                    boolean found = false;
                    for (QueryDocumentSnapshot doc : snaps) {
                        // Check current user is a member
                        db.collection("sessions").document(doc.getId())
                                .collection("members").document(user.getUid())
                                .get()
                                .addOnSuccessListener(memberSnap -> {
                                    if (!isAdded()) return;
                                    if (memberSnap.exists() && !cardNextSession.isShown()) {
                                        String title = doc.getString("songTitle");
                                        com.google.firebase.Timestamp ts = doc.getTimestamp("date");
                                        if (ts != null) {
                                            SimpleDateFormat fmt = new SimpleDateFormat("EEEE · h:mm a", Locale.getDefault());
                                            tvSessionDate.setText(fmt.format(ts.toDate()));
                                        }
                                        tvSessionSong.setText(title != null ? title : "Session");
                                        cardNextSession.setVisibility(View.VISIBLE);
                                        tvEmptySession.setVisibility(View.GONE);
                                    }
                                });
                    }
                    // If no sessions at all, show empty state after a delay
                    if (snaps.isEmpty()) {
                        cardNextSession.setVisibility(View.GONE);
                        tvEmptySession.setVisibility(View.VISIBLE);
                    }
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) swipeRefresh.setRefreshing(false);
                });
    }

    private void loadTasks() {
        db.collection("tasks")
                .whereEqualTo("assignedTo", user.getUid())
                .limit(10)
                .get()
                .addOnSuccessListener(snaps -> {
                    if (!isAdded()) return;
                    layoutTasks.removeAllViews();
                    if (snaps.isEmpty()) {
                        tvEmptyTasks.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvEmptyTasks.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : snaps) {
                        String title  = doc.getString("title");
                        String status = doc.getString("status");
                        addTaskRow(title != null ? title : "Task", status,
                                doc.getString("sessionId"));
                    }
                });
    }

    private void addTaskRow(String title, String status, String sessionId) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_row, layoutTasks, false);
        ((TextView) row.findViewById(R.id.tvTaskTitle)).setText(title);
        View dot = row.findViewById(R.id.viewTaskDot);
        if ("done".equals(status))       dot.setBackgroundResource(R.drawable.dot_green);
        else if ("rerecord".equals(status)) dot.setBackgroundResource(R.drawable.dot_amber);
        else                             dot.setBackgroundResource(R.drawable.dot_purple);
        layoutTasks.addView(row);
    }
}