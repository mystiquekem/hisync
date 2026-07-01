package com.example.hisync.api;

import com.example.hisync.dto.BandResponse;
import com.example.hisync.dto.LineupResponse;
import com.example.hisync.dto.LoginRequest;
import com.example.hisync.dto.LoginResponse;
import com.example.hisync.dto.RegisterRequest;
import com.example.hisync.dto.SessionResponse;
import com.example.hisync.dto.SessionTasksDto;
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

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @POST("auth/forgot-password")
    Call<Void> forgotPassword(@Body Map<String, String> body);

    @POST("auth/reset-password")
    Call<Void> resetPassword(@Body Map<String, String> body);

    // ── Users ─────────────────────────────────────────────────────────────────
    @PATCH("users/{id}")
    Call<LoginResponse> updateProfile(
            @Path("id") long userId,
            @Body Map<String, Object> body
    );

    @GET("users/{id}/instruments")
    Call<List<String>> getInstruments(@Path("id") long userId);

    // ── Bands ─────────────────────────────────────────────────────────────────
    @POST("bands")
    Call<BandResponse> createBand(@Body Map<String, Object> body);

    @POST("bands/join")
    Call<BandResponse> joinBand(@Body Map<String, Object> body);

    @GET("bands")
    Call<List<BandResponse>> getMyBands(@Query("userId") long userId);

    @GET("bands/{id}")
    Call<BandResponse> getBand(@Path("id") long bandId);

    // ── Lineups ───────────────────────────────────────────────────────────────
    @POST("lineups")
    Call<LineupResponse> createLineup(@Body Map<String, Object> body);

    @GET("lineups")
    Call<List<LineupResponse>> getLineups(@Query("bandId") long bandId);

    @GET("lineups/{id}")
    Call<LineupResponse> getLineup(@Path("id") long lineupId);

    @PATCH("lineups/{id}")
    Call<LineupResponse> updateLineup(
            @Path("id") long lineupId,
            @Body Map<String, Object> body
    );

    @DELETE("lineups/{id}")
    Call<Void> deleteLineup(@Path("id") long lineupId);

    // ── Sessions ──────────────────────────────────────────────────────────────
    @GET("sessions")
    Call<List<SessionResponse>> getSessionsByUser(
            @Query("userId") long userId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("sessions")
    Call<List<SessionResponse>> getSessionsByBand(
            @Query("bandId") long bandId,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("sessions/{id}")
    Call<SessionResponse> getSession(@Path("id") long sessionId);

    @POST("sessions")
    Call<SessionResponse> createSession(@Body Map<String, Object> body);

    @PATCH("sessions/{id}")
    Call<SessionResponse> updateSession(
            @Path("id") long sessionId,
            @Body Map<String, Object> body
    );

    @DELETE("sessions/{id}")
    Call<Void> deleteSession(@Path("id") long sessionId);

    // ── Tasks ─────────────────────────────────────────────────────────────────
    @GET("tasks")
    Call<List<TaskResponse>> getTasks(@Query("userId") long userId);

    @GET("tasks/band/{bandId}")
    Call<List<SessionTasksDto>> getTasksByBand(@Path("bandId") long bandId);

    @POST("tasks")
    Call<TaskResponse> createTask(@Body Map<String, Object> body);

    @PATCH("tasks/{id}")
    Call<TaskResponse> updateTask(
            @Path("id") long taskId,
            @Body Map<String, Object> body
    );

    @DELETE("tasks/{id}")
    Call<Void> deleteTask(@Path("id") long taskId);

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