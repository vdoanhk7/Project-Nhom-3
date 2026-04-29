package com.nhom3.client;

import com.nhom3.client.network.ServerHandler;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.LoginPayload;

public class ClientTest {
    
    public static void main(String[] args) {
        ServerHandler serverHandler = new ServerHandler();
        boolean isConnected = false;
        while (!isConnected) {
            try {
                serverHandler.getServerConnection().connect();
                isConnected = true; // Kết nối thành công, thoát vòng lặp
            } catch (Exception e) {
                System.out.println("Không thể kết nối đến server. Đang thử lại...");
            }
        }
        Thread serverThread = new Thread(serverHandler);
        serverThread.start();
        // Gửi gói tin đăng nhập mẫu
        LoginPayload loginPayload = new LoginPayload("testuser", "password123");
        LoginPayload loginPayload2 = new LoginPayload("testuser2", "password123");
        Packet loginPacket = new Packet(PacketType.LOGIN, loginPayload);
        Packet loginPacket2 = new Packet(PacketType.LOGIN, loginPayload2);
        try {
            serverHandler.getServerConnection().sendMessage(loginPacket);
            serverHandler.getServerConnection().sendMessage(loginPacket2);
        } catch (Exception e) {
            System.out.println("Lỗi khi gửi yêu cầu đăng nhập: " + e.getMessage());
        }
    }
}
