package com.nhom3.shared.network.payload;

public class ChangePasswordPayload {
    private final int userId;
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordPayload(int userId, String oldPassword, String newPassword) {
        this.userId = userId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public int getUserId() {
        return userId;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}
