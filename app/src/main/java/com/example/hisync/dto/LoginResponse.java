package com.example.hisync.dto;

public class LoginResponse {
    private long userId;
    private String email;
    private String displayName;
    private String role;

    public long getUserId()      { return userId; }
    public String getEmail()     { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole()      { return role; }
}