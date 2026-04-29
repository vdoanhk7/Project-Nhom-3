package com.nhom3.client.network;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.payload.LoginPayload;
import com.nhom3.shared.network.payload.ResultPayload;


public class ServerHandler extends Thread {
    private ServerConnection serverConnection;
    private boolean isListening;
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);

    public ServerHandler() {
        this.serverConnection = ServerConnection.getInstance();
        isListening = true;
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    @Override
    public void run() {
        while (isListening) {
            try {
                String jsonResponse = serverConnection.receiveResponse();
                Packet response = new Gson().fromJson(jsonResponse, Packet.class);
                // Xử lý gói tin dựa trên loại gói tin
                switch (response.getType()) {
                    // Add thêm các case khác UPDATE, ANOUNCEMENT,...
                    case RESULT:
                        // Sau dùng generic
                        String tempJson = new Gson().toJson(response.getPayload()); 
                        ResultPayload login = new Gson().fromJson(tempJson, ResultPayload.class);

                        logger.info("Kết quả: {}", login.getResult());
                        break;
                    default:
                        logger.warn("Loại gói tin không xác định: {}", response.getType());
                }
            } catch (IOException e) {
                logger.error("Lỗi khi nhận phản hồi từ server. Đang thử kết nối lại...", e);
                isListening = false; // Dừng lắng nghe để tránh vòng lặp vô hạn
            }
        }
    }
}
