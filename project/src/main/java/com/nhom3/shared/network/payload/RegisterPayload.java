package com.nhom3.shared.network.payload;

public class RegisterPayload{
    private String fullName;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String role;

    // Constructor
    public RegisterPayload(String fullName, String username, String password, String email, String phone, String role) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    //Getters
    public String getFullName() {
        return fullName;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public String getPhone() {
        return phone;
    }
    public String getRole() {
        return role;
    }

    //Setters
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public void setRole(String role) {
        this.role = role;
    }
}