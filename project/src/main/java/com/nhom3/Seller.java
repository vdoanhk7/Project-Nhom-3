package com.nhom3;

public class Seller {
    public Seller(String userName, String password, String name, String email, String phoneNumber) {
        super(userName, password, name, email, phoneNumber);
    }
    @Override
    public String getRoleName() {
        return "Seller";    
    }
}
