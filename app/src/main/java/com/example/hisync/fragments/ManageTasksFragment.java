package com.example.hisync.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SessionTasksDto;
import com.example.hisync.dto.TaskResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageTasksFragment extends Fragment {

    private static final DateTimeFormatter API_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter SHOW_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a");

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutSessions;
    private TextView tvEmpty;

    private long bandId, userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        bandId = prefs.getLong("bandId", -1);
        userId = prefs.getLong("userId", -1);

        swipeRefresh   = view.findViewById(R.id.swipeRefreshManageTasks);
        layoutSessions = view.findViewById(R.id.layoutTaskSessions);
        tvEmpty        = view.findViewById(R.id.tvEmptyManageTasks);

        view.findViewById(R.id.btnBackFromTasks).setOnClickListener(v -> {
            requireActivity().findViewById(R.id.fragmentContainer)
                    .setVisibility(View.GONE);
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadTasks);

        loadTasks();
    }

    private void loadTasks() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getTasksByBand(bandId)
                .enqueue(new Callback<List<SessionTasksDto>>() {
                    @Override
                    public void onResponse(Call<List<SessionTasksDto>> call,
                                           Response<List<SessionTasksDto>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        layoutSessions.removeAllViews();

                        if (response.body() == null || response.body().isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvEmpty.setVisibility(View.GONE);
                        for (SessionTasksDto session : response.body()) {
                            addSessionGroup(session);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SessionTasksDto>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void addSessionGroup(SessionTasksDto session) {
        View group = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_session_group, layoutSessions, false);

        ((TextView) group.findViewById(R.id.tvTaskSessionTitle))
                .setText(session.getSongTitle());

        String dateStr = "—";
        if (session.getSessionDate() != null) {
            try {
                LocalDateTime dt = LocalDateTime.parse(
                        session.getSessionDate().substring(0, 19), API_FMT);
                dateStr = dt.format(SHOW_FMT);
            } catch (Exception ignored) {}
        }
        ((TextView) group.findViewById(R.id.tvTaskSessionDate)).setText(dateStr);

        LinearLayout layoutTasks = group.findViewById(R.id.layoutGroupTasks);

        // Add task rows
        if (session.getTasks() != null) {
            for (TaskResponse task : session.getTasks()) {
                addTaskRow(layoutTasks, task, session.getSessionId());
            }
        }

        // Add task button
        group.findViewById(R.id.btnAddTask).setOnClickListener(v ->
                openCreateTask(session.getSessionId(), session.getSongTitle()));

        layoutSessions.addView(group);
    }

    private void addTaskRow(LinearLayout parent, TaskResponse task, long sessionId) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_manage_task_row, parent, false);

        ((TextView) row.findViewById(R.id.tvManageTaskTitle)).setText(task.getTitle());
        ((TextView) row.findViewById(R.id.tvManageTaskAssignee))
                .setText(task.getAssignedToName());

        TextView tvStatus = row.findViewById(R.id.tvManageTaskStatus);
        bindStatusBadge(tvStatus, task.getStatus());

        row.findViewById(R.id.btnEditTask).setOnClickListener(v ->
                openEditTask(task, sessionId));

        row.findViewById(R.id.btnDeleteTask).setOnClickListener(v ->
                confirmDeleteTask(task));

        parent.addView(row);
    }

    private void bindStatusBadge(TextView tv, String status) {
        switch (status) {
            case "submitted":
                tv.setText("Awaiting");
                tv.setBackgroundResource(R.drawable.bg_role_leader);
                tv.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark));
                break;
            case "approved":
                tv.setText("Approved");
                tv.setBackgroundResource(R.drawable.dot_green);
                tv.setTextColor(requireContext().getColor(R.color.white));
                break;
            case "rerecord":
                tv.setText("Rerecord");
                tv.setBackgroundResource(R.drawable.dot_amber);
                tv.setTextColor(requireContext().getColor(R.color.white));
                break;
            default: // pending
                tv.setText("Pending");
                tv.setBackgroundResource(R.drawable.bg_role_member);
                tv.setTextColor(requireContext().getColor(R.color.purple_primary));
                break;
        }
    }

    private void openCreateTask(long sessionId, String songTitle) {
        CreateTaskBottomSheet sheet =
                CreateTaskBottomSheet.newInstance(sessionId, songTitle, bandId, -1, null);
        sheet.setOnSavedListener(this::loadTasks);
        sheet.show(getParentFragmentManager(), "create_task");
    }

    private void openEditTask(TaskResponse task, long sessionId) {
        CreateTaskBottomSheet sheet = CreateTaskBottomSheet.newInstance(
                sessionId, "", bandId, task.getId(), task.getTitle());
        sheet.setOnSavedListener(this::loadTasks);
        sheet.show(getParentFragmentManager(), "edit_task");
    }

    private void confirmDeleteTask(TaskResponse task) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete task")
                .setMessage("Delete \"" + task.getTitle() + "\"?")
                .setPositiveButton("Delete", (d, w) -> deleteTask(task.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(long taskId) {
        RetrofitClient.getInstance().getApi()
                .deleteTask(taskId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) loadTasks();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(requireContext(),
                                    "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}