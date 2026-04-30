package com.nhom3.shared.network.payload;

public class LoginPayload{
    private String username;
    private String password;

    // Constructor
    public LoginPayload(String username, String password) {
        this.username = username;
        this.password = password;
    }

    //Getters
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }

    //Setters
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}