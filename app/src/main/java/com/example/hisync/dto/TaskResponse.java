package com.example.hisync.dto;

public class TaskResponse {
    private long id;
    private long sessionId;
    private String title;
    private String status;
    private String assignedToName;
    private String recordingUrl;
    private String sessionSong;   // new
    private String sessionDate;   // new

    public long getId()              { return id; }
    public long getSessionId()       { return sessionId; }
    public String getTitle()         { return title; }
    public String getStatus()        { return status; }
    public String getAssignedToName(){ return assignedToName; }
    public String getRecordingUrl()  { return recordingUrl; }
    public String getSessionSong()   { return sessionSong; }
    public String getSessionDate()   { return sessionDate; }
}