package com.nhom3.User;

public class Admin extends User {
    public Admin(String userName, String password, String name, String email, String phoneNumber) {
        super(userName, password, name, email, phoneNumber);
    }
    @Override
    public String getRoleName() {
        return "Admin";
    }
}
