package com.nhom3.User;

public class Bidder extends User {
    public Bidder(String id, String userName, String password, String name, String email, String phoneNumber) {
        super("BD-" + id, userName, password, name, email, phoneNumber);
    }
    @Override
    public String getRoleName() {
        return "BIDDER";    
    }
}
