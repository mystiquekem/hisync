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
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SessionResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SessionDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SESSION_ID  = "session_id";
    private static final String ARG_SONG_TITLE  = "song_title";

    public static SessionDetailBottomSheet newInstance(long sessionId, String songTitle) {
        SessionDetailBottomSheet sheet = new SessionDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
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

        long sessionId   = requireArguments().getLong(ARG_SESSION_ID);
        String songTitle = requireArguments().getString(ARG_SONG_TITLE);

        ((TextView) view.findViewById(R.id.tvSheetTitle)).setText(songTitle);

        LinearLayout layoutMembers = view.findViewById(R.id.layoutSheetMembers);
        LinearLayout layoutTasks   = view.findViewById(R.id.layoutSheetTasks);

        RetrofitClient.getInstance().getApi()
                .getSession(sessionId)
                .enqueue(new Callback<SessionResponse>() {
                    @Override
                    public void onResponse(Call<SessionResponse> call,
                                           Response<SessionResponse> response) {
                        if (!isAdded() || response.body() == null) return;
                        SessionResponse session = response.body();

                        // Members
                        if (session.getMembers() != null) {
                            for (SessionResponse.MemberResponse m : session.getMembers()) {
                                addMemberRow(layoutMembers,
                                        m.getInstrument() != null ? m.getInstrument() : "?",
                                        m.getDisplayName() != null ? m.getDisplayName() : "Unknown");
                            }
                        }

                        // Tasks
                        if (session.getTasks() != null) {
                            for (com.example.hisync.dto.TaskResponse t : session.getTasks()) {
                                addTaskRow(layoutTasks,
                                        t.getTitle() != null ? t.getTitle() : "Task",
                                        t.getStatus());
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<SessionResponse> call, Throwable t) {
                        // Bottom sheet vẫn mở, chỉ không có data
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
        if ("done".equals(status))         dot.setBackgroundResource(R.drawable.dot_green);
        else if ("rerecord".equals(status)) dot.setBackgroundResource(R.drawable.dot_amber);
        else                               dot.setBackgroundResource(R.drawable.dot_purple);
        parent.addView(row);
    }
}