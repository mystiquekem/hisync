package com.example.hisync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LineupResponse;
import com.example.hisync.dto.SessionResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateSessionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BAND_ID  = "band_id";
    private static final String ARG_USER_ID  = "user_id";
    private static final DateTimeFormatter API_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a");
    private static final int[] DURATIONS = {15, 30, 45, 60, 75, 90, 105, 120};

    private long bandId, userId;
    private Runnable onCreated;

    private List<LineupResponse> lineups = new ArrayList<>();
    private long selectedLineupId = -1;
    private LocalDate selectedDate;
    private LocalTime selectedTime;
    private int selectedDuration = 60;

    private Spinner spinnerLineup, spinnerDuration;
    private TextView tvDateTime;
    private MaterialButton btnPickDateTime, btnCreateSession;

    public static CreateSessionBottomSheet newInstance(long bandId, long userId) {
        CreateSessionBottomSheet sheet = new CreateSessionBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_BAND_ID, bandId);
        args.putLong(ARG_USER_ID, userId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnCreatedListener(Runnable listener) { this.onCreated = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_session, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bandId = requireArguments().getLong(ARG_BAND_ID);
        userId = requireArguments().getLong(ARG_USER_ID);

        spinnerLineup   = view.findViewById(R.id.spinnerLineup);
        spinnerDuration = view.findViewById(R.id.spinnerDuration);
        tvDateTime      = view.findViewById(R.id.tvSessionDateTime);
        btnPickDateTime = view.findViewById(R.id.btnPickDateTime);
        btnCreateSession= view.findViewById(R.id.btnCreateSession);

        // Duration spinner
        String[] durationLabels = new String[DURATIONS.length];
        for (int i = 0; i < DURATIONS.length; i++)
            durationLabels[i] = DURATIONS[i] + " min";
        ArrayAdapter<String> durAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, durationLabels);
        durAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDuration.setAdapter(durAdapter);
        spinnerDuration.setSelection(3); // default 60 min
        spinnerDuration.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                selectedDuration = DURATIONS[pos];
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        // Date + time picker
        btnPickDateTime.setOnClickListener(v -> showDatePicker());

        btnCreateSession.setOnClickListener(v -> createSession());

        loadLineups();
    }

    private void loadLineups() {
        RetrofitClient.getInstance().getApi()
                .getLineups(bandId)
                .enqueue(new Callback<List<LineupResponse>>() {
                    @Override
                    public void onResponse(Call<List<LineupResponse>> call,
                                           Response<List<LineupResponse>> response) {
                        if (!isAdded() || response.body() == null) return;
                        lineups = response.body();
                        if (lineups.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "Create a lineup first", Toast.LENGTH_SHORT).show();
                            dismiss();
                            return;
                        }
                        List<String> titles = new ArrayList<>();
                        for (LineupResponse l : lineups) titles.add(l.getSongTitle());
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_item, titles);
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        spinnerLineup.setAdapter(adapter);
                        selectedLineupId = lineups.get(0).getId();
                        spinnerLineup.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> p,
                                                               View v, int pos, long id) {
                                        selectedLineupId = lineups.get(pos).getId();
                                    }
                                    @Override
                                    public void onNothingSelected(AdapterView<?> p) { }
                                });
                    }

                    @Override
                    public void onFailure(Call<List<LineupResponse>> call, Throwable t) { }
                });
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder
                .datePicker()
                .setTitleText("Pick session date")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.of("UTC")).toLocalDate();
            showTimePicker();
        });

        datePicker.show(getParentFragmentManager(), "date_picker");
    }

    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(19)
                .setMinute(0)
                .setTitleText("Pick session time")
                .build();

        timePicker.addOnPositiveButtonClickListener(v -> {
            selectedTime = LocalTime.of(timePicker.getHour(), timePicker.getMinute());
            LocalDateTime dt = LocalDateTime.of(selectedDate, selectedTime);
            tvDateTime.setText(dt.format(DISPLAY_FMT));
            tvDateTime.setVisibility(View.VISIBLE);
        });

        timePicker.show(getParentFragmentManager(), "time_picker");
    }

    private void createSession() {
        if (selectedLineupId == -1) {
            Toast.makeText(requireContext(), "Pick a lineup", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate == null || selectedTime == null) {
            Toast.makeText(requireContext(), "Pick a date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateSession.setEnabled(false);
        LocalDateTime dt = LocalDateTime.of(selectedDate, selectedTime);

        Map<String, Object> body = new HashMap<>();
        body.put("bandId", bandId);
        body.put("lineupId", selectedLineupId);
        body.put("date", dt.format(API_FMT));
        body.put("durationMinutes", selectedDuration);
        body.put("createdBy", userId);

        RetrofitClient.getInstance().getApi()
                .createSession(body)
                .enqueue(new Callback<SessionResponse>() {
                    @Override
                    public void onResponse(Call<SessionResponse> call,
                                           Response<SessionResponse> response) {
                        btnCreateSession.setEnabled(true);
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(),
                                    "Session created!", Toast.LENGTH_SHORT).show();
                            if (onCreated != null) onCreated.run();
                            dismiss();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to create session", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<SessionResponse> call, Throwable t) {
                        btnCreateSession.setEnabled(true);
                        if (isAdded())
                            Toast.makeText(requireContext(),
                                    "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}