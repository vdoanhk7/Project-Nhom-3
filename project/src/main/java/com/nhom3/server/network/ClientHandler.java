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
import com.nhom3.server.network.liveUpdate.Announcer;


public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final AuthService authService;
    private final AuctionService auctionService;
    private final PacketDispatcher dispatcher;
    public BufferedReader in;
    public BufferedWriter out;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) throws IOException {
        this.clientSocket = socket;
        this.authService = new AuthService();
        this.auctionService = new AuctionService();
        this.clientSocket.setSoTimeout(600000); // Timeout 1 phút, sau đó tự ngắt kết nối 
        this.dispatcher = PacketDispatcher.getInstance();
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
        this.out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8));
    }

    public void close() throws IOException {
        clientSocket.close();
    }

    public synchronized void send(Packet packet) throws IOException {
        out.write(gson.toJson(packet));
        out.newLine();
        out.flush();
    }
    @Override
    public void run() {
        Logger logger = LoggerFactory.getLogger(ClientHandler.class);
        try {
            String line;
            while ((line = in.readLine()) != null) {
                Packet request = gson.fromJson(line, Packet.class);
                
                // FIX: Bảo vệ chống Null Pointer Exception khi chuỗi JSON rỗng hoặc không hợp lệ
                if (request == null || request.getType() == null) {
                    continue; 
                }
                
                System.out.println("[SERVER - RAW RECEIVE] Vừa nhận được gói tin loại: " + request.getType());

                // Sử dụng Dispatcher thay vì Switch Case khổng lồ
                Packet response = dispatcher.dispatch(request, gson, this);
                if (response != null) {
                    send(response);
                }
            }
        } catch (IOException e) {
            logger.error("Client ngắt kết nối");
        } finally {
            try {
                clientSocket.close();
                Announcer.getInstance().removeClientFromAll(this);
            } catch (IOException ex) {
            }
        }
    }
}
