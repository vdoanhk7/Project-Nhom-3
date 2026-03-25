package com.nhom3.User;

public class Admin extends User {
    public Admin(String id, String userName, String password, String name, String email, String phoneNumber) {
        super("AD-" + id, userName, password, name, email, phoneNumber);
    }
}

