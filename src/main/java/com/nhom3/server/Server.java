package com.nhom3.server;

import java.net.ServerSocket;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

import com.nhom3.server.network.ClientHandler;

public class Server {
    public static final int PORT = 8080; // Cổng do người dùng chọn
    public static final int MAX_CLIENTS = 100; // Giới hạn số lượng client kết nối đồng thời
    private ServerSocket serverSocket;
    private ExecutorService clientThreadPool;
    private List<ClientHandler> connectedClients;

    public Server() throws IOException {
        serverSocket = new ServerSocket(PORT);
        clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        connectedClients = new ArrayList<>();
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public void addClientHandler(ClientHandler clientHandler) {
        clientThreadPool.submit(clientHandler);
        connectedClients.add(clientHandler);
    }

    public void shutdown() {
        try {
            for (ClientHandler client : connectedClients) {
                client.interrupt(); // Dừng thread của client
            }
            clientThreadPool.shutdownNow(); // Dừng thread pool
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Đóng server socket
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
