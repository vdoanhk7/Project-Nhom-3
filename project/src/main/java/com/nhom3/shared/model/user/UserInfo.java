package com.nhom3.shared.model.user;

public class UserInfo {
    private String userName;
    private String password;
    private String name;
    
    public UserInfo(String userName, String password, String name) {
        this.userName = userName;
        this.password = password;
        this.name = name;
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
}
