package com.example.hisync.fragments;

import android.content.SharedPreferences;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.SongResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SongsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutSongs;
    private TextView tvEmpty;
    private FloatingActionButton fabAddSong;

    private long bandId, userId;
    private String userRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        bandId   = prefs.getLong("bandId", -1);
        userId   = prefs.getLong("userId", -1);
        userRole = prefs.getString("role", "member");

        swipeRefresh = view.findViewById(R.id.swipeRefreshSongs);
        layoutSongs  = view.findViewById(R.id.layoutSongs);
        tvEmpty      = view.findViewById(R.id.tvEmptySongs);
        fabAddSong   = view.findViewById(R.id.fabAddSong);

        // Chỉ leader mới thấy nút add
        fabAddSong.setVisibility(
                "leader".equals(userRole) || "admin".equals(userRole)
                        ? View.VISIBLE : View.GONE
        );

        fabAddSong.setOnClickListener(v -> openYouTubeSearch());

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setOnRefreshListener(this::loadSongs);

        loadSongs();
    }

    private void loadSongs() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getSongs(bandId)
                .enqueue(new Callback<List<SongResponse>>() {
                    @Override
                    public void onResponse(Call<List<SongResponse>> call,
                                           Response<List<SongResponse>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        layoutSongs.removeAllViews();
                        if (response.body() == null || response.body().isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvEmpty.setVisibility(View.GONE);
                        for (SongResponse song : response.body()) {
                            addSongRow(song);
                        }
                    }
                    @Override
                    public void onFailure(Call<List<SongResponse>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void addSongRow(SongResponse song) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_song_row, layoutSongs, false);

        Glide.with(this)
                .load(song.getThumbnailUrl())
                .placeholder(R.drawable.ic_nav_home)
                .into((ImageView) row.findViewById(R.id.ivSongThumb));

        ((TextView) row.findViewById(R.id.tvSongTitle)).setText(song.getTitle());
        ((TextView) row.findViewById(R.id.tvSongAdded)).setText("Added by " + song.getAddedByName());

        // Leader có thể xóa
        View btnDelete = row.findViewById(R.id.btnDeleteSong);
        if ("leader".equals(userRole) || "admin".equals(userRole)) {
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDelete(song));
        } else {
            btnDelete.setVisibility(View.GONE);
        }

        layoutSongs.addView(row);
    }

    private void confirmDelete(SongResponse song) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove song")
                .setMessage("Remove \"" + song.getTitle() + "\" from setlist?")
                .setPositiveButton("Remove", (d, w) -> deleteSong(song.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSong(long songId) {
        RetrofitClient.getInstance().getApi()
                .deleteSong(songId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) loadSongs();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openYouTubeSearch() {
        YouTubeSearchBottomSheet sheet = YouTubeSearchBottomSheet.newInstance(bandId, userId);
        sheet.setOnSongAddedListener(this::loadSongs);
        sheet.show(getParentFragmentManager(), "yt_search");
    }
}