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
import com.example.hisync.dto.LineupResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LineupsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutLineups;
    private TextView tvEmpty;
    private FloatingActionButton fabAddLineup;

    private long bandId, userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lineups, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        bandId = prefs.getLong("bandId", -1);
        userId = prefs.getLong("userId", -1);

        swipeRefresh  = view.findViewById(R.id.swipeRefreshLineups);
        layoutLineups = view.findViewById(R.id.layoutLineups);
        tvEmpty       = view.findViewById(R.id.tvEmptyLineups);
        fabAddLineup  = view.findViewById(R.id.fabAddLineup);

        fabAddLineup.setOnClickListener(v -> openCreateLineup());

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadLineups);

        loadLineups();
    }

    public void loadLineups() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getLineups(bandId)
                .enqueue(new Callback<List<LineupResponse>>() {
                    @Override
                    public void onResponse(Call<List<LineupResponse>> call,
                                           Response<List<LineupResponse>> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        layoutLineups.removeAllViews();
                        if (response.body() == null || response.body().isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            return;
                        }
                        tvEmpty.setVisibility(View.GONE);
                        for (LineupResponse lineup : response.body()) {
                            addLineupRow(lineup);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<LineupResponse>> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void addLineupRow(LineupResponse lineup) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_lineup_row, layoutLineups, false);

        Glide.with(this)
                .load(lineup.getThumbnailUrl())
                .placeholder(R.drawable.ic_nav_home)
                .into((ImageView) row.findViewById(R.id.ivLineupThumb));

        ((TextView) row.findViewById(R.id.tvLineupTitle)).setText(lineup.getSongTitle());

        int memberCount = lineup.getMembers() != null ? lineup.getMembers().size() : 0;
        ((TextView) row.findViewById(R.id.tvLineupMembers))
                .setText(memberCount + " member" + (memberCount != 1 ? "s" : ""));

        row.findViewById(R.id.btnEditLineup).setOnClickListener(v ->
                openEditLineup(lineup));

        row.findViewById(R.id.btnDeleteLineup).setOnClickListener(v ->
                confirmDeleteLineup(lineup));

        layoutLineups.addView(row);
    }

    private void openCreateLineup() {
        CreateLineupBottomSheet sheet = CreateLineupBottomSheet.newInstance(bandId, userId, null);
        sheet.setOnSavedListener(this::loadLineups);
        sheet.show(getParentFragmentManager(), "create_lineup");
    }

    private void openEditLineup(LineupResponse lineup) {
        CreateLineupBottomSheet sheet =
                CreateLineupBottomSheet.newInstance(bandId, userId, lineup);
        sheet.setOnSavedListener(this::loadLineups);
        sheet.show(getParentFragmentManager(), "edit_lineup");
    }

    private void confirmDeleteLineup(LineupResponse lineup) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete lineup")
                .setMessage("Delete \"" + lineup.getSongTitle() + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> deleteLineup(lineup.getId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLineup(long lineupId) {
        RetrofitClient.getInstance().getApi()
                .deleteLineup(lineupId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (isAdded()) loadLineups();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (isAdded())
                            Toast.makeText(requireContext(),
                                    "Failed to delete", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}