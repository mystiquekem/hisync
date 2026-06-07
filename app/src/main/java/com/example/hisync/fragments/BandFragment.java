package com.example.hisync.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BandFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvBandName, tvInviteCode, tvMemberCount;
    private LinearLayout layoutMembers;
    private MaterialButton btnCopyCode;

    private long bandId;
    private long userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_band, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        bandId = prefs.getLong("bandId", -1);
        userId = prefs.getLong("userId", -1);

        swipeRefresh  = view.findViewById(R.id.swipeRefreshBand);
        tvBandName    = view.findViewById(R.id.tvBandName);
        tvInviteCode  = view.findViewById(R.id.tvInviteCode);
        tvMemberCount = view.findViewById(R.id.tvMemberCount);
        layoutMembers = view.findViewById(R.id.layoutMembers);
        btnCopyCode   = view.findViewById(R.id.btnCopyCode);

        // Quick actions
        view.findViewById(R.id.btnNewSession).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Create session — coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnManageTasks).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Manage tasks — coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSubmissions).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Review submissions — coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnBandSettings).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Band settings — coming soon", Toast.LENGTH_SHORT).show());

        swipeRefresh.setColorSchemeResources(R.color.purple_primary);
        swipeRefresh.setProgressBackgroundColorSchemeResource(R.color.bg_surface);
        swipeRefresh.setOnRefreshListener(this::loadBand);

        loadBand();
    }

    private void loadBand() {
        if (bandId == -1) return;
        RetrofitClient.getInstance().getApi()
                .getBand(bandId)
                .enqueue(new Callback<BandResponse>() {
                    @Override
                    public void onResponse(Call<BandResponse> call, Response<BandResponse> response) {
                        if (!isAdded()) return;
                        swipeRefresh.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            bindBand(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<BandResponse> call, Throwable t) {
                        if (isAdded()) swipeRefresh.setRefreshing(false);
                    }
                });
    }

    private void bindBand(BandResponse band) {
        tvBandName.setText(band.getName());
        tvInviteCode.setText(band.getInviteCode());

        List<BandResponse.MemberDto> members = band.getMembers();
        int count = members != null ? members.size() : 0;
        tvMemberCount.setText(count + " member" + (count != 1 ? "s" : ""));

        btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(
                    ClipData.newPlainText("invite_code", band.getInviteCode()));
            Toast.makeText(requireContext(), "Invite code copied!", Toast.LENGTH_SHORT).show();
        });

        layoutMembers.removeAllViews();
        if (members != null) {
            for (BandResponse.MemberDto member : members) {
                addMemberRow(member);
            }
        }
    }

    private void addMemberRow(BandResponse.MemberDto member) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_band_member, layoutMembers, false);

        String name = member.getDisplayName() != null
                ? member.getDisplayName() : member.getEmail();
        String initial = name.isEmpty() ? "?" :
                String.valueOf(name.charAt(0)).toUpperCase();
        boolean isMe = member.getUserId() == userId;
        boolean isMemberLeader = "leader".equals(member.getRole());

        ((TextView) row.findViewById(R.id.tvMemberInitial)).setText(initial);
        ((TextView) row.findViewById(R.id.tvMemberName)).setText(
                isMe ? name + " (you)" : name);
        ((TextView) row.findViewById(R.id.tvMemberEmail)).setText(member.getEmail());

        TextView tvRole = row.findViewById(R.id.tvMemberRole);
        tvRole.setText(isMemberLeader ? "Leader" : "Member");
        tvRole.setBackgroundResource(isMemberLeader
                ? R.drawable.bg_role_leader
                : R.drawable.bg_role_member);
        tvRole.setTextColor(requireContext().getColor(
                isMemberLeader ? android.R.color.holo_orange_dark : R.color.purple_primary));

        layoutMembers.addView(row);
    }
}