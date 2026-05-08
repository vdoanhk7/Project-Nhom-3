package com.nhom3.client.network;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import javafx.application.Platform;

public class ServerHandler extends Thread {
    private ServerConnection serverConnection;
    private boolean isListening;
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private Gson gson = new Gson();
    private ClientPacketDispatcher dispatcher;

    public ServerHandler() {
        this.serverConnection = ServerConnection.getInstance();
        this.dispatcher = new ClientPacketDispatcher();
        isListening = true;
    }

    public ServerConnection getServerConnection() { return serverConnection; }

    @Override
    public void run() {
        while (isListening) {
            try {
                String jsonResponse = serverConnection.receiveResponse();
                Packet response = gson.fromJson(jsonResponse, Packet.class);
                System.out.println("[CLIENT - RAW RECEIVE] Vừa nhận phản hồi loại: " + response.getType());
                
                // Sử dụng Dispatcher để xử lý, luôn đảm bảo chạy trên luồng Giao diện (UI Thread)
                Platform.runLater(() -> {
                    dispatcher.dispatch(response, gson);
                });

            } catch (IOException e) {
                logger.error("Lỗi mất kết nối. Đang thử kết nối lại...", e);
                isListening = false; 
            }
        }
    }
}