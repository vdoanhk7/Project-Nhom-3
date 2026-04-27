package com.nhom3.shared.network;

public class Request {

    private String action;
    private String payload;

    public Request(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }
    
    public String getAction() {
        return action;
    }

    public String getPayload() {
        return payload;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
