package com.example.hisync.model;

public class Task {
    public long id;
    public long sessionId;
    public long assignedTo;
    public String title;
    public String status; // "pending" | "done" | "rerecord"

    public Task() {}
}