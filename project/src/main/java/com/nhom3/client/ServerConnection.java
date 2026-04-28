package com.nhom3.client;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.nhom3.shared.network.Request;

public class ServerConnection {
    // Thay đổi nếu server chạy trên host/port khác
    private static final String SERVER_HOST = "localhost"; 
    private static final int SERVER_PORT = 8080; 

    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);   
    private static ServerConnection instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final Gson gson = new Gson();

    // Singleton constructor
    private ServerConnection() {}

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Gửi yêu cầu đến server
    public void sendMessage(Request request) {
        try {
            String jsonRequest = gson.toJson(request);
            out.println(jsonRequest);
            logger.debug("Gửi đến server: {}", jsonRequest);
        } catch (Exception e) {
            logger.error("Lỗi khi gửi yêu cầu đến server: ", e);
        }
    }

    // Nhận phản hồi từ server
    public String receiveResponse() {
        try {
            String jsonResponse = in.readLine();
            if (jsonResponse == null) {
                logger.warn("Server đã đóng kết nối.");
                throw new RuntimeException("Server đã đóng kết nối.");
            }
            logger.debug("Nhận từ server: {}", jsonResponse);
            return jsonResponse;
        } catch (Exception e) {
            logger.error("Lỗi khi nhận phản hồi từ server", e);
            throw new RuntimeException("Lỗi khi nhận phản hồi từ server", e);
        }
    }

    // Kết nối đến server
    public void connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            logger.info("Kết nối đến server thành công tại {}:{}", SERVER_HOST, SERVER_PORT);
        } catch (Exception e) {
            logger.error("Lỗi kết nối đến server: ", e);
            throw new RuntimeException("Không thể kết nối đến server", e);
        }
    }

    // Đóng kết nối
    public void disconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            logger.info("Đóng kết nối đến server thành công");
        } catch (Exception e) {
            logger.error("Lỗi khi đóng kết nối: ", e);
        }
    }
}
