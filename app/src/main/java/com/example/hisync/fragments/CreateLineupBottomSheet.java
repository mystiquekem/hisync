package com.example.hisync.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hisync.BuildConfig;
import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;
import com.example.hisync.dto.LineupMemberDto;
import com.example.hisync.dto.LineupResponse;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

public class CreateLineupBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BAND_ID  = "band_id";
    private static final String ARG_USER_ID  = "user_id";
    private static final String ARG_LINEUP   = "lineup_json";

    private long bandId, userId;
    private LineupResponse editingLineup; // null = create mode
    private Runnable onSaved;

    // Selected song
    private String selectedYoutubeId;
    private String selectedTitle;
    private String selectedThumb;

    // Member selections: userId → chosen instrument
    private final Map<Long, String> memberInstruments = new HashMap<>();
    // All band members loaded from API
    private List<BandResponse.MemberDto> bandMembers = new ArrayList<>();

    private TextInputEditText etSearch;
    private LinearLayout layoutResults, layoutMembers;
    private MaterialButton btnSave;
    private TextView tvSelectedSong;
    private ImageView ivSelectedThumb;

    public static CreateLineupBottomSheet newInstance(
            long bandId, long userId, @Nullable LineupResponse editing) {
        CreateLineupBottomSheet sheet = new CreateLineupBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_BAND_ID, bandId);
        args.putLong(ARG_USER_ID, userId);
        if (editing != null) {
            // Pass editing lineup fields as individual args
            args.putLong("edit_id", editing.getId());
            args.putString("edit_title", editing.getSongTitle());
            args.putString("edit_yt", editing.getYoutubeId());
            args.putString("edit_thumb", editing.getThumbnailUrl());
        }
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSavedListener(Runnable listener) { this.onSaved = listener; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_create_lineup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        bandId = args.getLong(ARG_BAND_ID);
        userId = args.getLong(ARG_USER_ID);

        boolean isEdit = args.containsKey("edit_id");
        if (isEdit) {
            selectedYoutubeId = args.getString("edit_yt");
            selectedTitle     = args.getString("edit_title");
            selectedThumb     = args.getString("edit_thumb");
        }

        etSearch        = view.findViewById(R.id.etLineupSearch);
        layoutResults   = view.findViewById(R.id.layoutLineupResults);
        layoutMembers   = view.findViewById(R.id.layoutLineupMembers);
        btnSave         = view.findViewById(R.id.btnSaveLineup);
        tvSelectedSong  = view.findViewById(R.id.tvSelectedSong);
        ivSelectedThumb = view.findViewById(R.id.ivSelectedThumb);

        ((TextView) view.findViewById(R.id.tvLineupSheetTitle))
                .setText(isEdit ? "Edit Lineup" : "New Lineup");

        if (isEdit && selectedTitle != null) {
            tvSelectedSong.setText(selectedTitle);
            tvSelectedSong.setVisibility(View.VISIBLE);
            Glide.with(this).load(selectedThumb).into(ivSelectedThumb);
            ivSelectedThumb.setVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.btnLineupSearch).setOnClickListener(v -> {
            String q = etSearch.getText() != null
                    ? etSearch.getText().toString().trim() : "";
            if (!q.isEmpty()) searchYouTube(q);
        });

        btnSave.setOnClickListener(v -> save(isEdit ? args.getLong("edit_id") : -1));

        loadBandMembers(isEdit ? args : null);
    }

    private void loadBandMembers(@Nullable Bundle editArgs) {
        RetrofitClient.getInstance().getApi()
                .getBand(bandId)
                .enqueue(new Callback<BandResponse>() {
                    @Override
                    public void onResponse(Call<BandResponse> call,
                                           retrofit2.Response<BandResponse> response) {
                        if (!isAdded() || response.body() == null) return;
                        bandMembers = response.body().getMembers();
                        buildMemberRows(editArgs);
                    }

                    @Override
                    public void onFailure(Call<BandResponse> call, Throwable t) { }
                });
    }

    private void buildMemberRows(@Nullable Bundle editArgs) {
        if (!isAdded()) return;
        layoutMembers.removeAllViews();

        // Pre-fill instruments from editing lineup if available
        Map<Long, String> existingInstruments = new HashMap<>();
        if (editArgs != null && editArgs.containsKey("edit_id")) {
            // We'll load the lineup detail to get existing member instruments
            long editId = editArgs.getLong("edit_id");
            RetrofitClient.getInstance().getApi()
                    .getLineup(editId)
                    .enqueue(new Callback<com.example.hisync.dto.LineupResponse>() {
                        @Override
                        public void onResponse(
                                Call<com.example.hisync.dto.LineupResponse> call,
                                retrofit2.Response<com.example.hisync.dto.LineupResponse> response) {
                            if (!isAdded() || response.body() == null) return;
                            Map<Long, String> prefill = new HashMap<>();
                            if (response.body().getMembers() != null) {
                                for (LineupMemberDto m : response.body().getMembers()) {
                                    prefill.put(m.getUserId(), m.getInstrument());
                                }
                            }
                            renderMemberRows(prefill);
                        }

                        @Override
                        public void onFailure(
                                Call<com.example.hisync.dto.LineupResponse> call, Throwable t) {
                            renderMemberRows(new HashMap<>());
                        }
                    });
        } else {
            renderMemberRows(existingInstruments);
        }
    }

    private void renderMemberRows(Map<Long, String> prefill) {
        if (!isAdded()) return;
        layoutMembers.removeAllViews();

        for (BandResponse.MemberDto member : bandMembers) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_lineup_member_pick, layoutMembers, false);

            String name = member.getDisplayName() != null
                    ? member.getDisplayName() : member.getEmail();
            ((TextView) row.findViewById(R.id.tvPickMemberName)).setText(name);

            // Declare BOTH of these here, before the async call
            CheckBox checkMember = row.findViewById(R.id.checkMember);
            Spinner spinner = row.findViewById(R.id.spinnerInstrument);

            layoutMembers.addView(row);

            // Fetch this member's instruments to populate the spinner
            RetrofitClient.getInstance().getApi()
                    .getInstruments(member.getUserId())
                    .enqueue(new Callback<List<String>>() {
                        @Override
                        public void onResponse(Call<List<String>> call,
                                               retrofit2.Response<List<String>> response) {
                            if (!isAdded()) return;
                            List<String> instruments = response.body() != null
                                    ? response.body() : new ArrayList<>();
                            if (instruments.isEmpty()) instruments.add("other");

                            Spinner spinner = row.findViewById(R.id.spinnerInstrument);
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    requireContext(),
                                    android.R.layout.simple_spinner_item,
                                    instruments);
                            adapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item);
                            spinner.setAdapter(adapter);

                            // Pre-select if editing
                            String existing = prefill.get(member.getUserId());
                            if (existing != null) {
                                int idx = instruments.indexOf(existing);
                                if (idx >= 0) spinner.setSelection(idx);
                            }

                            // Only default-add if checkbox is checked (editing mode pre-check handles this)
                            if (checkMember.isChecked()) {
                                memberInstruments.put(member.getUserId(), instruments.get(0));
                            }

                            spinner.setOnItemSelectedListener(
                                    new AdapterView.OnItemSelectedListener() {
                                        @Override
                                        public void onItemSelected(AdapterView<?> parent,
                                                                   View view, int pos, long id) {
                                            memberInstruments.put(member.getUserId(),
                                                    instruments.get(pos));
                                        }
                                        @Override
                                        public void onNothingSelected(AdapterView<?> parent) { }
                                    });

                            // Pre-check if editing and member was already in lineup
                            if (prefill.containsKey(member.getUserId())) {
                                checkMember.setChecked(true);
                                spinner.setVisibility(View.VISIBLE);
                            } else {
                                memberInstruments.remove(member.getUserId()); // not selected by default
                            }

                            checkMember.setOnCheckedChangeListener((btn, isChecked) -> {
                                if (isChecked) {
                                    spinner.setVisibility(View.VISIBLE);
                                    // Add with current spinner selection
                                    int pos = spinner.getSelectedItemPosition();
                                    if (pos >= 0 && pos < instruments.size()) {
                                        memberInstruments.put(member.getUserId(), instruments.get(pos));
                                    }
                                } else {
                                    spinner.setVisibility(View.GONE);
                                    memberInstruments.remove(member.getUserId());
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<List<String>> call, Throwable t) { }
                    });

        }
    }

    private void searchYouTube(String query) {
        layoutResults.removeAllViews();
        new Thread(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet&type=video&maxResults=8"
                        + "&q=" + java.net.URLEncoder.encode(query, "UTF-8")
                        + "&key=" + BuildConfig.YOUTUBE_API_KEY;

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String body = response.body().string();
                JSONObject json = new JSONObject(body);
                JSONArray items = json.getJSONArray("items");

                requireActivity().runOnUiThread(() -> {
                    for (int i = 0; i < items.length(); i++) {
                        try {
                            JSONObject item    = items.getJSONObject(i);
                            String videoId     = item.getJSONObject("id").getString("videoId");
                            JSONObject snippet = item.getJSONObject("snippet");
                            String title       = snippet.getString("title");
                            String thumb       = snippet.getJSONObject("thumbnails")
                                    .getJSONObject("medium").getString("url");
                            addSearchResult(videoId, title, thumb);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Search failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void addSearchResult(String videoId, String title, String thumb) {
        if (!isAdded()) return;
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_youtube_result, layoutResults, false);
        Glide.with(this).load(thumb).into((ImageView) row.findViewById(R.id.ivYtThumb));
        ((TextView) row.findViewById(R.id.tvYtTitle)).setText(title);
        row.setOnClickListener(v -> {
            selectedYoutubeId = videoId;
            selectedTitle     = title;
            selectedThumb     = thumb;
            tvSelectedSong.setText(title);
            tvSelectedSong.setVisibility(View.VISIBLE);
            Glide.with(this).load(thumb).into(ivSelectedThumb);
            ivSelectedThumb.setVisibility(View.VISIBLE);
            layoutResults.removeAllViews(); // collapse results
        });
        layoutResults.addView(row);
    }

    private void save(long editId) {
        if (selectedYoutubeId == null || selectedTitle == null) {
            Toast.makeText(requireContext(),
                    "Pick a song first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (memberInstruments.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Select at least one member", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);

        // Build members list
        List<Map<String, Object>> members = new ArrayList<>();
        for (Map.Entry<Long, String> entry : memberInstruments.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", entry.getKey());
            m.put("instrument", entry.getValue());
            members.add(m);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("bandId", bandId);
        body.put("songTitle", selectedTitle);
        body.put("youtubeId", selectedYoutubeId);
        body.put("thumbnailUrl", selectedThumb);
        body.put("createdBy", userId);
        body.put("members", members);

        Call<com.example.hisync.dto.LineupResponse> call = editId == -1
                ? RetrofitClient.getInstance().getApi().createLineup(body)
                : RetrofitClient.getInstance().getApi().updateLineup(editId, body);

        call.enqueue(new Callback<com.example.hisync.dto.LineupResponse>() {
            @Override
            public void onResponse(Call<com.example.hisync.dto.LineupResponse> c,
                                   retrofit2.Response<com.example.hisync.dto.LineupResponse> r) {
                btnSave.setEnabled(true);
                if (!isAdded()) return;
                if (r.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            editId == -1 ? "Lineup created!" : "Lineup updated!",
                            Toast.LENGTH_SHORT).show();
                    if (onSaved != null) onSaved.run();
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), "Failed to save", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.hisync.dto.LineupResponse> c, Throwable t) {
                btnSave.setEnabled(true);
                if (isAdded())
                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}