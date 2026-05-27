package com.example.hisync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int MIN_DURATION = 2000; // 2 seconds minimum

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        long startTime = System.currentTimeMillis();

        // Check login state
        SharedPreferences prefs = getSharedPreferences("hisync", MODE_PRIVATE);
        long userId = prefs.getLong("userId", -1);

        // Calculate remaining wait time
        long elapsed = System.currentTimeMillis() - startTime;
        long delay = Math.max(0, MIN_DURATION - elapsed);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (userId != -1) {
                // Already logged in → go straight to MainActivity
                intent = new Intent(this, MainActivity.class);
            } else {
                // Not logged in → go to Login
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, delay);
    }
}