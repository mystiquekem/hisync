package com.example.hisync;

import android.app.Application;

import com.example.hisync.api.RetrofitClient;

/** Runs once when the app process starts, before any screen. */
public class HisyncApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}
