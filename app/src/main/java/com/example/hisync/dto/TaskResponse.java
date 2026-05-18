package com.example.hisync.dto;

public class TaskResponse {
    private long id;
    private long sessionId;
    private long assignedTo;
    private String title;
    private String status;  // "pending" | "done" | "rerecord"

    public long getId()         { return id; }
    public long getSessionId()  { return sessionId; }
    public long getAssignedTo() { return assignedTo; }
    public String getTitle()    { return title; }
    public String getStatus()   { return status; }
}