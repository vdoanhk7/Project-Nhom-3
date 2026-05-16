package com.nhom3.server.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.nhom3.server.service.AuctionService;
import com.nhom3.server.service.AuthService;
import com.nhom3.shared.network.packet.Packet;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final PacketDispatcher dispatcher;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.authService = new AuthService();
        this.auctionService = new AuctionService();
        this.dispatcher = new PacketDispatcher(this.authService, this.auctionService, this);
    }

    public void close() throws IOException {
        clientSocket.close();
    }

    public synchronized void send(Packet packet) throws IOException {
        BufferedWriter out = new BufferedWriter(
                new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
        Gson gson = new Gson();
        out.write(gson.toJson(packet));
        out.newLine();
        out.flush();
    }
    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            Gson gson = new Gson();

            while ((line = in.readLine()) != null) {
                Packet request = gson.fromJson(line, Packet.class);
                
                // FIX: Bảo vệ chống Null Pointer Exception khi chuỗi JSON rỗng hoặc không hợp lệ
                if (request == null || request.getType() == null) {
                    continue; 
                }
                
                if (request.getType() != com.nhom3.shared.network.packet.PacketType.SUBSCRIBE_SYSTEM_LOGS) {
                    System.out.println("[SERVER - RAW RECEIVE] Vừa nhận được gói tin loại: " + request.getType());
                }

                // Sử dụng Dispatcher thay vì Switch Case khổng lồ
                Packet response = dispatcher.dispatch(request, gson);
                if (response != null) {
                    send(response);
                }
            }
        } catch (IOException e) {
            logger.error("Client ngắt kết nối");
        } finally {
            com.nhom3.server.network.liveUpdate.SystemLogAnnouncer.getInstance().removeObserver(this);
            try {
                clientSocket.close();
            } catch (IOException ex) {
            }
        }
    }
}
