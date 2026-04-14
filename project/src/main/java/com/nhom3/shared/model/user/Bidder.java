package com.nhom3.shared.model.user;

public class Bidder extends User {
    private Role role = Role.BIDDER;
    public Bidder(int id, UserInfo userInfo, UserContact userContact) {
        super(id, userInfo, userContact, Role.BIDDER);
    }
}
