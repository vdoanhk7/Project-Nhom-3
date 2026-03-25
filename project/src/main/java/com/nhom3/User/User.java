package com.nhom3.User;

import com.nhom3.Entity;

public abstract class User extends Entity {
    protected String userName;
    protected String password;
    protected String name;
    protected String email;
    protected String phoneNumber;
    public User(String id, String userName, String password, String name, String email, String phoneNumber) {
        super("U-" + id);
        this.userName = userName;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void displayInfo() {
        System.out.println("ID: " + id);
        System.out.println("Username: " + userName);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Phone Number: " + phoneNumber);
    }
}