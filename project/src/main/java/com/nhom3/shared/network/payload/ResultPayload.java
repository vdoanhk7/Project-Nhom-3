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
    private String profileImageBase64;
    private int reputationScore;
    private double sellerRatingAverage;
    private int sellerRatingCount;

    // Cập nhật Constructor có chứa email và phone
    public ResultPayload(boolean result, String message, int userId, String username, String fullName, String role, String email, String phone) {
        this(result, message, userId, username, fullName, role, email, phone, null);
    }

    public ResultPayload(boolean result, String message, int userId, String username, String fullName,
            String role, String email, String phone, String profileImageBase64) {
        this(result, message, userId, username, fullName, role, email, phone, profileImageBase64,
                com.nhom3.shared.model.user.User.DEFAULT_REPUTATION_SCORE);
    }

    public ResultPayload(boolean result, String message, int userId, String username, String fullName,
            String role, String email, String phone, String profileImageBase64, int reputationScore) {
        this(result, message, userId, username, fullName, role, email, phone, profileImageBase64,
                reputationScore, 0, 0);
    }

    public ResultPayload(boolean result, String message, int userId, String username, String fullName,
            String role, String email, String phone, String profileImageBase64, int reputationScore,
            double sellerRatingAverage, int sellerRatingCount) {
        this.result = result;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.profileImageBase64 = profileImageBase64;
        this.reputationScore = Math.max(0,
                Math.min(com.nhom3.shared.model.user.User.DEFAULT_REPUTATION_SCORE, reputationScore));
        this.sellerRatingAverage = Math.max(0, Math.min(5, sellerRatingAverage));
        this.sellerRatingCount = Math.max(0, sellerRatingCount);
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
    public String getProfileImageBase64() { return profileImageBase64; }
    public int getReputationScore() { return reputationScore; }
    public double getSellerRatingAverage() { return sellerRatingAverage; }
    public int getSellerRatingCount() { return sellerRatingCount; }
}
