package com.example.hisync.fragments;

import com.example.hisync.BuildConfig;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import retrofit2.Call;
import retrofit2.Callback;

public class YouTubeSearchBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_BAND_ID = "band_id";
    private static final String ARG_USER_ID = "user_id";
    private static final String YT_API_KEY = BuildConfig.YOUTUBE_API_KEY;

    private long bandId, userId;
    private Runnable onSongAdded;

    private TextInputEditText etSearch;
    private LinearLayout layoutResults;

    public static YouTubeSearchBottomSheet newInstance(long bandId, long userId) {
        YouTubeSearchBottomSheet sheet = new YouTubeSearchBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_BAND_ID, bandId);
        args.putLong(ARG_USER_ID, userId);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSongAddedListener(Runnable listener) {
        this.onSongAdded = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_youtube_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bandId = requireArguments().getLong(ARG_BAND_ID);
        userId = requireArguments().getLong(ARG_USER_ID);

        etSearch      = view.findViewById(R.id.etYoutubeSearch);
        layoutResults = view.findViewById(R.id.layoutYoutubeResults);

        view.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String query = etSearch.getText() != null
                    ? etSearch.getText().toString().trim() : "";
            if (!query.isEmpty()) searchYouTube(query);
        });
    }

    private void searchYouTube(String query) {
        layoutResults.removeAllViews();

        // Chạy network call trên background thread
        new Thread(() -> {
            try {
                String url = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet&type=video&maxResults=10"
                        + "&q=" + java.net.URLEncoder.encode(query, "UTF-8")
                        + "&key=" + YT_API_KEY;

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
                                    .getJSONObject("medium")
                                    .getString("url");
                            addResultRow(videoId, title, thumb);
                        } catch (Exception ignored) {}
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void addResultRow(String videoId, String title, String thumbUrl) {
        if (!isAdded()) return;
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_youtube_result, layoutResults, false);

        Glide.with(this).load(thumbUrl)
                .into((ImageView) row.findViewById(R.id.ivYtThumb));
        ((TextView) row.findViewById(R.id.tvYtTitle)).setText(title);

        row.setOnClickListener(v -> addSongToBand(videoId, title, thumbUrl));
        layoutResults.addView(row);
    }

    private void addSongToBand(String videoId, String title, String thumbUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("bandId",       bandId);
        body.put("youtubeId",    videoId);
        body.put("title",        title);
        body.put("thumbnailUrl", thumbUrl);
        body.put("addedBy",      userId);

        RetrofitClient.getInstance().getApi()
                .addSong(body)
                .enqueue(new Callback<com.example.hisync.dto.SongResponse>() {
                    @Override
                    public void onResponse(Call<com.example.hisync.dto.SongResponse> call,
                                           retrofit2.Response<com.example.hisync.dto.SongResponse> response) {
                        if (!isAdded()) return;
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Added to setlist!", Toast.LENGTH_SHORT).show();
                            if (onSongAdded != null) onSongAdded.run();
                            dismiss();
                        } else {
                            Toast.makeText(requireContext(), "Already in setlist", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<com.example.hisync.dto.SongResponse> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}