package com.nhom3.shared.network.payload;

public class SystemLogSubscribePayload {
    private boolean isSub;

    public SystemLogSubscribePayload(boolean isSub) {
        this.isSub = isSub;
    }

    public boolean isSub() {
        return isSub;
    }

    public void setSub(boolean sub) {
        isSub = sub;
    }
}
