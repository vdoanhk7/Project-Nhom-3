package com.nhom3.shared.network.payload;

public class ResultPayload {
    private boolean result;
    private String message; 
    
    private int userId;
    private String username;
    private String fullName;
    private String role; 
    
    // THÊM 2 BIẾN NÀY
    private String email;
    private String phone;

    // Cập nhật Constructor có chứa email và phone
    public ResultPayload(boolean result, String message, int userId, String username, String fullName, String role, String email, String phone) {
        this.result = result;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.phone = phone;
    }

    // Getters
    public boolean getResult() { return result; }
    public String getMessage() { return message; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getEmail() { return email; } // Lấy email
    public String getPhone() { return phone; } // Lấy sđt
}