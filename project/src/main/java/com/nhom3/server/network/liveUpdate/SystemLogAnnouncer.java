package com.nhom3.server.network.liveUpdate;

import com.nhom3.server.network.ClientHandler;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.SystemLogResponsePayload;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SystemLogAnnouncer {
    private static SystemLogAnnouncer instance;
    private final Set<ClientHandler> observers;

    private SystemLogAnnouncer() {
        this.observers = new CopyOnWriteArraySet<>();
    }

    public synchronized static SystemLogAnnouncer getInstance() {
        if (instance == null) {
            instance = new SystemLogAnnouncer();
        }
        return instance;
    }

    public void addObserver(ClientHandler client) {
        observers.add(client);
        sendFullLogToClient(client);
    }

    public void removeObserver(ClientHandler client) {
        observers.remove(client);
    }

    public void broadcast(String newLogLine) {
        if (observers.isEmpty()) {
            return;
        }
        Packet packet = new Packet(PacketType.SYSTEM_LOGS_RESPONSE, new SystemLogResponsePayload(newLogLine, true));
        for (ClientHandler client : observers) {
            try {
                client.send(packet);
            } catch (IOException e) {
                removeObserver(client);
            }
        }
    }

    private void sendFullLogToClient(ClientHandler client) {
        StringBuilder logs = new StringBuilder();
        try {
            java.io.File file = new java.io.File("logs/server.log");
            if (file.exists()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        logs.append(line).append("\n");
                    }
                }
            } else {
                logs.append("Không tìm thấy file log hệ thống (logs/server.log).\n");
            }
        } catch (Exception e) {
            logs.append("Lỗi đọc file log: ").append(e.getMessage());
        }
        try {
            client.send(new Packet(PacketType.SYSTEM_LOGS_RESPONSE, new SystemLogResponsePayload(logs.toString(), false)));
        } catch (IOException e) {
            removeObserver(client);
        }
    }
}
