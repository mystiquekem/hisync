package com.example.hisync.dto;

import java.util.List;

public class LoginResponse {
    private String token;
    private long userId;
    private String email;
    private String displayName;
    private String role;
    private List<String> instruments;

    public String getToken()             { return token; }
    public long getUserId()              { return userId; }
    public String getEmail()             { return email; }
    public String getDisplayName()       { return displayName; }
    public String getRole()              { return role; }
    public List<String> getInstruments() { return instruments; }
}