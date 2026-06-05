package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BandSetupActivity extends AppCompatActivity {

    private LinearLayout layoutRoleSelect, layoutCreateBand, layoutJoinBand;
    private TextInputLayout tilBandName, tilBandDesc, tilInviteCode;
    private TextInputEditText etBandName, etBandDesc, etInviteCode;
    private MaterialButton btnCreateBand, btnJoinBand;

    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_band_setup);

        userId = getSharedPreferences("hisync", MODE_PRIVATE).getLong("userId", -1);

        layoutRoleSelect  = findViewById(R.id.layoutRoleSelect);
        layoutCreateBand  = findViewById(R.id.layoutCreateBand);
        layoutJoinBand    = findViewById(R.id.layoutJoinBand);
        tilBandName       = findViewById(R.id.tilBandName);
        tilBandDesc       = findViewById(R.id.tilBandDesc);
        tilInviteCode     = findViewById(R.id.tilInviteCode);
        etBandName        = findViewById(R.id.etBandName);
        etBandDesc        = findViewById(R.id.etBandDesc);
        etInviteCode      = findViewById(R.id.etInviteCode);
        btnCreateBand     = findViewById(R.id.btnCreateBand);
        btnJoinBand       = findViewById(R.id.btnJoinBand);

        showScreen("role");

        // Role selection
        findViewById(R.id.cardLeader).setOnClickListener(v -> showScreen("create"));
        findViewById(R.id.cardMember).setOnClickListener(v -> showScreen("join"));

        // Back buttons
        findViewById(R.id.tvBackToRole1).setOnClickListener(v -> showScreen("role"));
        findViewById(R.id.tvBackToRole2).setOnClickListener(v -> showScreen("role"));

        // Actions
        btnCreateBand.setOnClickListener(v -> createBand());
        btnJoinBand.setOnClickListener(v -> joinBand());
    }

    private void showScreen(String screen) {
        layoutRoleSelect.setVisibility(screen.equals("role")   ? View.VISIBLE : View.GONE);
        layoutCreateBand.setVisibility(screen.equals("create") ? View.VISIBLE : View.GONE);
        layoutJoinBand.setVisibility(screen.equals("join")     ? View.VISIBLE : View.GONE);
    }

    private void createBand() {
        String name = getText(etBandName);
        String desc = getText(etBandDesc);
        if (name.isEmpty()) { tilBandName.setError("Enter band name"); return; }
        tilBandName.setError(null);

        btnCreateBand.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", desc);
        body.put("createdBy", userId);

        RetrofitClient.getInstance().getApi()
                .createBand(body)
                .enqueue(new Callback<BandResponse>() {
                    @Override
                    public void onResponse(Call<BandResponse> call, Response<BandResponse> response) {
                        btnCreateBand.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            saveBandAndNavigate(response.body());
                        } else {
                            Toast.makeText(BandSetupActivity.this,
                                    "Failed to create band", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<BandResponse> call, Throwable t) {
                        btnCreateBand.setEnabled(true);
                        Toast.makeText(BandSetupActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void joinBand() {
        String code = getText(etInviteCode);
        if (code.isEmpty()) { tilInviteCode.setError("Enter invite code"); return; }
        tilInviteCode.setError(null);

        btnJoinBand.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("inviteCode", code);
        body.put("userId", userId);

        RetrofitClient.getInstance().getApi()
                .joinBand(body)
                .enqueue(new Callback<BandResponse>() {
                    @Override
                    public void onResponse(Call<BandResponse> call, Response<BandResponse> response) {
                        btnJoinBand.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            saveBandAndNavigate(response.body());
                        } else {
                            tilInviteCode.setError("Invalid invite code");
                        }
                    }
                    @Override
                    public void onFailure(Call<BandResponse> call, Throwable t) {
                        btnJoinBand.setEnabled(true);
                        Toast.makeText(BandSetupActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveBandAndNavigate(BandResponse band) {
        getSharedPreferences("hisync", MODE_PRIVATE).edit()
                .putLong("bandId", band.getId())
                .putString("bandName", band.getName())
                .apply();
        startActivity(new Intent(this, MainActivity.class));
        finishAffinity();
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}