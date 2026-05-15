package com.nhom3.shared.model.user;

public class UserInfo {
    private String userName;
    private String password;
    private String name;
    private String profileImageBase64;
    
    public UserInfo(String userName, String password, String name) {
        this(userName, password, name, null);
    }

    public UserInfo(String userName, String password, String name, String profileImageBase64) {
        this.userName = userName;
        this.password = password;
        this.name = name;
        this.profileImageBase64 = profileImageBase64;
    }
    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
}
