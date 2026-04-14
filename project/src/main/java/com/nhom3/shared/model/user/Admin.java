package com.nhom3.shared.model.user;

public class Admin extends User {
    private Role role = Role.ADMIN;
    public Admin(int id, UserInfo userInfo, UserContact userContact) {
        super(id, userInfo, userContact, Role.ADMIN);
    }
}

