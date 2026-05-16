package com.nhom3.shared.model.user;

import com.nhom3.shared.model.Entity;

public abstract class User extends Entity{
    protected UserInfo userInfo;
    protected UserContact userContact;
    protected final Role role;
    public User(int id, UserInfo userInfo, UserContact userContact, Role role) {
        super(id);
        this.userInfo = userInfo;
        this.userContact = userContact;
        this.role = role;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public UserContact getUserContact() {
        return userContact;
    }

    public Role getRole() {return role;}

    protected String formatUserInfo(String roleName) {
        String username = userInfo == null ? "N/A" : userInfo.getUserName();
        String fullName = userInfo == null ? "N/A" : userInfo.getName();
        String email = userContact == null ? "N/A" : userContact.getEmail();
        String phone = userContact == null ? "N/A" : userContact.getPhoneNumber();

        return String.format(
                "%s[id=%d, username=%s, fullName=%s, email=%s, phone=%s]",
                roleName,
                id,
                username,
                fullName,
                email,
                phone);
    }
    
}
