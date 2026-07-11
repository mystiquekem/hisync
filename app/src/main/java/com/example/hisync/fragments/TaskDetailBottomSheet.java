package com.example.hisync.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

public class TaskDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_TASK_ID       = "task_id";
    private static final String ARG_TASK_TITLE    = "task_title";
    private static final String ARG_STATUS        = "status";
    private static final String ARG_RECORDING_URL = "recording_url";
    private static final String ARG_SESSION_SONG  = "session_song";
    private static final String ARG_SESSION_DATE  = "session_date";
    private static final String ARG_ASSIGNEE      = "assignee";

    private long taskId;
    private String status;

    // Playback
    private MediaPlayer mediaPlayer;
    private final Handler seekHandler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = false;

    // Recording
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private File recordingFile;

    private ActivityResultLauncher<String> permissionLauncher;

    // Upload UI
    private MaterialButton btnRecord, btnPickFile, btnSubmit;
    private TextView tvUploadStatus;

    // File picker
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri pickedFileUri;

    public static TaskDetailBottomSheet newInstance(TaskResponse task,
                                                    String sessionSong, String sessionDate) {
        TaskDetailBottomSheet sheet = new TaskDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_TASK_ID,         task.getId());
        args.putString(ARG_TASK_TITLE,    task.getTitle());
        args.putString(ARG_STATUS,        task.getStatus());
        args.putString(ARG_RECORDING_URL, task.getRecordingUrl());
        args.putString(ARG_SESSION_SONG,  sessionSong);
        args.putString(ARG_SESSION_DATE,  sessionDate);
        args.putString(ARG_ASSIGNEE,      task.getAssignedToName());
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK
                            && result.getData() != null) {
                        pickedFileUri = result.getData().getData();
                        if (pickedFileUri != null) {
                            tvUploadStatus.setText("File selected ✓");
                            btnSubmit.setVisibility(View.VISIBLE);
                            recordingFile = null;
                        }
                    }
                });
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startRecording(); // retry sau khi được cấp quyền
                    } else {
                        Toast.makeText(requireContext(),
                                "Microphone permission required",
                                Toast.LENGTH_SHORT).show();
                    }
                });
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

        Bundle args        = requireArguments();
        taskId             = args.getLong(ARG_TASK_ID);
        status             = args.getString(ARG_STATUS, "pending");
        String taskTitle   = args.getString(ARG_TASK_TITLE, "Task");
        String recordingUrl= args.getString(ARG_RECORDING_URL);
        String sessionSong = args.getString(ARG_SESSION_SONG, "");
        String sessionDate = args.getString(ARG_SESSION_DATE, "");
        String assignee    = args.getString(ARG_ASSIGNEE, "");

        ((TextView) view.findViewById(R.id.tvDetailTaskTitle)).setText(taskTitle);
        ((TextView) view.findViewById(R.id.tvDetailSessionSong)).setText(sessionSong);
        ((TextView) view.findViewById(R.id.tvDetailSessionDate)).setText(sessionDate);
        ((TextView) view.findViewById(R.id.tvDetailAssignee)).setText(assignee);

        TextView tvStatus = view.findViewById(R.id.tvDetailStatus);
        bindStatus(tvStatus, status);

        View playerSection = view.findViewById(R.id.layoutPlayer);
        MaterialButton btnPlayPause = view.findViewById(R.id.btnPlayPause);
        SeekBar seekBar = view.findViewById(R.id.seekBarAudio);
        View uploadSection = view.findViewById(R.id.layoutUpload);
        btnRecord      = view.findViewById(R.id.btnRecord);
        btnPickFile    = view.findViewById(R.id.btnPickFile);
        btnSubmit      = view.findViewById(R.id.btnSubmitRecording);
        tvUploadStatus = view.findViewById(R.id.tvUploadStatus);

        if ((status.equals("submitted") || status.equals("approved"))
                && recordingUrl != null && !recordingUrl.isEmpty()) {
            playerSection.setVisibility(View.VISIBLE);
            uploadSection.setVisibility(View.GONE);
            setupPlayer(recordingUrl, btnPlayPause, seekBar);
        } else if (status.equals("pending") || status.equals("rerecord")) {
            playerSection.setVisibility(View.GONE);
            uploadSection.setVisibility(View.VISIBLE);
            setupUpload();
        } else {
            playerSection.setVisibility(View.GONE);
            uploadSection.setVisibility(View.GONE);
        }
    }

    private void setupUpload() {
        btnSubmit.setVisibility(View.GONE);

        btnPickFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(Intent.createChooser(intent, "Select audio file"));
        });

        btnRecord.setOnClickListener(v -> {
            if (isRecording) stopRecording();
            else startRecording();
        });

        btnSubmit.setOnClickListener(v -> uploadAndSubmit());
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        try {
            recordingFile = new File(requireContext().getCacheDir(),
                    "recording_" + taskId + ".m4a");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(recordingFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            btnRecord.setText("⏹ Stop Recording");
            btnRecord.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFEF4B3C));
            tvUploadStatus.setText("Recording...");
            pickedFileUri = null;
        } catch (IOException e) {
            Toast.makeText(requireContext(),
                    "Cannot start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;
        btnRecord.setText("🎤 Record");
        btnRecord.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF3A9BAD));
        tvUploadStatus.setText("Recording saved ✓");
        btnSubmit.setVisibility(View.VISIBLE);
    }

    private void uploadAndSubmit() {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Uploading...");
        tvUploadStatus.setText("Uploading to cloud...");

        new Thread(() -> {
            try {
                String url = uploadToCloudinary();
                if (url == null) throw new Exception("Upload returned null");
                requireActivity().runOnUiThread(() -> submitRecordingUrl(url));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Submit");
                    tvUploadStatus.setText("Upload failed. Try again.");
                });
            }
        }).start();
    }

    private String uploadToCloudinary() throws Exception {
        String cloudName    = "dmxxaqbn5";
        String uploadPreset = "hisync_recordings";
        String uploadUrl    = "https://api.cloudinary.com/v1_1/" + cloudName + "/auto/upload";

        RequestBody fileBody;
        String fileName;

        if (pickedFileUri != null) {
            byte[] bytes = requireContext().getContentResolver()
                    .openInputStream(pickedFileUri).readAllBytes();
            fileBody = RequestBody.create(bytes, MediaType.parse("audio/*"));
            fileName = "recording_" + taskId + ".audio";
        } else if (recordingFile != null && recordingFile.exists()) {
            fileBody = RequestBody.create(recordingFile, MediaType.parse("audio/mp4"));
            fileName = recordingFile.getName();
        } else {
            throw new Exception("No file selected");
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", fileName, fileBody)
                .addFormDataPart("upload_preset", uploadPreset)
                .build();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .build();

        Response response = client.newCall(request).execute();
        String body = response.body().string();
        JSONObject json = new JSONObject(body);
        return json.getString("secure_url");
    }

    private void submitRecordingUrl(String cloudinaryUrl) {
        Map<String, String> body = new HashMap<>();
        body.put("recordingUrl", cloudinaryUrl);
        RetrofitClient.getInstance().getApi()
                .submitRecording(taskId, body)
                .enqueue(new Callback<TaskResponse>() {
                    @Override
                    public void onResponse(Call<TaskResponse> call,
                                           retrofit2.Response<TaskResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            tvUploadStatus.setText("Submitted! Awaiting approval ✓");
                            btnSubmit.setVisibility(View.GONE);
                            btnRecord.setVisibility(View.GONE);
                            btnPickFile.setVisibility(View.GONE);
                            Toast.makeText(requireContext(),
                                    "Recording submitted!", Toast.LENGTH_SHORT).show();
                        } else {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Submit");
                            tvUploadStatus.setText("Submission failed. Try again.");
                        }
                    }
                    @Override
                    public void onFailure(Call<TaskResponse> call, Throwable t) {
                        if (!isAdded()) return;
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Submit");
                        tvUploadStatus.setText("Network error. Try again.");
                    }
                });
    }

    private void setupPlayer(String url, MaterialButton btnPlayPause, SeekBar seekBar) {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                seekBar.setMax(mp.getDuration());
                btnPlayPause.setEnabled(true);
                btnPlayPause.setText("▶ Play");
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
                updateSeekBar(seekBar, btnPlayPause);
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

    private void updateSeekBar(SeekBar seekBar, MaterialButton btn) {
        if (mediaPlayer != null && isPlaying) {
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            seekHandler.postDelayed(() -> updateSeekBar(seekBar, btn), 500);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        seekHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (isRecording && mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }
}