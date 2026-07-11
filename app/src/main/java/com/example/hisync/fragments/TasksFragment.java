package com.example.hisync.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TasksFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private long userId, bandId;
    private boolean isLeader;

    // Member views
    private LinearLayout layoutPending, layoutSubmitted, layoutRerecord, layoutApproved;
    private View memberView;

    // Leader views
    private View leaderView;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnToggleManage, btnToggleSubmissions;
    private View viewManage, viewSubmissions;

    // Manage section (reused from ManageTasksFragment logic)
    private LinearLayout layoutManageSessions;
    private TextView tvManageEmpty;

    // Submissions section
    private LinearLayout layoutSubmissionsList;
    private TextView tvSubmissionsEmpty;

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

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        userId   = prefs.getLong("userId", -1);
        bandId   = prefs.getLong("bandId", -1);
        String role = prefs.getString("role", "member");
        isLeader = "leader".equals(role) || "admin".equals(role);

        swipeRefresh = view.findViewById(R.id.swipeRefreshTasks);
        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);

        memberView = view.findViewById(R.id.memberTasksView);
        leaderView = view.findViewById(R.id.leaderTasksView);

        if (isLeader) {
            memberView.setVisibility(View.GONE);
            leaderView.setVisibility(View.VISIBLE);
            setupLeaderView(view);
            swipeRefresh.setOnRefreshListener(this::loadLeaderData);
            loadLeaderData();
        } else {
            memberView.setVisibility(View.VISIBLE);
            leaderView.setVisibility(View.GONE);
            setupMemberView(view);
            swipeRefresh.setOnRefreshListener(this::loadMemberTasks);
            loadMemberTasks();
        }
    }

    // ── MEMBER ───────────────────────────────────────────────────────────────

    private void setupMemberView(View view) {
        layoutPending   = view.findViewById(R.id.layoutPendingTasks);
        layoutSubmitted = view.findViewById(R.id.layoutSubmittedTasks);
        layoutRerecord  = view.findViewById(R.id.layoutRerecordTasks);
        layoutApproved  = view.findViewById(R.id.layoutApprovedTasks);
    }

    private void loadMemberTasks() {
        if (userId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getTasks(userId)
                .enqueue(new Callback<List<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskResponse>> call,
                                           Response<List<TaskResponse>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        clearMemberSections();
                        if (response.body() == null) return;
                        for (TaskResponse task : response.body()) {
                            addMemberTaskRow(task);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TaskResponse>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void clearMemberSections() {
        layoutPending.removeAllViews();
        layoutSubmitted.removeAllViews();
        layoutRerecord.removeAllViews();
        layoutApproved.removeAllViews();
    }

    private void addMemberTaskRow(TaskResponse task) {
        LinearLayout target;
        switch (task.getStatus()) {
            case "submitted": target = layoutSubmitted; break;
            case "rerecord":  target = layoutRerecord;  break;
            case "approved":  target = layoutApproved;  break;
            default:          target = layoutPending;   break;
        }

        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_row, target, false);
        ((TextView) row.findViewById(R.id.tvTaskTitle))
                .setText(task.getTitle() != null ? task.getTitle() : "Task");

        View dot = row.findViewById(R.id.viewTaskDot);
        TextView pill = row.findViewById(R.id.tvTaskStatusPill);
        pill.setVisibility(View.VISIBLE);

        switch (task.getStatus()) {
            case "approved":
                dot.setBackgroundResource(R.drawable.dot_green);
                pill.setText("Approved");
                pill.setBackgroundResource(R.drawable.dot_green);
                pill.setTextColor(requireContext().getColor(R.color.white));
                break;
            case "submitted":
                dot.setBackgroundResource(R.drawable.dot_amber);
                pill.setText("Awaiting");
                pill.setBackgroundColor(0xFFF5A623);
                pill.setTextColor(0xFF5C3A00);
                break;
            case "rerecord":
                dot.setBackgroundResource(R.drawable.dot_amber);
                pill.setText("Rerecord");
                pill.setBackgroundColor(0xFFEF4B3C);
                pill.setTextColor(0xFFFDF1E3);
                break;
            default:
                dot.setBackgroundResource(R.drawable.dot_purple);
                pill.setText("Pending");
                pill.setBackgroundColor(0x22EF4B3C);
                pill.setTextColor(requireContext().getColor(R.color.purple_primary));
                break;
        }

        row.setOnClickListener(v -> {
            TaskDetailBottomSheet sheet = TaskDetailBottomSheet.newInstance(
                    task,
                    task.getSessionSong() != null ? task.getSessionSong() : "",
                    task.getSessionDate() != null ? task.getSessionDate() : "");
            sheet.show(getParentFragmentManager(), "task_detail");
        });

        target.addView(row);
    }

    // ── LEADER ───────────────────────────────────────────────────────────────

    private void setupLeaderView(View view) {
        toggleGroup        = view.findViewById(R.id.toggleTaskGroup);
        btnToggleManage    = view.findViewById(R.id.btnToggleManage);
        btnToggleSubmissions = view.findViewById(R.id.btnToggleSubmissions);
        viewManage         = view.findViewById(R.id.viewManage);
        viewSubmissions    = view.findViewById(R.id.viewSubmissions);
        layoutManageSessions = view.findViewById(R.id.layoutManageSessions);
        tvManageEmpty      = view.findViewById(R.id.tvManageEmpty);
        layoutSubmissionsList = view.findViewById(R.id.layoutSubmissionsList);
        tvSubmissionsEmpty = view.findViewById(R.id.tvSubmissionsEmpty);

        // Default: Manage tab selected
        showManage();

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnToggleManage) {
                showManage();
            } else if (checkedId == R.id.btnToggleSubmissions) {
                showSubmissions();
            }
        });
    }

    private void showManage() {
        viewManage.setVisibility(View.VISIBLE);
        viewSubmissions.setVisibility(View.GONE);
    }

    private void showSubmissions() {
        viewManage.setVisibility(View.GONE);
        viewSubmissions.setVisibility(View.VISIBLE);
    }

    private void loadLeaderData() {
        loadManageTasks();
        loadSubmissions();
    }

    private void loadManageTasks() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getTasksByBand(bandId)
                .enqueue(new Callback<List<com.example.hisync.dto.SessionTasksDto>>() {
                    @Override
                    public void onResponse(Call<List<com.example.hisync.dto.SessionTasksDto>> call,
                                           Response<List<com.example.hisync.dto.SessionTasksDto>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        layoutManageSessions.removeAllViews();
                        if (response.body() == null || response.body().isEmpty()) {
                            tvManageEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvManageEmpty.setVisibility(View.GONE);
                        for (com.example.hisync.dto.SessionTasksDto session : response.body()) {
                            addManageSessionGroup(session);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<com.example.hisync.dto.SessionTasksDto>> call,
                                          Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void addManageSessionGroup(com.example.hisync.dto.SessionTasksDto session) {
        View group = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_session_group, layoutManageSessions, false);

        ((TextView) group.findViewById(R.id.tvTaskSessionTitle))
                .setText(session.getSongTitle());

        String dateStr = "—";
        if (session.getSessionDate() != null) {
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(
                        session.getSessionDate().substring(0, 19),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                dateStr = dt.format(
                        java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"));
            } catch (Exception ignored) {}
        }
        ((TextView) group.findViewById(R.id.tvTaskSessionDate)).setText(dateStr);

        LinearLayout layoutTasks = group.findViewById(R.id.layoutGroupTasks);
        if (session.getTasks() != null) {
            for (TaskResponse task : session.getTasks()) {
                addManageTaskRow(layoutTasks, task, session.getSessionId());
            }
        }

        group.findViewById(R.id.btnAddTask).setOnClickListener(v -> {
            CreateTaskBottomSheet sheet = CreateTaskBottomSheet.newInstance(
                    session.getSessionId(), session.getSongTitle(), bandId, -1, null);
            sheet.setOnSavedListener(this::loadLeaderData);
            sheet.show(getParentFragmentManager(), "create_task");
        });

        layoutManageSessions.addView(group);
    }

    private void addManageTaskRow(LinearLayout parent, TaskResponse task, long sessionId) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_manage_task_row, parent, false);

        ((TextView) row.findViewById(R.id.tvManageTaskTitle)).setText(task.getTitle());
        ((TextView) row.findViewById(R.id.tvManageTaskAssignee))
                .setText(task.getAssignedToName());

        TextView tvStatus = row.findViewById(R.id.tvManageTaskStatus);
        bindStatusBadge(tvStatus, task.getStatus());

        row.findViewById(R.id.btnEditTask).setOnClickListener(v -> {
            CreateTaskBottomSheet sheet = CreateTaskBottomSheet.newInstance(
                    sessionId, "", bandId, task.getId(), task.getTitle());
            sheet.setOnSavedListener(this::loadLeaderData);
            sheet.show(getParentFragmentManager(), "edit_task");
        });

        row.findViewById(R.id.btnDeleteTask).setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete task")
                        .setMessage("Delete \"" + task.getTitle() + "\"?")
                        .setPositiveButton("Delete", (d, w) -> deleteTask(task.getId()))
                        .setNegativeButton("Cancel", null)
                        .show());

        parent.addView(row);
    }

    private void deleteTask(long taskId) {
        RetrofitClient.getInstance().getApi()
                .deleteTask(taskId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) loadLeaderData();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    private void loadSubmissions() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getSubmissions(bandId)
                .enqueue(new Callback<List<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskResponse>> call,
                                           Response<List<TaskResponse>> response) {
                        if (!isAdded()) return;
                        layoutSubmissionsList.removeAllViews();
                        if (response.body() == null || response.body().isEmpty()) {
                            tvSubmissionsEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvSubmissionsEmpty.setVisibility(View.GONE);
                        for (TaskResponse task : response.body()) {
                            addSubmissionRow(task);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<TaskResponse>> call, Throwable t) {}
                });
    }

    private void addSubmissionRow(TaskResponse task) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_submission_row, layoutSubmissionsList, false);

        ((TextView) row.findViewById(R.id.tvSubmissionTitle)).setText(task.getTitle());
        ((TextView) row.findViewById(R.id.tvSubmissionAssignee))
                .setText(task.getAssignedToName());
        ((TextView) row.findViewById(R.id.tvSubmissionSong))
                .setText(task.getSessionSong() != null ? task.getSessionSong() : "");

        row.setOnClickListener(v -> {
            SubmissionReviewBottomSheet sheet =
                    SubmissionReviewBottomSheet.newInstance(task);
            sheet.setOnReviewedListener(this::loadLeaderData);
            sheet.show(getParentFragmentManager(), "submission_review");
        });

        layoutSubmissionsList.addView(row);
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
                tv.setBackgroundColor(0xFF4CAF50);
                tv.setTextColor(requireContext().getColor(R.color.white));
                break;
            case "rerecord":
                tv.setText("Rerecord");
                tv.setBackgroundColor(0xFFEF4B3C);
                tv.setTextColor(requireContext().getColor(R.color.white));
                break;
            default:
                tv.setText("Pending");
                tv.setBackgroundResource(R.drawable.bg_role_member);
                tv.setTextColor(requireContext().getColor(R.color.purple_primary));
                break;
        }
    }
}