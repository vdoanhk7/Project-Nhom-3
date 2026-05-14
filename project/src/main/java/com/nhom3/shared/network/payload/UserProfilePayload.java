package com.nhom3.shared.network.payload;

public class UserProfilePayload {
    private final int userId;
    private final String username;
    private final String fullName;
    private final String role;
    private final String email;
    private final String phone;

    public UserProfilePayload(int userId, String username, String fullName,
            String role, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.phone = phone;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }
}
