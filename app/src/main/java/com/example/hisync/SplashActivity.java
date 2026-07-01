package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
        // Apply saved theme before setContentView
        int savedTheme = getSharedPreferences("hisync", MODE_PRIVATE)
                .getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (userId == -1) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
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
                        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {
                            prefs.edit()
                                    .putLong("bandId", response.body().get(0).getId())
                                    .putString("bandName", response.body().get(0).getName())
                                    .apply();
                        }

                        navigateAfterSplash(prefs);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<List<BandResponse>> call, Throwable t) {
                        navigateAfterSplash(getSharedPreferences("hisync", MODE_PRIVATE));
                        finish();
                    }
                });
    }

    /**
     * Routing priority:
     * 1. No instruments saved → InstrumentSetupActivity
     * 2. No band → BandSetupActivity
     * 3. Otherwise → MainActivity
     */
    private void navigateAfterSplash(SharedPreferences prefs) {
        String instruments = prefs.getString("instruments", "");
        long bandId = prefs.getLong("bandId", -1);

        if (instruments.isEmpty()) {
            startActivity(new Intent(this, InstrumentSetupActivity.class));
        } else if (bandId == -1) {
            startActivity(new Intent(this, BandSetupActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }
}