package com.nhom3.server.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import com.nhom3.server.network.liveUpdate.SystemLogAnnouncer;

public class SystemLogAppender extends AppenderBase<ILoggingEvent> {
    private Layout<ILoggingEvent> layout;
    
    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }
    
    @Override
    protected void append(ILoggingEvent eventObject) {
        String logLine;
        if (layout != null) {
            logLine = layout.doLayout(eventObject);
        } else {
            logLine = eventObject.getFormattedMessage() + "\n";
        }
        SystemLogAnnouncer.getInstance().broadcast(logLine);
    }
}
