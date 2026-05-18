package com.example.hisync.api;

import com.example.hisync.dto.LoginRequest;
import com.example.hisync.dto.LoginResponse;
import com.example.hisync.dto.RegisterRequest;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.TaskResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HisyncApi {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @GET("sessions")
    Call<List<SessionResponse>> getSessions(
            @Query("userId") long userId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("sessions/{id}")
    Call<SessionResponse> getSession(@Path("id") long sessionId);

    @GET("tasks")
    Call<List<TaskResponse>> getTasks(@Query("userId") long userId);

    @PATCH("tasks/{id}/status")
    Call<TaskResponse> updateTaskStatus(
            @Path("id") long taskId,
            @Body java.util.Map<String, String> body
    );
}