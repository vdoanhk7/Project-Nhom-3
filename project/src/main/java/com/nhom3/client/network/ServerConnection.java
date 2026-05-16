package com.nhom3.client.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
// Tự động văng lỗi IOException nếu cố nhận gửi mà ngắt kết nối 
import java.io.OutputStreamWriter;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;

import com.nhom3.shared.network.packet.Packet;

public class ServerConnection {
    private static ServerConnection instance;
    private static final String SERVER_HOST = readConfig("AUCTION_SERVER_HOST", "localhost");
    private static final int SERVER_PORT = readIntConfig("AUCTION_SERVER_PORT", 8080);

    private static final Logger logger = LoggerFactory.getLogger(ServerConnection.class);

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;
    private final Gson gson = new Gson();

    // Singleton constructor
    private ServerConnection() {
    }

    public static ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    // Kết nối đến server
    public void connect() throws IOException {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            logger.info("Kết nối đến server thành công tại {}:{}", SERVER_HOST, SERVER_PORT);
        } catch (IOException e) {
            logger.error("Lỗi kết nối đến server: ", e);
            throw new IOException("Lỗi kết nối đến server", e);
        }
    }

    // Đóng kết nối
    public void disconnect() {
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (socket != null)
                socket.close();
            logger.info("Đóng kết nối đến server thành công");
        } catch (Exception e) {
            logger.error("Lỗi khi đóng kết nối: ", e);
        }
    }

    // Gửi yêu cầu đến server
    public synchronized void sendMessage(Packet request) throws IOException {
        try {
            String jsonRequest = gson.toJson(request);
            out.write(jsonRequest);
            out.newLine(); // Đảm bảo readLine bên server có thể đọc được
            out.flush();
            logger.debug("Gửi đến server: {}", jsonRequest);
        } catch (IOException e) {
            logger.error("Lỗi khi gửi yêu cầu đến server: ", e);
            throw new IOException("Lỗi khi gửi yêu cầu đến server", e);
        }
    }

    // Nhận phản hồi từ server
    public String receiveResponse() throws IOException {
        try {
            String jsonResponse = in.readLine();
            if (jsonResponse == null) {
                throw new IOException("Kết nối đã bị đóng bởi server");
            }
            logger.debug("Nhận từ server: {}", jsonResponse);
            return jsonResponse;
        } catch (Exception e) { // Multiple exceptions
            logger.error("Lỗi khi nhận phản hồi từ server", e);
            throw new IOException("Lỗi khi nhận phản hồi từ server", e);
        }
    }

    private static String readConfig(String envName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = System.getProperty(envName.toLowerCase().replace('_', '.'));
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        return defaultValue;
    }

    private static int readIntConfig(String envName, int defaultValue) {
        String value = readConfig(envName, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
