package com.example.hisync.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "http://192.168.2.10:8080/api/";
    private static RetrofitClient instance;
    private final HisyncApi api;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
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
}