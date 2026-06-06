package com.example.hisync.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.hisync.EditProfileActivity;
import com.example.hisync.ForgotPasswordActivity;
import com.example.hisync.MainActivity;
import com.example.hisync.R;
import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;
import com.example.hisync.dto.TaskResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextView tvStatSessions, tvStatTasks, tvStatMembers;
    private TextView tvProfileBand, tvProfileRole;
    private long userId, bandId;
    private String bandName, inviteCode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("hisync", AppCompatActivity.MODE_PRIVATE);
        String displayName = prefs.getString("displayName", "");
        String email       = prefs.getString("email", "");
        String role        = prefs.getString("role", "member");
        userId  = prefs.getLong("userId", -1);
        bandId  = prefs.getLong("bandId", -1);
        bandName = prefs.getString("bandName", "");

        String name = (displayName != null && !displayName.isEmpty())
                ? displayName : email.split("@")[0];

        // Header
        ((TextView) view.findViewById(R.id.tvProfileName)).setText(name);
        ((TextView) view.findViewById(R.id.tvProfileEmail)).setText(email);
        ((TextView) view.findViewById(R.id.tvProfileAvatar))
                .setText(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());

        tvProfileRole = view.findViewById(R.id.tvProfileRole);
        tvProfileBand = view.findViewById(R.id.tvProfileBand);

        boolean isLeader = "leader".equals(role) || "admin".equals(role);
        tvProfileRole.setText(isLeader ? "Leader" : "Member");
        tvProfileRole.setBackgroundResource(isLeader
                ? R.drawable.bg_role_leader : R.drawable.bg_role_member);
        tvProfileRole.setTextColor(requireContext().getColor(
                isLeader ? android.R.color.holo_orange_dark : R.color.purple_primary));
        tvProfileBand.setText(bandName != null && !bandName.isEmpty() ? bandName : "No band");

        // Stats
        tvStatSessions = view.findViewById(R.id.tvStatSessions);
        tvStatTasks    = view.findViewById(R.id.tvStatTasks);
        tvStatMembers  = view.findViewById(R.id.tvStatMembers);

        loadStats();

        // Edit button in header
        ((MaterialButton) view.findViewById(R.id.btnEditProfile))
                .setOnClickListener(v ->
                        startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        // Menu
        view.findViewById(R.id.itemEditProfile).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        view.findViewById(R.id.itemChangePassword).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ForgotPasswordActivity.class)));

        view.findViewById(R.id.itemBandInfo).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Band info — coming soon", Toast.LENGTH_SHORT).show());

        view.findViewById(R.id.itemInviteCode).setOnClickListener(v -> {
            if (inviteCode != null && !inviteCode.isEmpty()) {
                ClipboardManager cm = (ClipboardManager)
                        requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("invite_code", inviteCode));
                Toast.makeText(requireContext(), "Invite code copied!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No invite code available", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.itemSignOut).setOnClickListener(v ->
                ((MainActivity) requireActivity()).signOut());
    }

    private void loadStats() {
        // Tasks done
        if (userId != -1) {
            RetrofitClient.getInstance().getApi()
                    .getTasks(userId)
                    .enqueue(new Callback<List<TaskResponse>>() {
                        @Override
                        public void onResponse(Call<List<TaskResponse>> call,
                                               Response<List<TaskResponse>> response) {
                            if (!isAdded() || response.body() == null) return;
                            long done = response.body().stream()
                                    .filter(t -> "done".equals(t.getStatus())).count();
                            tvStatTasks.setText(String.valueOf(done));
                        }
                        @Override
                        public void onFailure(Call<List<TaskResponse>> call, Throwable t) {}
                    });
        }

        // Band members + invite code
        if (bandId != -1) {
            RetrofitClient.getInstance().getApi()
                    .getBand(bandId)
                    .enqueue(new Callback<BandResponse>() {
                        @Override
                        public void onResponse(Call<BandResponse> call,
                                               Response<BandResponse> response) {
                            if (!isAdded() || response.body() == null) return;
                            inviteCode = response.body().getInviteCode();
                            List<BandResponse.MemberDto> members = response.body().getMembers();
                            int count = members != null ? members.size() : 0;
                            tvStatMembers.setText(String.valueOf(count));
                        }
                        @Override
                        public void onFailure(Call<BandResponse> call, Throwable t) {}
                    });
        }

        // Sessions — dùng getSessions với range rộng
        tvStatSessions.setText("—");
    }
}