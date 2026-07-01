package com.example.hisync.schedule;

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

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SessionResponse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeekFragment extends Fragment {

    private static final String ARG_WEEK_START = "week_start";
    private static final DateTimeFormatter DAY_FMT   = DateTimeFormatter.ofPattern("EEE\nd");
    private static final DateTimeFormatter LABEL_FMT  = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter API_FMT    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private LocalDate weekStart;
    private long currentUserId;

    private final TextView[]     dayHeaders = new TextView[7];
    private final LinearLayout[] dayColumns = new LinearLayout[7];

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
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        currentUserId = prefs.getLong("userId", -1);
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

        int[] headerIds = {R.id.tvMon, R.id.tvTue, R.id.tvWed,
                R.id.tvThu, R.id.tvFri, R.id.tvSat, R.id.tvSun};
        int[] colIds    = {R.id.colMon, R.id.colTue, R.id.colWed,
                R.id.colThu, R.id.colFri, R.id.colSat, R.id.colSun};

        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            dayHeaders[i] = view.findViewById(headerIds[i]);
            dayColumns[i] = view.findViewById(colIds[i]);
            LocalDate day = weekStart.plusDays(i);
            dayHeaders[i].setText(day.format(DAY_FMT));
            if (day.equals(today)) {
                dayHeaders[i].setBackgroundResource(R.drawable.bg_day_today);
                dayHeaders[i].setTextColor(requireContext().getColor(R.color.white));
            } else {
                dayHeaders[i].setBackgroundResource(0);
                dayHeaders[i].setTextColor(requireContext().getColor(R.color.text_tertiary));
            }
        }

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
                        for (SessionResponse session : response.body()) {
                            placeSessionCard(session);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<SessionResponse>> call, Throwable t) {
                        // Silently fail — tuần khác vẫn load bình thường
                    }
                });
    }

    private void placeSessionCard(SessionResponse session) {
        if (session.getDate() == null) return;

        // Parse ISO date string từ backend
        LocalDate sessionDate;
        try {
            sessionDate = LocalDate.parse(session.getDate().substring(0, 10));
        } catch (Exception e) {
            return;
        }

        int dayIndex = sessionDate.getDayOfWeek().getValue() - 1; // Mon=0
        if (dayIndex < 0 || dayIndex > 6) return;

        LinearLayout col = dayColumns[dayIndex];
        if (col == null || !isAdded()) return;

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_chip, col, false);
        ((TextView) card.findViewById(R.id.tvChipTitle)).setText(session.getSongTitle());
        card.setOnClickListener(v -> {
            SessionDetailBottomSheet sheet =
                    SessionDetailBottomSheet.newInstance(session.getId(), session.getSongTitle());
            sheet.show(getParentFragmentManager(), "session_detail");
        });
        col.addView(card);
    }
}