package com.nhom3.server;

import java.net.ServerSocket;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

import com.nhom3.server.network.ClientHandler;

public class Server {
    public static final int DEFAULT_PORT = 8080;
    public static final int MAX_CLIENTS = 100; 
    private final ServerSocket serverSocket;
    private final int port;
    private final ExecutorService clientThreadPool;
    private final List<ClientHandler> connectedClients;
    private final Semaphore clientLimiter;

    public Server() throws IOException {
        port = readIntConfig("AUCTION_SERVER_PORT", "auction.server.port", DEFAULT_PORT);
        serverSocket = new ServerSocket(port);
        // Sử dụng Virtual Threads thay cho FixedThreadPool để tối ưu I/O
        clientThreadPool = Executors.newVirtualThreadPerTaskExecutor();
        // Cấu trúc dữ liệu Thread-safe cho danh sách clients
        connectedClients = new CopyOnWriteArrayList<>();
        // Semaphore quản lý giới hạn kết nối (thay thế cho size của FixedThreadPool cũ)
        clientLimiter = new Semaphore(MAX_CLIENTS);
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public int getPort() {
        return port;
    }

    public List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public void addClientHandler(ClientHandler clientHandler) {
        connectedClients.add(clientHandler);
        // Submit thông qua một Runnable để tự động xóa client khi hoàn thành/lỗi
        clientThreadPool.submit(() -> {
            try {
                clientHandler.run();
            } finally {
                removeClientHandler(clientHandler);
            }
        });
    }

    public void removeClientHandler(ClientHandler clientHandler) {
        connectedClients.remove(clientHandler);
        clientLimiter.release(); // Giải phóng 1 slot khi client ngắt kết nối
    }

    public void acquireClientSlot() throws InterruptedException {
        clientLimiter.acquire(); // Xin 1 slot trước khi accept
    }

    public void releaseClientSlot() {
        clientLimiter.release(); // Hoàn trả slot nếu có lỗi
    }

    public void shutdown() {
        try {
            for (ClientHandler client : connectedClients) {
                client.close();
            }
            clientThreadPool.shutdownNow(); // Dừng toàn bộ ThreadPool
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Đóng port
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi tắt server: " + e.getMessage());
        }
    }

    private static int readIntConfig(String envName, String propertyName, int defaultValue) {
        String propertyValue = System.getProperty(propertyName);
        String value = propertyValue != null && !propertyValue.isBlank()
                ? propertyValue
                : System.getenv(envName);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
