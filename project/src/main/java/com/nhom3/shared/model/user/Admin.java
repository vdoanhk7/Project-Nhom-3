package com.nhom3.shared.model.user;

public class Admin extends User {
    public Admin(int id, UserInfo userInfo, UserContact userContact) {
        super(id, userInfo, userContact, Role.ADMIN);
    }

    @Override
    public String printInfo() {
        return formatUserInfo("Admin");
    }
}
