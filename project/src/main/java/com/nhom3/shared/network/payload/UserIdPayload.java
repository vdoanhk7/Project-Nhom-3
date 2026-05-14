package com.nhom3.shared.network.payload;

public class UserIdPayload {
    private int userId;

    public UserIdPayload() {}

    public UserIdPayload(int userId) {
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}
