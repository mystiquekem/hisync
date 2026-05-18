package com.example.hisync.dto;

import java.util.List;

public class SessionResponse {
    private long id;
    private String songTitle;
    private String date;       // ISO string, e.g. "2025-06-10T19:00:00"
    private long createdBy;
    private List<MemberResponse> members;
    private List<TaskResponse> tasks;

    public long getId()          { return id; }
    public String getSongTitle() { return songTitle; }
    public String getDate()      { return date; }
    public long getCreatedBy()   { return createdBy; }
    public List<MemberResponse> getMembers() { return members; }
    public List<TaskResponse> getTasks()     { return tasks; }

    public static class MemberResponse {
        private long userId;
        private String displayName;
        private String instrument;

        public long getUserId()        { return userId; }
        public String getDisplayName() { return displayName; }
        public String getInstrument()  { return instrument; }
    }
}