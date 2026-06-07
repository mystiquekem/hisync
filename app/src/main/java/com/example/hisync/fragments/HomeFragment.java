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

import com.example.hisync.MainActivity;
import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvAvatar, tvUserName, tvUserEmail;
    private TextView tvSessionDate, tvSessionSong, tvEmptySession;
    private LinearLayout cardNextSession, layoutTasks, tvEmptyTasks;

    private long userId;
    private String displayName, email;

    // thêm field
    private TextView tvRoleBadge, tvBandName;
    private String userRole, bandName;


    private static final DateTimeFormatter API_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter SHOW_FMT =
            DateTimeFormatter.ofPattern("EEEE · h:mm a");

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

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        userId      = prefs.getLong("userId", -1);
        displayName = prefs.getString("displayName", "");
        email       = prefs.getString("email", "");
        // trong onViewCreated, thêm:
        userRole = prefs.getString("role", "member");
        bandName = prefs.getString("bandName", "");
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge);
        tvBandName  = view.findViewById(R.id.tvBandName);

        swipeRefresh    = view.findViewById(R.id.swipeRefresh);
        tvAvatar        = view.findViewById(R.id.tvAvatar);
        tvUserName      = view.findViewById(R.id.tvUserName);
        tvSessionDate   = view.findViewById(R.id.tvSessionDate);
        tvSessionSong   = view.findViewById(R.id.tvSessionSong);
        cardNextSession = view.findViewById(R.id.cardNextSession);
        layoutTasks     = view.findViewById(R.id.layoutTasks);
        tvEmptySession  = view.findViewById(R.id.tvEmptySession);
        tvEmptyTasks    = view.findViewById(R.id.tvEmptyTasks);


        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadData);

        populateHeader();
        loadData();
    }

    private void populateHeader() {
        String name = (displayName != null && !displayName.isEmpty())
                ? displayName : email.split("@")[0];
        tvUserName.setText(name);
        tvAvatar.setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        // role badge
        boolean isLeader = "leader".equals(userRole) || "admin".equals(userRole);
        tvRoleBadge.setText(isLeader ? "Leader" : "Member");
        tvRoleBadge.setBackgroundResource(isLeader
                ? R.drawable.bg_role_leader : R.drawable.bg_role_member);
        tvRoleBadge.setTextColor(requireContext().getColor(
                isLeader ? android.R.color.holo_orange_dark : R.color.purple_primary));

        // band name
        tvBandName.setText(bandName != null && !bandName.isEmpty() ? bandName : "No band");
    }
    private void loadData() {
        loadNextSession();
        loadTasks();
    }

    private void loadNextSession() {
        String from = LocalDate.now().atStartOfDay().format(API_FMT);
        String to   = LocalDate.now().plusDays(30).atStartOfDay().format(API_FMT);

        RetrofitClient.getInstance().getApi()
                .getSessions(userId, from, to)
                .enqueue(new Callback<List<SessionResponse>>() {
                    @Override
                    public void onResponse(Call<List<SessionResponse>> call,
                                           Response<List<SessionResponse>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        if (response.body() != null && !response.body().isEmpty()) {
                            SessionResponse next = response.body().get(0);
                            tvSessionSong.setText(next.getSongTitle());
                            if (next.getDate() != null) {
                                try {
                                    LocalDateTime dt = LocalDateTime.parse(
                                            next.getDate().substring(0, 19), API_FMT);
                                    tvSessionDate.setText(dt.format(SHOW_FMT));
                                } catch (Exception ignored) {}
                            }
                            cardNextSession.setVisibility(View.VISIBLE);
                            tvEmptySession.setVisibility(View.GONE);
                        } else {
                            cardNextSession.setVisibility(View.GONE);
                            tvEmptySession.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SessionResponse>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void loadTasks() {
        RetrofitClient.getInstance().getApi()
                .getTasks(userId)
                .enqueue(new Callback<List<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskResponse>> call,
                                           Response<List<TaskResponse>> response) {
                        if (!isAdded()) return;
                        layoutTasks.removeAllViews();
                        if (response.body() == null || response.body().isEmpty()) {
                            tvEmptyTasks.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvEmptyTasks.setVisibility(View.GONE);
                        for (TaskResponse task : response.body()) {
                            addTaskRow(task.getTitle() != null ? task.getTitle() : "Task",
                                    task.getStatus());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TaskResponse>> call, Throwable t) { }
                });
    }

    private void addTaskRow(String title, String status) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_row, layoutTasks, false);
        ((TextView) row.findViewById(R.id.tvTaskTitle)).setText(title);
        View dot = row.findViewById(R.id.viewTaskDot);
        if ("done".equals(status))          dot.setBackgroundResource(R.drawable.dot_green);
        else if ("rerecord".equals(status)) dot.setBackgroundResource(R.drawable.dot_amber);
        else                                dot.setBackgroundResource(R.drawable.dot_purple);
        layoutTasks.addView(row);
    }
}