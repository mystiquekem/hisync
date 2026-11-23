package com.example.hisync.api;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.hisync.BuildConfig;
import com.example.hisync.LoginActivity;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String PREFS = "hisync";
    private static final String KEY_TOKEN = "token";

    private static RetrofitClient instance;
    private static Context appContext;
    private final HisyncApi api;

    /** Call once at app start (done in HisyncApp). */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    private RetrofitClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor());

        // Full request/response logging only in debug builds:
        // it prints passwords and login tokens to logcat.
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.SERVER_URL) // ← dùng BuildConfig
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(HisyncApi.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) instance = new RetrofitClient();
        return instance;
    }

    public HisyncApi getApi() {
        return api;
    }

    private static SharedPreferences prefs() {
        if (appContext == null)
            throw new IllegalStateException("RetrofitClient.init(context) was not called");
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Adds "Authorization: Bearer <token>" to every request.
     * If the server says 401 (token missing, expired or invalid), signs the
     * user out and opens the login screen.
     */
    private static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            String token = prefs().getString(KEY_TOKEN, null);

            if (token != null) {
                request = request.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
            }

            Response response = chain.proceed(request);

            // Login itself returns 401 for a wrong password — don't sign out for that.
            boolean isAuthCall = request.url().encodedPath().contains("/auth/");
            if (response.code() == 401 && !isAuthCall) {
                signOutAndShowLogin(token);
            }
            return response;
        }

        private static synchronized void signOutAndShowLogin(String tokenUsed) {
            SharedPreferences prefs = prefs();
            String current = prefs.getString(KEY_TOKEN, null);
            // Several requests can fail at once; only react to the first one.
            // Skip if the user has since logged in again with a new token...
            if (current != null && !current.equals(tokenUsed)) return;
            // ...or if another failed request already signed them out.
            if (current == null && !prefs.contains("userId")) return;

            int theme = prefs.getInt("theme_mode", -100);
            SharedPreferences.Editor editor = prefs.edit().clear();
            if (theme != -100) editor.putInt("theme_mode", theme); // keep theme choice
            editor.commit();

            Intent intent = new Intent(appContext, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        }
    }
}
