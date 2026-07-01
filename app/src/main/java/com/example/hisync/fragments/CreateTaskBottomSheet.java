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

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LineupMemberDto;
import com.example.hisync.dto.LineupResponse;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateTaskBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SESSION_ID   = "session_id";
    private static final String ARG_SONG_TITLE   = "song_title";
    private static final String ARG_BAND_ID      = "band_id";
    private static final String ARG_TASK_ID      = "task_id";
    private static final String ARG_TASK_TITLE   = "task_title";

    private long sessionId, bandId, taskId;
    private Runnable onSaved;

    private List<LineupMemberDto> sessionMembers = new ArrayList<>();
    private long selectedUserId = -1;

    private TextInputLayout tilTitle;
    private TextInputEditText etTitle;
    private Spinner spinnerMember;
    private MaterialButton btnSave;

    public static CreateTaskBottomSheet newInstance(
            long sessionId, String songTitle, long bandId,
            long taskId, @Nullable String existingTitle) {
        CreateTaskBottomSheet sheet = new CreateTaskBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        args.putString(ARG_SONG_TITLE, songTitle);
        args.putLong(ARG_BAND_ID, bandId);
        args.putLong(ARG_TASK_ID, taskId);
        if (existingTitle != null) args.putString(ARG_TASK_TITLE, existingTitle);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSavedListener(Runnable listener) { this.onSaved = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args   = requireArguments();
        sessionId     = args.getLong(ARG_SESSION_ID);
        bandId        = args.getLong(ARG_BAND_ID);
        taskId        = args.getLong(ARG_TASK_ID, -1);
        boolean isEdit = taskId != -1;

        tilTitle      = view.findViewById(R.id.tilTaskTitle);
        etTitle       = view.findViewById(R.id.etTaskTitle);
        spinnerMember = view.findViewById(R.id.spinnerTaskMember);
        btnSave       = view.findViewById(R.id.btnSaveTask);

        String songTitle = args.getString(ARG_SONG_TITLE, "");
        ((TextView) view.findViewById(R.id.tvTaskSheetTitle))
                .setText(isEdit ? "Edit Task" : "New Task — " + songTitle);

        if (isEdit) {
            String existing = args.getString(ARG_TASK_TITLE, "");
            etTitle.setText(existing);
            spinnerMember.setVisibility(View.GONE); // reassign not shown in edit for simplicity
        }

        btnSave.setOnClickListener(v -> save(isEdit));

        if (!isEdit) loadSessionMembers();
    }

    private void loadSessionMembers() {
        RetrofitClient.getInstance().getApi()
                .getSession(sessionId)
                .enqueue(new Callback<SessionResponse>() {
                    @Override
                    public void onResponse(Call<SessionResponse> call,
                                           Response<SessionResponse> response) {
                        if (!isAdded() || response.body() == null) return;
                        sessionMembers = response.body().getMembers() != null
                                ? response.body().getMembers() : new ArrayList<>();

                        if (sessionMembers.isEmpty()) {
                            Toast.makeText(requireContext(),
                                    "No members in this session's lineup",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<String> names = new ArrayList<>();
                        for (LineupMemberDto m : sessionMembers)
                            names.add(m.getDisplayName() + " (" + m.getInstrument() + ")");

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_item, names);
                        adapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        spinnerMember.setAdapter(adapter);
                        selectedUserId = sessionMembers.get(0).getUserId();

                        spinnerMember.setOnItemSelectedListener(
                                new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> p,
                                                               View v, int pos, long id) {
                                        selectedUserId = sessionMembers.get(pos).getUserId();
                                    }
                                    @Override
                                    public void onNothingSelected(AdapterView<?> p) { }
                                });
                    }

                    @Override
                    public void onFailure(Call<SessionResponse> call, Throwable t) { }
                });
    }

    private void save(boolean isEdit) {
        String title = etTitle.getText() != null
                ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            tilTitle.setError("Enter a task title");
            return;
        }
        tilTitle.setError(null);

        if (!isEdit && selectedUserId == -1) {
            Toast.makeText(requireContext(),
                    "Select a member", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);

        if (isEdit) {
            RetrofitClient.getInstance().getApi()
                    .updateTask(taskId, body)
                    .enqueue(new Callback<TaskResponse>() {
                        @Override
                        public void onResponse(Call<TaskResponse> call,
                                               Response<TaskResponse> response) {
                            btnSave.setEnabled(true);
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                if (onSaved != null) onSaved.run();
                                dismiss();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Failed to update", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<TaskResponse> call, Throwable t) {
                            btnSave.setEnabled(true);
                        }
                    });
        } else {
            body.put("sessionId", sessionId);
            body.put("assignedTo", selectedUserId);
            RetrofitClient.getInstance().getApi()
                    .createTask(body)
                    .enqueue(new Callback<TaskResponse>() {
                        @Override
                        public void onResponse(Call<TaskResponse> call,
                                               Response<TaskResponse> response) {
                            btnSave.setEnabled(true);
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                if (onSaved != null) onSaved.run();
                                dismiss();
                            } else {
                                Toast.makeText(requireContext(),
                                        "Failed to create", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<TaskResponse> call, Throwable t) {
                            btnSave.setEnabled(true);
                        }
                    });
        }
    }
}