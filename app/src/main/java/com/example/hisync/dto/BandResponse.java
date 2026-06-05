package com.example.hisync.dto;

import java.util.List;

public class BandResponse {
    private long id;
    private String name;
    private String description;
    private String inviteCode;
    private List<MemberDto> members;

    public long getId()           { return id; }
    public String getName()       { return name; }
    public String getDescription(){ return description; }
    public String getInviteCode() { return inviteCode; }
    public List<MemberDto> getMembers() { return members; }

    public static class MemberDto {
        private long userId;
        private String displayName;
        private String email;
        private String role;

        public long getUserId()        { return userId; }
        public String getDisplayName() { return displayName; }
        public String getEmail()       { return email; }
        public String getRole()        { return role; }
    }
}