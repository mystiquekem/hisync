package com.example.hisync.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.hisync.R;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;

public class TaskDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TASK_TITLE    = "task_title";
    private static final String ARG_STATUS        = "status";
    private static final String ARG_RECORDING_URL = "recording_url";
    private static final String ARG_SESSION_SONG  = "session_song";
    private static final String ARG_SESSION_DATE  = "session_date";
    private static final String ARG_ASSIGNEE      = "assignee";

    private MediaPlayer mediaPlayer;
    private Handler seekHandler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;

    public static TaskDetailBottomSheet newInstance(TaskResponse task,
                                                    String sessionSong, String sessionDate) {
        TaskDetailBottomSheet sheet = new TaskDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TASK_TITLE,    task.getTitle());
        args.putString(ARG_STATUS,        task.getStatus());
        args.putString(ARG_RECORDING_URL, task.getRecordingUrl());
        args.putString(ARG_SESSION_SONG,  sessionSong);
        args.putString(ARG_SESSION_DATE,  sessionDate);
        args.putString(ARG_ASSIGNEE,      task.getAssignedToName());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_task_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args       = requireArguments();
        String taskTitle  = args.getString(ARG_TASK_TITLE, "Task");
        String status     = args.getString(ARG_STATUS, "pending");
        String recordingUrl = args.getString(ARG_RECORDING_URL);
        String sessionSong  = args.getString(ARG_SESSION_SONG, "");
        String sessionDate  = args.getString(ARG_SESSION_DATE, "");
        String assignee     = args.getString(ARG_ASSIGNEE, "");

        ((TextView) view.findViewById(R.id.tvDetailTaskTitle)).setText(taskTitle);
        ((TextView) view.findViewById(R.id.tvDetailSessionSong)).setText(sessionSong);
        ((TextView) view.findViewById(R.id.tvDetailSessionDate)).setText(sessionDate);
        ((TextView) view.findViewById(R.id.tvDetailAssignee)).setText(assignee);

        TextView tvStatus = view.findViewById(R.id.tvDetailStatus);
        bindStatus(tvStatus, status);

        // Player section — only visible if recording exists
        View playerSection = view.findViewById(R.id.layoutPlayer);
        MaterialButton btnPlayPause = view.findViewById(R.id.btnPlayPause);
        SeekBar seekBar = view.findViewById(R.id.seekBarAudio);

        if (recordingUrl != null && !recordingUrl.isEmpty()
                && (status.equals("submitted") || status.equals("approved"))) {
            playerSection.setVisibility(View.VISIBLE);
            setupPlayer(recordingUrl, btnPlayPause, seekBar);
        } else {
            playerSection.setVisibility(View.GONE);
        }
    }

    private void bindStatus(TextView tv, String status) {
        switch (status) {
            case "submitted":
                tv.setText("Awaiting Approval");
                tv.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark));
                break;
            case "approved":
                tv.setText("Approved ✓");
                tv.setTextColor(requireContext().getColor(R.color.green_accent));
                break;
            case "rerecord":
                tv.setText("Re-record Requested");
                tv.setTextColor(requireContext().getColor(android.R.color.holo_red_light));
                break;
            default:
                tv.setText("Pending");
                tv.setTextColor(requireContext().getColor(R.color.purple_primary));
                break;
        }
    }

    private void setupPlayer(String url, MaterialButton btnPlayPause, SeekBar seekBar) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                btnPlayPause.setEnabled(true);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                btnPlayPause.setText("▶ Play");
                seekBar.setProgress(0);
                seekHandler.removeCallbacksAndMessages(null);
            });
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Cannot load recording", Toast.LENGTH_SHORT).show();
            return;
        }

        btnPlayPause.setEnabled(false);
        btnPlayPause.setText("Loading...");

        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                mediaPlayer.pause();
                isPlaying = false;
                btnPlayPause.setText("▶ Play");
                seekHandler.removeCallbacksAndMessages(null);
            } else {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayPause.setText("⏸ Pause");
                updateSeekBar(seekBar);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) mediaPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
    }

    private void updateSeekBar(SeekBar seekBar) {
        if (mediaPlayer != null && isPlaying) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            seekHandler.postDelayed(() -> updateSeekBar(seekBar), 500);
        }
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