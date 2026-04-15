package com.example.hisync.model;

public class Task {
    public String id;
    public String sessionId;
    public String assignedTo;
    public String title;
    public String status; // "pending" | "done" | "rerecord"

    public Task() {}
}