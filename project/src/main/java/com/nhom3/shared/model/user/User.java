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
    
}