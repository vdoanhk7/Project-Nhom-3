package com.nhom3.server.network;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nhom3.shared.network.Response;

public class AuctionServer {
    private static final Logger log = LoggerFactory.getLogger(AuctionServer.class);
    private static final int PORT = 8080;
    public static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log.info("Server đang chạy ở port {}", PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                log.info("Client connected: {}:{}", socket.getInetAddress(), socket.getPort());
                
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (Exception e) {
            log.error("Lỗi server: ", e);
        }
    }
    
    public static void broadcast(Response response) {
        for (ClientHandler client : clients) {
            client.sendResponse(response);
        }
    }
}