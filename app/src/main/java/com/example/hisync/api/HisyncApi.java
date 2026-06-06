package com.example.hisync.api;

import com.example.hisync.dto.BandResponse;
import com.example.hisync.dto.LoginRequest;
import com.example.hisync.dto.LoginResponse;
import com.example.hisync.dto.RegisterRequest;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.SongResponse;
import com.example.hisync.dto.TaskResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface HisyncApi {

    // Auth
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    // Bands
    @POST("bands")
    Call<BandResponse> createBand(@Body Map<String, Object> body);

    @POST("bands/join")
    Call<BandResponse> joinBand(@Body Map<String, Object> body);

    @GET("bands")
    Call<List<BandResponse>> getMyBands(@Query("userId") long userId);

    @GET("bands/{id}")
    Call<BandResponse> getBand(@Path("id") long bandId);

    // Songs
    @GET("songs")
    Call<List<SongResponse>> getSongs(@Query("bandId") long bandId);

    @POST("songs")
    Call<SongResponse> addSong(@Body Map<String, Object> body);

    @DELETE("songs/{id}")
    Call<Void> deleteSong(@Path("id") long songId);

    // Sessions
    @GET("sessions")
    Call<List<SessionResponse>> getSessions(
            @Query("userId") long userId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("sessions/{id}")
    Call<SessionResponse> getSession(@Path("id") long sessionId);

    // Tasks
    @GET("tasks")
    Call<List<TaskResponse>> getTasks(@Query("userId") long userId);

    @PATCH("tasks/{id}/status")
    Call<TaskResponse> updateTaskStatus(
            @Path("id") long taskId,
            @Body Map<String, String> body
    );

    @PATCH("tasks/{id}/recording")
    Call<TaskResponse> submitRecording(
            @Path("id") long taskId,
            @Body Map<String, String> body
    );

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body Map<String, String> body);

    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body Map<String, String> body);

    @PATCH("users/{id}")
    Call<LoginResponse> updateProfile(
            @Path("id") long userId,
            @Body Map<String, String> body
    );
}