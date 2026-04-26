package com.example.gamecatalog.data.models;

public class RegisterResponse {
    private boolean success;
    private String token;
    private int user_id;
    private String error;

    public boolean isSuccess() { return success; }
    public String getToken() { return token; }
    public int getUserId() { return user_id; }
    public String getError() { return error; }
}