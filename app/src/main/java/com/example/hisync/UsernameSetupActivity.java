package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UsernameSetupActivity extends AppCompatActivity {

    private TextInputLayout tilUsername;
    private TextInputEditText etUsername;
    private MaterialButton btnContinue;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_username_setup);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);

        tilUsername = findViewById(R.id.tilUsername);
        etUsername  = findViewById(R.id.etUsername);
        btnContinue = findViewById(R.id.btnContinue);

        // Pre-fill với displayName hiện tại nếu có
        String current = prefs.getString("displayName", "");
        if (!current.isEmpty()) etUsername.setText(current);

        btnContinue.setOnClickListener(v -> {
            String name = etUsername.getText() != null
                    ? etUsername.getText().toString().trim() : "";
            if (name.isEmpty()) {
                tilUsername.setError("Please enter a name");
                return;
            }
            if (name.length() < 2) {
                tilUsername.setError("At least 2 characters");
                return;
            }
            tilUsername.setError(null);
            saveUsername(name);
        });

        // Skip button
        findViewById(R.id.tvSkip).setOnClickListener(v -> goNext());
    }

    private void saveUsername(String name) {
        btnContinue.setEnabled(false);
        Map<String, Object> body = new HashMap<>();
        body.put("displayName", name);

        RetrofitClient.getInstance().getApi()
                .updateProfile(userId, body)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnContinue.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            // Update SharedPreferences ngay
                            getSharedPreferences("hisync", MODE_PRIVATE).edit()
                                    .putString("displayName", name)
                                    .apply();
                            goNext();
                        } else {
                            Toast.makeText(UsernameSetupActivity.this,
                                    "Failed to save name", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        btnContinue.setEnabled(true);
                        // Network lỗi → vẫn lưu local và tiếp tục
                        getSharedPreferences("hisync", MODE_PRIVATE).edit()
                                .putString("displayName", name)
                                .apply();
                        goNext();
                    }
                });
    }

    private void goNext() {
        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        boolean onboardingDone = prefs.getBoolean("onboarding_done", false);
        Intent next = onboardingDone
                ? new Intent(this, BandSetupActivity.class)
                : new Intent(this, OnboardingActivity.class);
        startActivity(next);
        finish();
    }
}