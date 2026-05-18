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

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TasksFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutPending, layoutDone;
    private long userId;

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
        userId = prefs.getLong("userId", -1);

        swipeRefresh  = view.findViewById(R.id.swipeRefreshTasks);
        layoutPending = view.findViewById(R.id.layoutPendingTasks);
        layoutDone    = view.findViewById(R.id.layoutDoneTasks);

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadTasks);

        loadTasks();
    }

    private void loadTasks() {
        if (userId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getTasks(userId)
                .enqueue(new Callback<List<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskResponse>> call,
                                           Response<List<TaskResponse>> response) {
                        if (!isAdded()) return;
                        layoutPending.removeAllViews();
                        layoutDone.removeAllViews();
                        swipeRefresh.setRefreshing(false);
                        if (response.body() == null) return;

                        for (TaskResponse task : response.body()) {
                            boolean isDone = "done".equals(task.getStatus());
                            LinearLayout target = isDone ? layoutDone : layoutPending;
                            View row = LayoutInflater.from(requireContext())
                                    .inflate(R.layout.item_task_row, target, false);
                            ((TextView) row.findViewById(R.id.tvTaskTitle))
                                    .setText(task.getTitle() != null ? task.getTitle() : "Task");
                            View dot = row.findViewById(R.id.viewTaskDot);
                            if (isDone)
                                dot.setBackgroundResource(R.drawable.dot_green);
                            else if ("rerecord".equals(task.getStatus()))
                                dot.setBackgroundResource(R.drawable.dot_amber);
                            else
                                dot.setBackgroundResource(R.drawable.dot_purple);
                            target.addView(row);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TaskResponse>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }
}