package com.nhom3.shared.network.payload;

public class SystemLogResponsePayload {
    private String logs;
    private boolean isAppend;

    public SystemLogResponsePayload(String logs, boolean isAppend) {
        this.logs = logs;
        this.isAppend = isAppend;
    }

    public String getLogs() {
        return logs;
    }

    public void setLogs(String logs) {
        this.logs = logs;
    }

    public boolean isAppend() {
        return isAppend;
    }

    public void setAppend(boolean append) {
        isAppend = append;
    }
}

