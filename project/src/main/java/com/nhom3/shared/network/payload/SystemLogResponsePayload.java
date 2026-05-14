package com.nhom3.shared.network.payload;

public class SystemLogResponsePayload {
    private String logs;

    public SystemLogResponsePayload(String logs) {
        this.logs = logs;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }
}
