package com.example.hisync.schedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hisync.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SessionDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SESSION_ID = "session_id";
    private static final String ARG_SONG_TITLE = "song_title";

    private FirebaseFirestore db;

    public static SessionDetailBottomSheet newInstance(String sessionId, String songTitle) {
        SessionDetailBottomSheet sheet = new SessionDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionId);
        args.putString(ARG_SONG_TITLE, songTitle);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        String sessionId = requireArguments().getString(ARG_SESSION_ID);
        String songTitle = requireArguments().getString(ARG_SONG_TITLE);

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        tvTitle.setText(songTitle);

        LinearLayout layoutMembers = view.findViewById(R.id.layoutSheetMembers);
        LinearLayout layoutTasks   = view.findViewById(R.id.layoutSheetTasks);

        // Load members
        db.collection("sessions").document(sessionId)
                .collection("members")
                .get()
                .addOnSuccessListener(snaps -> {
                    layoutMembers.removeAllViews();
                    for (QueryDocumentSnapshot doc : snaps) {
                        String instrument = doc.getString("instrument");
                        String userId = doc.getId();

                        // Fetch display name from users collection
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("displayName");
                                    if (name == null) name = userId;
                                    addMemberRow(layoutMembers, instrument != null ? instrument : "?", name);
                                });
                    }
                });

        // Load tasks
        db.collection("tasks")
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(snaps -> {
                    layoutTasks.removeAllViews();
                    for (QueryDocumentSnapshot doc : snaps) {
                        String title  = doc.getString("title");
                        String status = doc.getString("status");
                        addTaskRow(layoutTasks, title != null ? title : "Task", status);
                    }
                });
    }

    private void addMemberRow(LinearLayout parent, String instrument, String name) {
        if (!isAdded()) return;
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_member_row, parent, false);
        ((TextView) row.findViewById(R.id.tvInstrument)).setText(instrument.toUpperCase());
        ((TextView) row.findViewById(R.id.tvMemberName)).setText(name);
        parent.addView(row);
    }

    private void addTaskRow(LinearLayout parent, String title, String status) {
        if (!isAdded()) return;
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_row, parent, false);
        ((TextView) row.findViewById(R.id.tvTaskTitle)).setText(title);

        View dot = row.findViewById(R.id.viewTaskDot);
        if ("done".equals(status)) {
            dot.setBackgroundResource(R.drawable.dot_green);
        } else if ("rerecord".equals(status)) {
            dot.setBackgroundResource(R.drawable.dot_amber);
        } else {
            dot.setBackgroundResource(R.drawable.dot_purple);
        }
        parent.addView(row);
    }
}