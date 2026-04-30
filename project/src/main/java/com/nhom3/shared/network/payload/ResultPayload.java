package com.nhom3.shared.network.payload;

public class ResultPayload {
    private boolean result;

    public ResultPayload(boolean result) {
        this.result = result;
    }

    //Getter and Setter
    public boolean getResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
