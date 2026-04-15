package com.example.hisync.schedule;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.hisync.R;
import com.example.hisync.model.Session;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeekFragment extends Fragment {

    private static final String ARG_WEEK_START = "week_start";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEE\nd");
    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");

    private LocalDate weekStart;
    private FirebaseFirestore db;
    private String currentUid;

    // day column views: index 0=Mon … 6=Sun
    private final TextView[] dayHeaders = new TextView[7];
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
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
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

        // Month label
        TextView tvMonth = view.findViewById(R.id.tvMonthLabel);
        tvMonth.setText(weekStart.format(LABEL_FMT));

        // Wire up day columns
        int[] headerIds = {R.id.tvMon, R.id.tvTue, R.id.tvWed,
                R.id.tvThu, R.id.tvFri, R.id.tvSat, R.id.tvSun};
        int[] colIds = {R.id.colMon, R.id.colTue, R.id.colWed,
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

        loadSessions(view);
    }

    private void loadSessions(View view) {
        // Convert week range to Timestamps
        Date from = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date to = Date.from(weekStart.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());

        // Query sessions where the current user is a member
        db.collection("sessions")
                .whereGreaterThanOrEqualTo("date", new com.google.firebase.Timestamp(from))
                .whereLessThan("date", new com.google.firebase.Timestamp(to))
                .get()
                .addOnSuccessListener(sessionSnaps -> {
                    List<Session> sessions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : sessionSnaps) {
                        // Filter: only show sessions the user is a member of
                        db.collection("sessions")
                                .document(doc.getId())
                                .collection("members")
                                .document(currentUid)
                                .get()
                                .addOnSuccessListener(memberSnap -> {
                                    if (memberSnap.exists()) {
                                        Session s = doc.toObject(Session.class);
                                        s.id = doc.getId();
                                        if (isAdded()) placeSessionCard(s, view);
                                    }
                                });
                    }
                });
    }

    private void placeSessionCard(Session session, View rootView) {
        if (session.date == null) return;

        LocalDate sessionDate = session.date.toDate()
                .toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        int dayIndex = sessionDate.getDayOfWeek().getValue() - 1; // Mon=0
        if (dayIndex < 0 || dayIndex > 6) return;

        LinearLayout col = dayColumns[dayIndex];
        if (col == null || !isAdded()) return;

        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_session_chip, col, false);

        TextView tvTitle = card.findViewById(R.id.tvChipTitle);
        tvTitle.setText(session.songTitle);

        card.setOnClickListener(v -> {
            SessionDetailBottomSheet sheet =
                    SessionDetailBottomSheet.newInstance(session.id, session.songTitle);
            sheet.show(getParentFragmentManager(), "session_detail");
        });

        col.addView(card);
    }
}