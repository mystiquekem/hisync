package com.example.hisync.dto;

public class RegisterRequest {
    private String email;
    private String password;
    private String displayName;

    public RegisterRequest(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }
}