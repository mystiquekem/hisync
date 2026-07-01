package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;
import com.example.hisync.dto.LoginRequest;
import com.example.hisync.dto.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

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

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        if (prefs.getLong("userId", -1) != -1) {
            navigateAfterLogin(prefs);
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
        tvSignUp    = findViewById(R.id.tvSignUp);

        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvForgotPassword.setVisibility(android.view.View.VISIBLE);
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        MaterialButton btnGoogle = findViewById(R.id.btnGoogleSignIn);
        if (btnGoogle != null) btnGoogle.setVisibility(android.view.View.GONE);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> {
            String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
            if (validateInputs(email, password)) signIn(email, password);
        });

        tvSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
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
                        if (response.isSuccessful() && response.body() != null) {
                            saveSession(response.body());
                            fetchBandThenNavigate(response.body().getUserId());
                        } else {
                            setLoading(false);
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

    private void fetchBandThenNavigate(long userId) {
        RetrofitClient.getInstance().getApi()
                .getMyBands(userId)
                .enqueue(new Callback<List<BandResponse>>() {
                    @Override
                    public void onResponse(Call<List<BandResponse>> call,
                                           Response<List<BandResponse>> response) {
                        setLoading(false);
                        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {
                            prefs.edit()
                                    .putLong("bandId", response.body().get(0).getId())
                                    .putString("bandName", response.body().get(0).getName())
                                    .apply();
                        }

                        navigateAfterLogin(prefs);
                    }

                    @Override
                    public void onFailure(Call<List<BandResponse>> call, Throwable t) {
                        setLoading(false);
                        navigateAfterLogin(getSharedPreferences("hisync", MODE_PRIVATE));
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

    private void navigateAfterLogin(SharedPreferences prefs) {
        String instruments = prefs.getString("instruments", "");
        long bandId = prefs.getLong("bandId", -1);

        Intent next;
        if (instruments.isEmpty()) {
            next = new Intent(this, InstrumentSetupActivity.class);
        } else if (bandId == -1) {
            next = new Intent(this, BandSetupActivity.class);
        } else {
            next = new Intent(this, MainActivity.class);
        }
        startActivity(next);
        finish();
    }

    private void setLoading(boolean loading) {
        btnSignIn.setEnabled(!loading);
    }
}