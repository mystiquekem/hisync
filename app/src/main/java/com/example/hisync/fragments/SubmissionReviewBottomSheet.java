package com.example.hisync.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmissionReviewBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TASK_ID       = "task_id";
    private static final String ARG_TASK_TITLE    = "task_title";
    private static final String ARG_ASSIGNEE      = "assignee";
    private static final String ARG_SESSION_SONG  = "session_song";
    private static final String ARG_RECORDING_URL = "recording_url";

    private long taskId;
    private MediaPlayer mediaPlayer;
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;
    private Runnable onReviewed;

    public static SubmissionReviewBottomSheet newInstance(TaskResponse task) {
        SubmissionReviewBottomSheet sheet = new SubmissionReviewBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID,         task.getId());
        args.putString(ARG_TASK_TITLE,    task.getTitle());
        args.putString(ARG_ASSIGNEE,      task.getAssignedToName());
        args.putString(ARG_SESSION_SONG,  task.getSessionSong() != null
                ? task.getSessionSong() : "");
        args.putString(ARG_RECORDING_URL, task.getRecordingUrl());
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnReviewedListener(Runnable listener) {
        this.onReviewed = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_submission_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args       = requireArguments();
        taskId            = args.getLong(ARG_TASK_ID);
        String taskTitle  = args.getString(ARG_TASK_TITLE, "Task");
        String assignee   = args.getString(ARG_ASSIGNEE, "");
        String sessionSong= args.getString(ARG_SESSION_SONG, "");
        String recordingUrl = args.getString(ARG_RECORDING_URL);

        ((TextView) view.findViewById(R.id.tvReviewTaskTitle)).setText(taskTitle);
        ((TextView) view.findViewById(R.id.tvReviewAssignee)).setText(assignee);
        ((TextView) view.findViewById(R.id.tvReviewSong)).setText(sessionSong);

        SeekBar seekBar       = view.findViewById(R.id.seekBarReview);
        ProgressBar progressBar = view.findViewById(R.id.progressBarReview);
        MaterialButton btnPlay  = view.findViewById(R.id.btnReviewPlay);
        MaterialButton btnApprove   = view.findViewById(R.id.btnApprove);
        MaterialButton btnRerecord  = view.findViewById(R.id.btnRerecord);

        if (recordingUrl != null && !recordingUrl.isEmpty()) {
            setupPlayer(recordingUrl, btnPlay, seekBar, progressBar);
        } else {
            view.findViewById(R.id.layoutReviewPlayer).setVisibility(View.GONE);
            Toast.makeText(requireContext(),
                    "No recording found", Toast.LENGTH_SHORT).show();
        }

        btnApprove.setOnClickListener(v -> updateStatus("approved", btnApprove, btnRerecord));
        btnRerecord.setOnClickListener(v -> updateStatus("rerecord", btnApprove, btnRerecord));
    }

    private void setupPlayer(String url, MaterialButton btnPlay,
                             SeekBar seekBar, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        btnPlay.setEnabled(false);
        btnPlay.setText("Loading...");

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                seekBar.setMax(mp.getDuration());
                btnPlay.setEnabled(true);
                btnPlay.setText("▶ Play");
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlay.setText("▶ Play");
                seekBar.setProgress(0);
                seekHandler.removeCallbacksAndMessages(null);
            });
        } catch (IOException e) {
            progressBar.setVisibility(View.GONE);
            btnPlay.setText("Cannot load");
            return;
        }

        btnPlay.setOnClickListener(v -> {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                btnPlay.setText("▶ Play");
                seekHandler.removeCallbacksAndMessages(null);
            } else {
                mediaPlayer.start();
                isPlaying = true;
                btnPlay.setText("⏸ Pause");
                updateSeekBar(seekBar, btnPlay);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void updateSeekBar(SeekBar seekBar, MaterialButton btnPlay) {
        if (mediaPlayer != null && isPlaying) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            seekHandler.postDelayed(() -> updateSeekBar(seekBar, btnPlay), 500);
        }
    }

    private void updateStatus(String status,
                              MaterialButton btnApprove, MaterialButton btnRerecord) {
        btnApprove.setEnabled(false);
        btnRerecord.setEnabled(false);

        Map<String, String> body = new HashMap<>();
        body.put("status", status);

        RetrofitClient.getInstance().getApi()
                .updateTaskStatus(taskId, body)
                .enqueue(new Callback<TaskResponse>() {
                    @Override
                    public void onResponse(Call<TaskResponse> call,
                                           Response<TaskResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            String msg = "approved".equals(status)
                                    ? "Recording approved ✓"
                                    : "Re-record requested";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                            if (onReviewed != null) onReviewed.run();
                            dismiss();
                        } else {
                            btnApprove.setEnabled(true);
                            btnRerecord.setEnabled(true);
                            Toast.makeText(requireContext(),
                                    "Failed. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<TaskResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        btnApprove.setEnabled(true);
                        btnRerecord.setEnabled(true);
                        Toast.makeText(requireContext(),
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        seekHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}