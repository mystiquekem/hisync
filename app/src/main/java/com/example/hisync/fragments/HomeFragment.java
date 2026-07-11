package com.example.hisync.fragments;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.SessionTasksDto;
import com.example.hisync.dto.TaskResponse;
import com.example.hisync.dto.BandResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvAvatar, tvUserName, tvBandName, tvRoleBadge;
    private TextView tvSessionDate, tvSessionSong, tvEmptySession;
    private TextView tvStatSessions, tvStatTasks, tvStatMembers;
    private TextView tvProgressLabel, tvProgressEncourage;
    private LinearLayout cardNextSession, layoutTasks;
    private LinearLayout layoutCountdown;
    private TextView tvDaysCount;
    private View viewProgressFill;
    private long userId, bandId;
    private String displayName, email, userRole, bandName;

    private LinearLayout layoutEmptyNoTasks;
    private View layoutEmptyAllDone;
    private TextView tvEmptyNoTasksEmoji, tvEmptyNoTasksMsg, tvEmptyNoTasksSub, tvAllDoneSub;
    private com.google.android.material.button.MaterialButton btnEmptyAction;

    private static final DateTimeFormatter API_FMT  =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter SHOW_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a");

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
        bandId      = prefs.getLong("bandId", -1);
        displayName = prefs.getString("displayName", "");
        email       = prefs.getString("email", "");
        userRole    = prefs.getString("role", "member");
        bandName    = prefs.getString("bandName", "");

        swipeRefresh      = view.findViewById(R.id.swipeRefresh);
        tvAvatar          = view.findViewById(R.id.tvAvatar);
        tvUserName        = view.findViewById(R.id.tvUserName);
        tvBandName        = view.findViewById(R.id.tvBandName);
        tvRoleBadge       = view.findViewById(R.id.tvRoleBadge);
        tvSessionDate     = view.findViewById(R.id.tvSessionDate);
        tvSessionSong     = view.findViewById(R.id.tvSessionSong);
        tvEmptySession    = view.findViewById(R.id.tvEmptySession);
        cardNextSession   = view.findViewById(R.id.cardNextSession);
        layoutTasks       = view.findViewById(R.id.layoutTasks);
        tvStatSessions    = view.findViewById(R.id.tvStatSessions);
        tvStatTasks       = view.findViewById(R.id.tvStatTasks);
        tvStatMembers     = view.findViewById(R.id.tvStatMembers);
        tvProgressLabel   = view.findViewById(R.id.tvProgressLabel);
        tvProgressEncourage = view.findViewById(R.id.tvProgressEncourage);
        viewProgressFill  = view.findViewById(R.id.viewProgressFill);
        layoutCountdown   = view.findViewById(R.id.layoutCountdown);
        tvDaysCount       = view.findViewById(R.id.tvDaysCount);

        layoutEmptyNoTasks   = view.findViewById(R.id.layoutEmptyNoTasks);
        layoutEmptyAllDone   = view.findViewById(R.id.layoutEmptyAllDone);
        tvEmptyNoTasksEmoji  = view.findViewById(R.id.tvEmptyNoTasksEmoji);
        tvEmptyNoTasksMsg    = view.findViewById(R.id.tvEmptyNoTasksMsg);
        tvEmptyNoTasksSub    = view.findViewById(R.id.tvEmptyNoTasksSub);
        tvAllDoneSub         = view.findViewById(R.id.tvAllDoneSub);
        btnEmptyAction       = view.findViewById(R.id.btnEmptyAction);

        btnEmptyAction.setOnClickListener(v -> {
            // Jump to Schedule tab — position 1 for member, 2 for leader
            boolean isLeader = "leader".equals(userRole) || "admin".equals(userRole);
            int schedulePos = isLeader ? 2 : 1;
            if (getActivity() instanceof com.example.hisync.MainActivity) {
                ((com.example.hisync.MainActivity) getActivity())
                        .navigateTo(schedulePos);
            }
        });

        swipeRefresh.setColorSchemeColors(0xFFD4603A, 0xFF3A9BAD, 0xFFF5A623);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadData);

        populateHeader();
        animateEntrance(view);
        loadData();
    }

    private void populateHeader() {
        String name = (displayName != null && !displayName.isEmpty())
                ? displayName : (email != null ? email.split("@")[0] : "?");
        tvUserName.setText("Hey, " + name + "! ✦");
        tvAvatar.setText(name.isEmpty() ? "?" :
                String.valueOf(name.charAt(0)).toUpperCase());
        tvBandName.setText((bandName != null && !bandName.isEmpty())
                ? "🎸 " + bandName : "No band yet");

        boolean isLeader = "leader".equals(userRole) || "admin".equals(userRole);
        tvRoleBadge.setText(isLeader ? "Leader" : "Member");
    }

    private void animateEntrance(View root) {
        // Staggered slide-up + fade for each major section
        int[] ids = {
                R.id.tvAvatar,
                R.id.cardNextSession,
                R.id.tvStatSessions
        };
        // Animate the three cards with offset delays
        View[] cards = {
                root.findViewById(R.id.tvUserName).getRootView()
        };

        long delay = 0;
        for (int i = 0; i < 4; i++) {
            final long d = delay;
            // We'll animate via alpha on the parent LinearLayout children
            delay += 100;
        }

        // Animate the whole scroll content with a single slide-up
        View scrollContent = root.findViewById(R.id.layoutTasks);
        if (scrollContent != null && scrollContent.getParent() instanceof View) {
            View parent = (View) scrollContent.getParent().getParent();
            parent.setAlpha(0f);
            parent.setTranslationY(30f);
            parent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void loadData() {
        loadNextSession();
        loadMyTasks();
        loadBandProgress();
    }

    private void loadNextSession() {
        String from = LocalDate.now().atStartOfDay().format(API_FMT);
        String to   = LocalDate.now().plusDays(30).atStartOfDay().format(API_FMT);

        RetrofitClient.getInstance().getApi()
                .getSessionsByUser(userId, from, to)
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
                                    String raw = next.getDate();
                                    if (raw.length() > 19) raw = raw.substring(0, 19);
                                    LocalDateTime dt = LocalDateTime.parse(raw, API_FMT);
                                    tvSessionDate.setText(dt.format(SHOW_FMT));

                                    // Days countdown
                                    long days = ChronoUnit.DAYS.between(
                                            LocalDate.now(), dt.toLocalDate());
                                    if (days >= 0) {
                                        tvDaysCount.setText(String.valueOf(days));
                                        layoutCountdown.setVisibility(View.VISIBLE);
                                    }
                                } catch (Exception ignored) {}
                            }

                            cardNextSession.setVisibility(View.VISIBLE);
                            tvEmptySession.setVisibility(View.GONE);

                            // Animate session card in
                            cardNextSession.setAlpha(0f);
                            cardNextSession.setTranslationX(-20f);
                            cardNextSession.animate()
                                    .alpha(1f)
                                    .translationX(0f)
                                    .setDuration(350)
                                    .setInterpolator(new DecelerateInterpolator())
                                    .start();
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

    private void loadMyTasks() {
        RetrofitClient.getInstance().getApi()
                .getTasks(userId)
                .enqueue(new Callback<List<TaskResponse>>() {
                    @Override
                    public void onResponse(Call<List<TaskResponse>> call,
                                           Response<List<TaskResponse>> response) {
                        if (!isAdded()) return;
                        layoutTasks.removeAllViews();
                        layoutEmptyNoTasks.setVisibility(View.GONE);
                        layoutEmptyAllDone.setVisibility(View.GONE);

                        List<TaskResponse> tasks = response.body();

                        // Case A: no tasks at all
                        if (tasks == null || tasks.isEmpty()) {
                            showEmptyNoTasks();
                            tvStatTasks.setText("0");
                            return;
                        }

                        tvStatTasks.setText(String.valueOf(tasks.size()));

                        // Case B: all tasks approved
                        long approvedCount = tasks.stream()
                                .filter(t -> "approved".equals(t.getStatus()))
                                .count();

                        if (approvedCount == tasks.size()) {
                            showEmptyAllDone(tasks.size());
                            return;
                        }

                        // Normal: show task rows
                        int delay = 0;
                        for (TaskResponse task : tasks) {
                            addTaskRow(task, delay);
                            delay += 60;
                        }
                    }

                    @Override
                    public void onFailure(Call<List<TaskResponse>> call, Throwable t) {}
                });
    }

    private void showEmptyNoTasks() {
        boolean isLeader = "leader".equals(userRole) || "admin".equals(userRole);

        if (isLeader) {
            tvEmptyNoTasksEmoji.setText("🎸");
            tvEmptyNoTasksMsg.setText("Your band has no tasks yet.");
            tvEmptyNoTasksSub.setText("Head to the Band tab to assign tasks to your members.");
        } else {
            tvEmptyNoTasksEmoji.setText("🎵");
            tvEmptyNoTasksMsg.setText("Nothing on your plate!");
            tvEmptyNoTasksSub.setText("Enjoy the quiet before rehearsal. Your leader will assign tasks soon.");
        }

        layoutEmptyNoTasks.setVisibility(View.VISIBLE);

        // Bounce animation on emoji
        tvEmptyNoTasksEmoji.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(400)
                .withEndAction(() -> tvEmptyNoTasksEmoji.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .start())
                .start();

        // Fade in the card
        layoutEmptyNoTasks.setAlpha(0f);
        layoutEmptyNoTasks.animate()
                .alpha(1f)
                .setDuration(400)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void showEmptyAllDone(int total) {
        boolean isLeader = "leader".equals(userRole) || "admin".equals(userRole);

        tvAllDoneSub.setText(isLeader
                ? "All " + total + " tasks approved. The band is in great shape! 🔥"
                : "All " + total + " of your tasks are approved. Great work! 🔥");

        // Leader action button → Manage Tasks, member → Schedule
        boolean leader = "leader".equals(userRole) || "admin".equals(userRole);
        btnEmptyAction.setText(leader ? "Manage Tasks" : "View Schedule");
        btnEmptyAction.setOnClickListener(v -> {
            int pos = leader ? 3 : 1; // Tasks pos for leader, Schedule for member
            if (getActivity() instanceof com.example.hisync.MainActivity) {
                ((com.example.hisync.MainActivity) getActivity()).navigateTo(pos);
            }
        });

        layoutEmptyAllDone.setVisibility(View.VISIBLE);

        // Slide up animation
        layoutEmptyAllDone.setAlpha(0f);
        layoutEmptyAllDone.setTranslationY(20f);
        layoutEmptyAllDone.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void loadBandProgress() {
        if (bandId == -1) return;

        RetrofitClient.getInstance().getApi()
                .getTasksByBand(bandId)
                .enqueue(new Callback<List<SessionTasksDto>>() {
                    @Override
                    public void onResponse(Call<List<SessionTasksDto>> call,
                                           Response<List<SessionTasksDto>> response) {
                        if (!isAdded() || response.body() == null) return;

                        int total = 0, approved = 0;
                        int sessionCount = response.body().size();

                        for (SessionTasksDto s : response.body()) {
                            if (s.getTasks() != null) {
                                total += s.getTasks().size();
                                for (TaskResponse t : s.getTasks()) {
                                    if ("approved".equals(t.getStatus())) approved++;
                                }
                            }
                        }

                        tvStatSessions.setText(String.valueOf(sessionCount));

                        // Replace tvStatMembers.setText("—") with this:
                        RetrofitClient.getInstance().getApi()
                                .getBand(bandId)
                                .enqueue(new Callback<BandResponse>() {
                                    @Override
                                    public void onResponse(Call<BandResponse> call,
                                                           Response<BandResponse> response) {
                                        if (!isAdded() || response.body() == null) return;
                                        int count = response.body().getMembers() != null
                                                ? response.body().getMembers().size() : 0;
                                        tvStatMembers.setText(String.valueOf(count));
                                    }
                                    @Override
                                    public void onFailure(Call<BandResponse> call, Throwable t) {
                                        if (isAdded()) tvStatMembers.setText("—");
                                    }
                                });

                        final int finalApproved = approved;
                        final int finalTotal = total;

                        tvProgressLabel.setText(approved + "/" + total + " tasks");

                        // Encouragement message
                        float pct = total == 0 ? 0 : (float) approved / total;
                        if (pct == 0)       tvProgressEncourage.setText("Let's get started! 🎸");
                        else if (pct < 0.4) tvProgressEncourage.setText("Keep going! 💪");
                        else if (pct < 0.7) tvProgressEncourage.setText("Great progress! 🔥");
                        else if (pct < 1.0) tvProgressEncourage.setText("Almost there! ⭐");
                        else                tvProgressEncourage.setText("All done! 🎉");

                        // Animate progress bar
                        if (total > 0) {
                            animateProgressBar(approved, total);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SessionTasksDto>> call, Throwable t) {}
                });
    }

    private void animateProgressBar(int approved, int total) {
        if (!isAdded() || viewProgressFill == null) return;

        viewProgressFill.post(() -> {
            int parentWidth = ((View) viewProgressFill.getParent()).getWidth();
            int targetWidth = (int) ((float) approved / total * parentWidth);

            ValueAnimator animator = ValueAnimator.ofInt(0, targetWidth);
            animator.setDuration(900);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(anim -> {
                if (!isAdded()) return;
                ViewGroup.LayoutParams lp = viewProgressFill.getLayoutParams();
                lp.width = (int) anim.getAnimatedValue();
                viewProgressFill.setLayoutParams(lp);
            });
            animator.start();
        });
    }

    private void addTaskRow(TaskResponse task, int animDelay) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_task_row, layoutTasks, false);

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

        // Tap to open detail
        row.setOnClickListener(v -> {
            TaskDetailBottomSheet sheet = TaskDetailBottomSheet.newInstance(
                    task,
                    task.getSessionSong() != null ? task.getSessionSong() : "",
                    task.getSessionDate() != null ? task.getSessionDate() : ""
            );
            sheet.show(getParentFragmentManager(), "task_detail");
        });

        // Staggered slide-in animation
        row.setAlpha(0f);
        row.setTranslationY(16f);
        row.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(animDelay)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        layoutTasks.addView(row);
    }
}