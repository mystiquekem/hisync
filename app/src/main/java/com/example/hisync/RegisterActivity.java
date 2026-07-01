package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LoginResponse;
import com.example.hisync.dto.RegisterRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvSignIn = findViewById(R.id.tvSignIn);

        btnRegister.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (validate(email, password)) register(email, password);
        });

        tvSignIn.setOnClickListener(v -> finish());
    }

    private boolean validate(String email, String password) {
        boolean valid = true;
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Minimum 6 characters");
            valid = false;
        }
        return valid;
    }

    private void register(String email, String password) {
        btnRegister.setEnabled(false);
        String displayName = email.split("@")[0];

        RetrofitClient.getInstance().getApi()
                .register(new RegisterRequest(email, password, displayName))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnRegister.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            saveSession(response.body());
                            // New users always go to instrument setup first
                            startActivity(new Intent(RegisterActivity.this,
                                    InstrumentSetupActivity.class));
                            finishAffinity();
                        } else {
                            tilEmail.setError("Registration failed — email may already exist");
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        tilEmail.setError("Cannot connect to server: " + t.getMessage());
                    }
                });
    }

    private void saveSession(LoginResponse body) {
        List<String> instruments = body.getInstruments();
        String instrumentsCsv = (instruments != null && !instruments.isEmpty())
                ? android.text.TextUtils.join(",", instruments)
                : "";

        getSharedPreferences("hisync", MODE_PRIVATE).edit()
                .putLong("userId", body.getUserId())
                .putString("displayName", body.getDisplayName())
                .putString("email", body.getEmail())
                .putString("role", body.getRole())
                .putString("instruments", instrumentsCsv)
                .apply();
    }
}