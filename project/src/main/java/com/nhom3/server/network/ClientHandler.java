package com.nhom3.server.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.nhom3.shared.network.packet.Packet;
import com.nhom3.shared.network.packet.PacketType;
import com.nhom3.shared.network.payload.LoginPayload;
import com.nhom3.shared.network.payload.ResultPayload;

public class ClientHandler extends Thread {
    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        try {
            // Thiết lập luồng đọc/ghi với client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                // Nhận gói tin từ client và xử lý
                Packet request = new Gson().fromJson(line, Packet.class);
                // Xử lý gói tin dựa trên loại gói tin
                switch (request.getType()) {
                    // Add thêm các case khác LOGIN, BID,...
                    case LOGIN:
                        // Cast payload về đúng kiểu dữ liệu
                        // Sau dùng generic
                        String tempJson = new Gson().toJson(request.getPayload()); 
                        LoginPayload login = new Gson().fromJson(tempJson, LoginPayload.class);

                        logger.info("Đăng nhập từ: " + login.getUsername());
                        // Gửi phản hồi về client
                        ResultPayload resultPayload = new ResultPayload(true);
                        Packet response = new Packet(PacketType.RESULT, resultPayload);
                        out.write(new Gson().toJson(response));
                        out.newLine();
                        out.flush();
                        break;
                    default:
                        logger.warn("Loại gói tin không xác định: " + request.getType());
                }
            }
        } catch (IOException e) {
            logger.error("Lỗi I/O trong ClientHandler", e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Lỗi không xác định trong ClientHandler", e);
            e.printStackTrace();
        }
    }
}
