package com.nhom3.shared.model.user;

import com.nhom3.shared.model.Entity;

public abstract class User extends Entity{
    protected UserInfo userInfo;
    protected UserContact userContact;

    public User(int id, UserInfo userInfo, UserContact userContact) {
        super(id);
        this.userInfo = userInfo;
        this.userContact = userContact;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public UserContact getUserContact() {
        return userContact;
    }
    
}