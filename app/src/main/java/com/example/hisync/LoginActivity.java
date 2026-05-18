package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.LoginRequest;
import com.example.hisync.dto.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnSignIn;
    private TextView tvSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nếu đã login rồi thì vào thẳng MainActivity
        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        if (prefs.getLong("userId", -1) != -1) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupClickListeners();
    }

    private void initViews() {
        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnSignIn   = findViewById(R.id.btnSignIn);
        // btnGoogleSignIn đã bỏ — giữ nguyên XML nhưng không gán listener
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp    = findViewById(R.id.tvSignUp);

        // Ẩn nút Google vì không còn dùng
        MaterialButton btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) btnGoogle.setVisibility(android.view.View.GONE);
        if (tvForgotPassword != null) tvForgotPassword.setVisibility(android.view.View.GONE);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (validateInputs(email, password)) signIn(email, password);
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;
        tilEmail.setError(null);
        tilPassword.setError(null);
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email");
            valid = false;
        }
        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            valid = false;
        }
        return valid;
    }

    private void signIn(String email, String password) {
        setLoading(true);
        RetrofitClient.getInstance().getApi()
                .login(new LoginRequest(email, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful() && response.body() != null) {
                            saveSession(response.body());
                            navigateToMain();
                        } else {
                            tilPassword.setError("Email or password incorrect");
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(LoginActivity.this,
                                "Cannot connect to server: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveSession(LoginResponse body) {
        getSharedPreferences("hisync", MODE_PRIVATE).edit()
                .putLong("userId", body.getUserId())
                .putString("displayName", body.getDisplayName())
                .putString("email", body.getEmail())
                .putString("role", body.getRole())
                .apply();
    }

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}