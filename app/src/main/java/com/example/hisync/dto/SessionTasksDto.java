package com.example.hisync.dto;

import java.util.List;

public class SessionTasksDto {
    private long sessionId;
    private String songTitle;
    private String sessionDate;
    private List<TaskResponse> tasks;

    public long getSessionId()          { return sessionId; }
    public String getSongTitle()        { return songTitle; }
    public String getSessionDate()      { return sessionDate; }
    public List<TaskResponse> getTasks(){ return tasks; }
}