package com.example.hisync;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.hisync.api.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    // Step 1
    private LinearLayout layoutStep1, layoutStep2;
    private TextInputLayout tilEmail, tilOtp, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etEmail, etOtp, etNewPassword, etConfirmPassword;
    private MaterialButton btnSendOtp, btnResetPassword;
    private TextView tvBack;

    private String confirmedEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_forgot_password);

        layoutStep1       = findViewById(R.id.layoutStep1);
        layoutStep2       = findViewById(R.id.layoutStep2);
        tilEmail          = findViewById(R.id.tilEmail);
        tilOtp            = findViewById(R.id.tilOtp);
        tilNewPassword    = findViewById(R.id.tilNewPassword);
        tilConfirmPassword= findViewById(R.id.tilConfirmPassword);
        etEmail           = findViewById(R.id.etEmail);
        etOtp             = findViewById(R.id.etOtp);
        etNewPassword     = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSendOtp        = findViewById(R.id.btnSendOtp);
        btnResetPassword  = findViewById(R.id.btnResetPassword);
        tvBack            = findViewById(R.id.tvBack);

        showStep(1);

        tvBack.setOnClickListener(v -> finish());

        btnSendOtp.setOnClickListener(v -> {
            String email = getText(etEmail);
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.setError("Enter a valid email");
                return;
            }
            tilEmail.setError(null);
            sendOtp(email);
        });

        btnResetPassword.setOnClickListener(v -> {
            String otp      = getText(etOtp);
            String password = getText(etNewPassword);
            String confirm  = getText(etConfirmPassword);

            boolean valid = true;
            tilOtp.setError(null);
            tilNewPassword.setError(null);
            tilConfirmPassword.setError(null);

            if (otp.length() != 6) {
                tilOtp.setError("Enter the 6-digit OTP");
                valid = false;
            }
            if (password.length() < 6) {
                tilNewPassword.setError("Minimum 6 characters");
                valid = false;
            }
            if (!password.equals(confirm)) {
                tilConfirmPassword.setError("Passwords do not match");
                valid = false;
            }
            if (valid) resetPassword(otp, password);
        });
    }

    private void sendOtp(String email) {
        btnSendOtp.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        RetrofitClient.getInstance().getApi()
                .forgotPassword(body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnSendOtp.setEnabled(true);
                        if (response.isSuccessful()) {
                            confirmedEmail = email;
                            showStep(2);
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "OTP sent to " + email, Toast.LENGTH_LONG).show();
                        } else {
                            tilEmail.setError("Email not registered");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnSendOtp.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetPassword(String otp, String newPassword) {
        btnResetPassword.setEnabled(false);
        Map<String, String> body = new HashMap<>();
        body.put("email", confirmedEmail);
        body.put("otp", otp);
        body.put("newPassword", newPassword);

        RetrofitClient.getInstance().getApi()
                .resetPassword(body)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        btnResetPassword.setEnabled(true);
                        if (response.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Password reset successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            tilOtp.setError("Invalid or expired OTP");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnResetPassword.setEnabled(true);
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showStep(int step) {
        layoutStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        layoutStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}