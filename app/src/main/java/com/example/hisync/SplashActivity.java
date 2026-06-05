package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hisync.api.RetrofitClient;
import com.example.hisync.dto.BandResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private static final int MIN_DURATION = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (userId == -1) {
                // Chưa login
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                // Đã login → fetch band
                fetchBandThenNavigate(userId);
            }
        }, MIN_DURATION);
    }

    private void fetchBandThenNavigate(long userId) {
        RetrofitClient.getInstance().getApi()
                .getMyBands(userId)
                .enqueue(new Callback<List<BandResponse>>() {
                    @Override
                    public void onResponse(Call<List<BandResponse>> call,
                                           Response<List<BandResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {
                            getSharedPreferences("hisync", MODE_PRIVATE).edit()
                                    .putLong("bandId", response.body().get(0).getId())
                                    .putString("bandName", response.body().get(0).getName())
                                    .apply();
                            startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        } else {
                            startActivity(new Intent(SplashActivity.this, BandSetupActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onFailure(Call<List<BandResponse>> call, Throwable t) {
                        // Network lỗi → vào thẳng main
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }
}