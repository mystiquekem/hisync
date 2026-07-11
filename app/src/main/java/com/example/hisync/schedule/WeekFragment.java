package com.example.hisync.schedule;

import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
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

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LineupMemberDto;
import com.example.hisync.dto.SessionResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeekFragment extends Fragment {

    private static final String ARG_WEEK_START = "week_start";
    private static final DateTimeFormatter DOW_FMT   = DateTimeFormatter.ofPattern("EEE");
    private static final DateTimeFormatter LABEL_FMT  = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter API_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DAY_HEADER_FMT = DateTimeFormatter.ofPattern("EEE, MMM d");
    private static final DateTimeFormatter TIME_FMT   = DateTimeFormatter.ofPattern("h:mm a");

    private static final int[] AVATAR_COLORS = {
            0xFFEF9F27, 0xFF2D8A9A, 0xFF8B6F47, 0xFFC4382C
    };

    private LocalDate weekStart;
    private LocalDate today;
    private long currentUserId;
    private boolean isLeader;

    private LinearLayout dateStrip;
    private LinearLayout agendaContainer;
    private final Map<LocalDate, View> dayNumViews = new HashMap<>();
    private final Map<LocalDate, View> dayDots = new HashMap<>();

    public static WeekFragment newInstance(String weekStartIso) {
        WeekFragment f = new WeekFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WEEK_START, weekStartIso);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weekStart = LocalDate.parse(requireArguments().getString(ARG_WEEK_START));
        today = LocalDate.now();
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
        isLeader = "leader".equals(prefs.getString("role", "member"))
                || "admin".equals(prefs.getString("role", "member"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvMonth = view.findViewById(R.id.tvMonthLabel);
        tvMonth.setText(weekStart.format(LABEL_FMT));

        dateStrip = view.findViewById(R.id.dateStrip);
        agendaContainer = view.findViewById(R.id.agendaContainer);

        buildDateStrip();
        buildEmptyAgendaSkeleton();
        loadSessions();
    }

    private void buildDateStrip() {
        dateStrip.removeAllViews();
        dayNumViews.clear();
        dayDots.clear();

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            View chip = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_date_strip_day, dateStrip, false);

            TextView tvDow = chip.findViewById(R.id.tvChipDow);
            TextView tvNum = chip.findViewById(R.id.tvChipNum);
            View dot = chip.findViewById(R.id.dotHasSession);

            tvDow.setText(day.format(DOW_FMT));
            tvNum.setText(String.valueOf(day.getDayOfMonth()));

            if (day.equals(today)) {
                tvNum.setBackgroundResource(R.drawable.bg_day_selected);
                tvNum.setTextColor(requireContext().getColor(R.color.white));
                tvDow.setTextColor(requireContext().getColor(R.color.purple_primary));
            }

            dayNumViews.put(day, tvNum);
            dayDots.put(day, dot);
            dateStrip.addView(chip);
        }
    }

    private void buildEmptyAgendaSkeleton() {
        agendaContainer.removeAllViews();
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);

            View header = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_day_header, agendaContainer, false);
            ((TextView) header).setText(
                    day.equals(today) ? "TODAY" : day.format(DAY_HEADER_FMT).toUpperCase());
            header.setTag("header_" + day);
            agendaContainer.addView(header);

            View empty = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_day_empty, agendaContainer, false);
            empty.setTag("body_" + day);
            agendaContainer.addView(empty);
        }
    }

    private View findDayBodySlot(LocalDate day) {
        String tag = "body_" + day;
        for (int i = 0; i < agendaContainer.getChildCount(); i++) {
            View child = agendaContainer.getChildAt(i);
            if (tag.equals(child.getTag())) return child;
        }
        return null;
    }

    public void refresh() {
        if (!isAdded()) return;
        buildEmptyAgendaSkeleton();
        loadSessions();
    }

    private void loadSessions() {
        if (currentUserId == -1) return;

        String from = weekStart.atStartOfDay().format(API_FMT);
        String to   = weekStart.plusDays(7).atStartOfDay().format(API_FMT);

        RetrofitClient.getInstance().getApi()
                .getSessionsByUser(currentUserId, from, to)
                .enqueue(new Callback<List<SessionResponse>>() {
                    @Override
                    public void onResponse(Call<List<SessionResponse>> call,
                                           Response<List<SessionResponse>> response) {
                        if (!isAdded() || response.body() == null) return;

                        Map<LocalDate, List<SessionResponse>> byDay = new HashMap<>();
                        for (SessionResponse s : response.body()) {
                            if (s.getDate() == null) continue;
                            LocalDate d;
                            try {
                                d = LocalDate.parse(s.getDate().substring(0, 10));
                            } catch (Exception e) {
                                continue;
                            }
                            byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(s);
                        }

                        for (Map.Entry<LocalDate, List<SessionResponse>> entry : byDay.entrySet()) {
                            renderDay(entry.getKey(), entry.getValue());
                            View dot = dayDots.get(entry.getKey());
                            if (dot != null) dot.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SessionResponse>> call, Throwable t) {
                    }
                });
    }

    private void renderDay(LocalDate day, List<SessionResponse> sessions) {
        View slot = findDayBodySlot(day);
        if (slot == null || !(slot.getParent() instanceof ViewGroup)) return;
        ViewGroup parent = (ViewGroup) slot.getParent();
        int index = parent.indexOfChild(slot);
        parent.removeView(slot);

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setTag("body_" + day);

        for (SessionResponse session : sessions) {
            if (day.equals(today)) {
                wrapper.addView(buildHeroCard(session));
            } else {
                wrapper.addView(buildRegularCard(session));
            }
            if (isLeader) {
                wrapper.addView(buildLeaderActions(session));
            }
        }

        parent.addView(wrapper, index);
    }

    private View buildHeroCard(SessionResponse session) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_hero, null, false);

        TextView tvTime = card.findViewById(R.id.tvHeroTime);
        TextView tvTitle = card.findViewById(R.id.tvHeroTitle);
        TextView tvLineup = card.findViewById(R.id.tvHeroLineup);
        LinearLayout avatarStack = card.findViewById(R.id.heroAvatarStack);

        tvTime.setText(formatTimeLine(session));
        tvTitle.setText(session.getSongTitle());

        List<LineupMemberDto> members = session.getMembers();
        int count = members != null ? members.size() : 0;
        tvLineup.setText(count + (count == 1 ? " member in lineup" : " members in lineup"));
        addAvatarStack(avatarStack, members, requireContext().getColor(R.color.purple_primary));

        card.setOnClickListener(v -> openDetail(session));
        return card;
    }

    private View buildRegularCard(SessionResponse session) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_card, null, false);

        TextView tvTime = card.findViewById(R.id.tvCardTime);
        TextView tvTitle = card.findViewById(R.id.tvCardTitle);
        TextView tvLineup = card.findViewById(R.id.tvCardLineup);

        tvTime.setText(formatTimeShort(session));
        tvTitle.setText(session.getSongTitle());

        List<LineupMemberDto> members = session.getMembers();
        int count = members != null ? members.size() : 0;
        tvLineup.setText(count + (count == 1 ? " member lineup" : " members lineup"));

        card.setOnClickListener(v -> openDetail(session));
        return card;
    }

    private View buildLeaderActions(SessionResponse session) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_leader_actions, null, false);

        View edit = row.findViewById(R.id.btnEditSession);
        View cancel = row.findViewById(R.id.btnCancelSession);

        edit.setOnClickListener(v -> {
            android.widget.Toast.makeText(requireContext(),
                    "Edit session — coming soon", android.widget.Toast.LENGTH_SHORT).show();
        });
        cancel.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cancel session?")
                    .setMessage("This will remove \"" + session.getSongTitle() + "\" from the schedule.")
                    .setPositiveButton("Cancel session", (dialog, which) -> deleteSession(session))
                    .setNegativeButton("Keep it", null)
                    .show();
        });

        return row;
    }

    private void addAvatarStack(LinearLayout stack, List<LineupMemberDto> members, int borderColor) {
        stack.removeAllViews();
        if (members == null) return;
        int shown = Math.min(members.size(), 4);
        for (int i = 0; i < shown; i++) {
            String name = members.get(i).getDisplayName();
            String initial = (name != null && !name.isEmpty())
                    ? name.substring(0, 1).toUpperCase() : "?";

            TextView av = new TextView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(24), dp(24));
            if (i > 0) lp.setMarginStart(dp(-8));
            av.setLayoutParams(lp);
            av.setGravity(android.view.Gravity.CENTER);
            av.setTextColor(requireContext().getColor(R.color.white));
            av.setTextSize(10);
            av.setText(initial);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(AVATAR_COLORS[i % AVATAR_COLORS.length]);
            bg.setStroke(dp(2), borderColor);
            av.setBackground(bg);

            stack.addView(av);
        }
    }

    private void openDetail(SessionResponse session) {
        SessionDetailBottomSheet sheet =
                SessionDetailBottomSheet.newInstance(session.getId(), session.getSongTitle());
        sheet.show(getParentFragmentManager(), "session_detail");
    }

    private String formatTimeLine(SessionResponse session) {
        try {
            LocalDateTime dt = LocalDateTime.parse(session.getDate(), API_FMT);
            return dt.format(TIME_FMT) + " · " + session.getDurationMinutes() + " min";
        } catch (Exception e) {
            return "";
        }
    }

    private String formatTimeShort(SessionResponse session) {
        try {
            LocalDateTime dt = LocalDateTime.parse(session.getDate(), API_FMT);
            return dt.format(TIME_FMT);
        } catch (Exception e) {
            return "";
        }
    }

    private void deleteSession(SessionResponse session) {
        RetrofitClient.getInstance().getApi()
                .deleteSession(session.getId())
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            buildEmptyAgendaSkeleton();
                            loadSessions();
                        } else {
                            android.widget.Toast.makeText(requireContext(),
                                    "Couldn't cancel session. Try again.",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;
                        android.widget.Toast.makeText(requireContext(),
                                "Couldn't cancel session. Check your connection.",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int dp(int value) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}