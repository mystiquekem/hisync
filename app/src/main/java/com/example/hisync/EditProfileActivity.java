package com.example.hisync;

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

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etDisplayName;
    private TextInputLayout tilDisplayName;
    private MaterialButton btnSave;
    private long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_edit_profile);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        userId = prefs.getLong("userId", -1);
        String currentName = prefs.getString("displayName", "");

        tilDisplayName = findViewById(R.id.tilDisplayName);
        etDisplayName  = findViewById(R.id.etDisplayName);
        btnSave        = findViewById(R.id.btnSave);

        etDisplayName.setText(currentName);

        findViewById(R.id.tvBack).setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etDisplayName.getText() != null
                    ? etDisplayName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                tilDisplayName.setError("Enter a display name");
                return;
            }
            tilDisplayName.setError(null);
            saveProfile(name);
        });
    }

    private void saveProfile(String name) {
        btnSave.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("displayName", name);

        RetrofitClient.getInstance().getApi()
                .updateProfile(userId, body)
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnSave.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            // cập nhật lại SharedPreferences
                            getSharedPreferences("hisync", MODE_PRIVATE).edit()
                                    .putString("displayName", response.body().getDisplayName())
                                    .apply();
                            Toast.makeText(EditProfileActivity.this,
                                    "Profile updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(EditProfileActivity.this,
                                    "Failed to update", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        btnSave.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}