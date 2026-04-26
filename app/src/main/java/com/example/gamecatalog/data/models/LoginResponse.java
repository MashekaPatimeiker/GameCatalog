package com.example.gamecatalog.data.models;

public class LoginResponse {
    private boolean success;
    private String token;
    private int user_id;
    private String username;
    private String error;

    // Getters and setters
    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public int getUserId() { return user_id; }
    public String getUsername() { return username; }
    public String getError() { return error; }
}